/*
 * This file is part of j60870.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package com.openmuc.j60870.app;

import com.openmuc.j60870.*;
import com.openmuc.j60870.ie.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleServer {


    private int connectionIdCounter = 1;
    private DataProcessImage dataProcessImage;
    //private List<ServerListener.ConnectionListener> connectionListenerList = new ArrayList<>();
    private Map<Integer, Connection> connectionMap = new HashMap<>();
    private int port = 2404;           //默认端口
    private int slaveId = 1;
    private String ip = "127.0.0.1";   //The bind address
    private int iaoLength = 3;     //Information Object Address (IOA) field length  信息结构地址长度
    private int caLength = 2;      //Common Address (CA) field length.              公共地址长度
    private int cotLength = 2;     //Cause Of Transmission (CoT) field length.      传送原因长度

    private int spontaneousTime = 1; //突发上送周期
    private Thread spontaneousThread;     //突发上送线程
    Server server;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws UnknownHostException {
        //new SimpleServer().start();
    }

    public SimpleServer(DataProcessImage dataProcessImage,int port,int slaveId,Map<String,String> extraMap){
        this.dataProcessImage = dataProcessImage;
        this.port = port;
        this.slaveId = slaveId;
        if(extraMap.containsKey("ioaLength")) iaoLength=Integer.valueOf(extraMap.get("ioaLength"));
        if(extraMap.containsKey("caLength")) caLength=Integer.valueOf(extraMap.get("caLength"));
        if(extraMap.containsKey("coTLength")) cotLength=Integer.valueOf(extraMap.get("coTLength"));
        if(extraMap.containsKey("spontaneousTime")) spontaneousTime=Integer.valueOf(extraMap.get("spontaneousTime"));
    }


    public void start() throws UnknownHostException {
        log("### Starting Server ###\n", "\nBind Address: ", ip, "\nPort:         ", String.valueOf(this.port), "\nIAO length:   ", String.valueOf(iaoLength),
                "\nCA length:    ", String.valueOf(caLength), "\nCOT length:   ", String.valueOf(cotLength), "\n");

        Server.Builder builder = Server.builder();
        //InetAddress bindAddress = InetAddress.getByName(ip);
        InetAddress bindAddress =InetAddress.getByName("0.0.0.0");
        builder.setBindAddr(bindAddress)
                .setPort(this.port)
                .setIoaFieldLength(iaoLength)
                .setCommonAddressFieldLength(caLength)
                .setCotFieldLength(cotLength);// .setMaxNumOfOutstandingIPdus(10);
        server = builder.build();
        try {
            server.start(new ServerListener());
            spontaneousByStation();
        } catch (IOException e) {
            log("Unable to start listening: \"", e.getMessage(), "\". Will quit.");
        }
    }

    public void stop(){
        try{
            server.stop();
            for(Connection connection : connectionMap.values()){
                connection.close();
            }
        }catch (Exception ex){

        }

    }

    private void log(String... strings) {
        String time = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ").format(new Date());
        println(time, strings);
    }

    private void println(String string, String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        for (String s : strings) {
            sb.append(s);
        }
        System.out.println(sb.toString());
    }



    private void addConnection(int myConnectionId, Connection connection){
        connectionMap.put(myConnectionId,connection);
        log(myConnectionId + "-add**************************************************************************"+connectionMap.size());
    }


    private void removeConnection(int myConnectionId, Connection connection){
        connectionMap.remove(myConnectionId);
        log(myConnectionId + "-remove**************************************************************************"+connectionMap.size());
    }

    //突发上送，服务端统一处理所有连接
    private void spontaneousByStation(){
        spontaneousThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int count=0;
                while(true){
                    try{
                        Thread.sleep(spontaneousTime*1000);
                        sendPendingSpontaneous();
                    }catch (Exception ex){
                        log(ex.toString());
                    }
                }
            }
        });
        spontaneousThread.start();
    }

    private void sendPendingSpontaneous() {
        Map<ASduType, List<InformationObject>> sendBatchMap = dataProcessImage.getSendBatchMap();
        for (ASduType aSduType : sendBatchMap.keySet()) {
            List<InformationObject> sendBatch = sendBatchMap.get(aSduType);
            for (InformationObject informationObject : sendBatch) {
                if (informationObject.getChangeFlag() > 0) {
                    informationObject.setChangeFlag(0);
                    broadcastSpontaneous(aSduType, informationObject);
                }
            }
        }
    }

    /**
     * SOE 事件触发后立即上送，不等待 spontaneous 轮询周期。
     */
    public void sendSpontaneousImmediately(ASduType aSduType, InformationObject informationObject) {
        if (informationObject == null || aSduType == null) {
            return;
        }
        informationObject.setChangeFlag(0);
        broadcastSpontaneous(aSduType, informationObject);
    }

    private void broadcastSpontaneous(ASduType aSduType, InformationObject informationObject) {
        for (Connection connection : connectionMap.values()) {
            if (connection.isClosed() || connection.isStopped()) {
                continue;
            }
            try {
                ASdu sendASdu = new ASdu(aSduType, true, CauseOfTransmission.SPONTANEOUS, false, false, 0, slaveId, informationObject);
                connection.send(sendASdu);
            } catch (IOException e) {
                log("Spontaneous send failed: ", e.getMessage());
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getSlaveId() {
        return slaveId;
    }

    public class ServerListener implements ServerEventListener {

        @Override
        public void connectionIndication(Connection connection) {

            int myConnectionId = connectionIdCounter++;
            log("A client (Originator Address " + connection.getOriginatorAddress() + ") has connected using TCP/IP. Will listen for a StartDT request. Connection ID: " + myConnectionId);
            log("Started data transfer on connection (" + myConnectionId, ") Will listen for incoming commands.");
            ConnectionListener connectionListener = new ConnectionListener(connection, myConnectionId);
            connection.setConnectionListener(connectionListener);
        }

        @Override
        public void serverStoppedListeningIndication(IOException e) {
            log("Server has stopped listening for new connections : \"", e.getMessage(), "\". Will quit.");
        }

        @Override
        public void connectionAttemptFailed(IOException e) {
            log("Connection attempt failed: ", e.getMessage());
        }


        //广播变化的数据
        //public void broadcastChangeData(){}

        public class ConnectionListener implements ConnectionEventListener {

            private final Connection connection;
            private final int connectionId;
            private boolean selected = false;

            public ConnectionListener(Connection connection, int connectionId) {
                this.connection = connection;
                this.connectionId = connectionId;
                addConnection(connectionId,connection);
                //backgroundScan();
            }

            @Override
            public void newASdu(ASdu aSdu) {
                log("Got new ASdu:");
                println(aSdu.toString(), "\n");
                //scanStart = true;
                InformationObject informationObject = null;
                try {
                    switch (aSdu.getTypeIdentification()) {
                        // interrogation command  总召唤
                        case C_IC_NA_1:
                            interrogatedByStation(aSdu);
                            break;
                        case C_SC_NA_1: //单点遥控
                            singleCommand(aSdu);
                            break;
                        case C_SE_NC_1: //设定值命令 , 短浮点数
                            setShortFloatingCommand(aSdu);
                        case C_CS_NA_1:
                            IeTime56 ieTime56 = new IeTime56(System.currentTimeMillis());
                            log("Got Clock synchronization command (103). Send current time: \n", ieTime56.toString());
                            connection.synchronizeClocks(aSdu.getCommonAddress(), ieTime56);
                            break;
                        case C_SE_NB_1:
                            log("Got Set point command, scaled value (49)");
                            break;
                        default:
                            log("Got unknown request: ", aSdu.toString(), ". Send negative confirm with CoT UNKNOWN_TYPE_ID(44)\n");
                            connection.sendConfirmation(aSdu, aSdu.getCommonAddress(), true, CauseOfTransmission.UNKNOWN_TYPE_ID);
                    }
                } catch (EOFException e) {
                    log("Will quit listening for commands on connection (" + connectionId, ") because socket was closed.");
                } catch (IOException e) {
                    log("Will quit listening for commands on connection (" + connectionId, ") because of error: \"", e.getMessage(), "\".");
                }
            }

            //原因20-响应总召唤
            private void interrogatedByStation(ASdu aSdu) throws IOException {
                log("Got interrogation command (100). Will send scaled measured values.");
                connection.sendConfirmation(aSdu);
                Map<ASduType,List<InformationObject>> sendBatchMap = dataProcessImage.getSendBatchMap();
                for(ASduType aSduType :  sendBatchMap.keySet()){
                    List<InformationObject> sendBatch = sendBatchMap.get(aSduType);
                    for(InformationObject informationObject:sendBatch){
                        //ASdu sendASdu = new ASdu(aSduType, true, CauseOfTransmission.INTERROGATED_BY_STATION, false, false, 0, aSdu.getCommonAddress(), informationObject);
                        ASdu sendASdu = new ASdu(aSduType, true, CauseOfTransmission.INTERROGATED_BY_STATION, false, false, 0, slaveId, informationObject);
                        connection.send(sendASdu);
                    }
                }
                connection.sendActivationTermination(aSdu);
            }

            //原因45 -单点遥控
            private void singleCommand(ASdu aSdu) throws IOException {
                InformationObject informationObject = aSdu.getInformationObjects()[0];
                IeSingleCommand singleCommand = (IeSingleCommand) informationObject.getInformationElements()[0][0];
                int address = informationObject.getInformationObjectAddress();
                if (singleCommand.isSelect()) {
                    log("Got single command (45) with select true. Select command.");
                    selected = true;
                    connection.sendConfirmation(aSdu);
                    //} else if (!singleCommand.isSelect() && selected) { //选控，这里的选择是否遥放到此点里面
                } else if (!singleCommand.isSelect()) {  //风机遥控暂时都用直控
                    log("Got single command (45) with select false. Execute selected command.");
                    selected = false;
                    dataProcessImage.write(address,singleCommand.isCommandStateOn()?1:0);
                    connection.sendConfirmation(aSdu);
                } else {
                    log("Got single command (45) with select false. But no command is selected, no execution.");
                }
            }

            //原因50 - 设定值命令 , 短浮点数
            private void setShortFloatingCommand(ASdu aSdu) throws IOException {
                InformationObject informationObject = aSdu.getInformationObjects()[0];
                IeShortFloat ieShortFloat = (IeShortFloat) informationObject.getInformationElements()[0][0];
                IeQualifierOfSetPointCommand ieQualifierOfSetPointCommand = (IeQualifierOfSetPointCommand) informationObject.getInformationElements()[0][1];
                int address = informationObject.getInformationObjectAddress();
                log("Got single command (45) with select false. Execute selected command.");
                selected = false;
                dataProcessImage.write(address,ieShortFloat.getValue());
                connection.sendConfirmation(aSdu);
                log("Got single command (45) with select false. But no command is selected, no execution.");
            }


            @Override
            public void connectionClosed(IOException e) {
                log("Connection (" + connectionId, ") was closed. ", e.getMessage());
                //scanThread.interrupt();
                removeConnection(connectionId,connection);
            }

            @Override
            public void dataTransferStateChanged(boolean stopped) {
                String dtState = "started";
                if (stopped) {
                    dtState = "stopped";
                }
                log("Data transfer of connection (" + connectionId + ") was ", dtState, ".");
            }


        }//ConnectionListener结束


    }//ServerListener结束



}
