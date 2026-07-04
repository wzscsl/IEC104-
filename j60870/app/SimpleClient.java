package com.openmuc.j60870.app;

import com.openmuc.j60870.*;
import com.openmuc.j60870.ie.IeQualifierOfInterrogation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;


/**
 * @ Author     ：dengzhihai
 * @ Date       ：Created in 20:47 2023/8/20
 * @ Description：104规约简单客户端
 * @ Modified By：
 * @Version: 1.0
 */
public class SimpleClient {

    public static final String INTERROGATION_ACTION_KEY = "i";
    public static final String COUNTER_INTERROGATION_ACTION_KEY = "ci";
    public static final String CLOCK_SYNC_ACTION_KEY = "c";
    public static final String SINGLE_COMMAND_SELECT = "s";
    public static final String SINGLE_COMMAND_EXECUTE = "e";
    public static final String SEND_STOPDT = "p";
    public static final String SEND_STARTDT = "t";

    private Connection connection;
    private String host = "192.168.1.210";
    private int port = 2404;
    private int commonAddress = 1;
    private int startDtRetries = 2;
    private int connectionTimeout = 5000;  //ms单位
    private int messageFragmentTimeout = 5000;  //ms单位

    ConnectionEventListener listener;


    //Logger logger = LoggerFactory.getLogger(this.getClass());

    static Logger logger = LoggerFactory.getLogger("test");


    public static void main(String[] args) {
        ConnectionEventListener listener = new ConnectionEventListener() {
            @Override
            public void newASdu(ASdu aSdu) {
                logger.info("\nReceived ASDU:\n", aSdu.getTypeIdentification() + " length:"+aSdu.getInformationObjects().length);
                logger.info(aSdu.toString());
            }

            @Override
            public void connectionClosed(IOException e) {
                logger.info("Received connection closed signal. Reason: ");
                if (!e.getMessage().isEmpty()) {
                    logger.info(e.getMessage());
                } else {
                    logger.info("unknown");
                }
            }

            @Override
            public void dataTransferStateChanged(boolean stopped) {
                String dtState = "started";
                if (stopped) {
                    dtState = "stopped";
                }
                logger.info("Data transfer was ", dtState);
            }
        };
        SimpleClient simpleClient = new SimpleClient();
        simpleClient.init(new HashMap<>(),listener);
        simpleClient.connect();
        while(true){
            try{
                Thread.sleep(2000);
                if(simpleClient.connection.isClosed()==false)
                    simpleClient.actionCalled(SimpleClient.INTERROGATION_ACTION_KEY);
                if(simpleClient.connection.isClosed()==true)
                    simpleClient.connect();
            }catch (Exception ex){

            }
        }
    }

    public void init(Map<String,String> paramMap,ConnectionEventListener listener){
        if(paramMap.containsKey("host")) host =paramMap.get("host");
        if(paramMap.containsKey("port")) port =Integer.parseInt(paramMap.get("port"));
        if(paramMap.containsKey("commonAddress")) commonAddress =Integer.parseInt(paramMap.get("commonAddress"));
        if(paramMap.containsKey("startDtRetries")) startDtRetries=Integer.parseInt(paramMap.get("startDtRetries"));
        if(paramMap.containsKey("connectionTimeout")) connectionTimeout=Integer.parseInt(paramMap.get("connectionTimeout"));
        if(paramMap.containsKey("messageFragmentTimeout")) messageFragmentTimeout=Integer.parseInt(paramMap.get("messageFragmentTimeout"));
        this.listener = listener;
    }

    public boolean isConnected(){
        if(connection == null)
            return false;
        else if(connection.isClosed())
            return false;
        else
            return true;
    }

    public void connect() {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            logger.error("Unknown host: ", host);
            return;
        }
        ClientConnectionBuilder clientConnectionBuilder = new ClientConnectionBuilder(address)
                .setMessageFragmentTimeout(messageFragmentTimeout)
                .setConnectionTimeout(connectionTimeout)
                .setPort(port);
        try {
            connection = clientConnectionBuilder.build();
        } catch (IOException e) {
            logger.info("Unable to connect to remote host: ", host, ".");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                connection.close();
            }
        });
        boolean connected = false;
        int retries = startDtRetries;
        int i = 1;
        while (!connected && i <= retries) {
            try {
                logger.info("Send start DT. Try no. " + i);
                connection.startDataTransfer(this.listener);
            } catch (InterruptedIOException e2) {
                if (i == retries) {
                    logger.info("Starting data transfer timed out. Closing connection. Because of no more retries.");
                    connection.close();
                    return;
                } else {
                    logger.info("Got Timeout.class Next try.");
                    ++i;
                    continue;
                }
            } catch (IOException e) {
                logger.info("Connection closed for the following reason: ", e.getMessage());
                return;
            }
            connected = true;
        }
        logger.info("successfully connected");
    }

    public boolean actionCalled(String actionKey) {
        try {
            switch (actionKey) {
                case INTERROGATION_ACTION_KEY:
                    logger.info("** Sending general interrogation command.");
                    connection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION,
                            new IeQualifierOfInterrogation(20));
                    break;
                case COUNTER_INTERROGATION_ACTION_KEY:
                    //logger.info("Enter the freeze action: 0=read, 1=counter freeze wo reset, 2=counter freeze with reset, 3=counter reset");
                    //String reference = actionProcessor.getReader().readLine();
                    //logger.info("** Sending counter interrogation command.");
                    //connection.counterInterrogation(commonAddrParam, CauseOfTransmission.ACTIVATION,
                    //        new IeQualifierOfCounterInterrogation(5, Integer.parseInt(reference)));
                    //break;
                case CLOCK_SYNC_ACTION_KEY:
                    //logger.info("** Sending synchronize clocks command.");
                    //connection.synchronizeClocks(commonAddrParam.getValue(), new IeTime56(System.currentTimeMillis()));
                    //break;
                case SINGLE_COMMAND_SELECT:
                    logger.info("** Sending single command select.");
                    //connection.singleCommand(commonAddrParam, CauseOfTransmission.ACTIVATION, 5000,new IeSingleCommand(true, 0, true));
                    break;
                case SINGLE_COMMAND_EXECUTE:
                    logger.info("** Sending single command execute.");
                    //connection.singleCommand(commonAddrParam, CauseOfTransmission.ACTIVATION, 5000, new IeSingleCommand(false, 0, false));
                    break;
                case SEND_STOPDT:
                    logger.info("** Sending STOPDT act.");
                    connection.stopDataTransfer();
                    break;
                case SEND_STARTDT:
                    logger.info("** Sending STARTDT act.");
                    connection.startDataTransfer(this.listener);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("** Sending STOPDT act.",e);
            return false;
        }
        return true;
    }


}
