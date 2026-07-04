package com.iec104tester.core;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.model.DataPointInfo;
import com.iec104tester.model.ServerConfig;
import com.openmuc.j60870.*;
import com.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages the IEC 104 server lifecycle, connection handling, and data transmission.
 */
public class ServerManager {

    public enum ServerState { STOPPED, STARTING, RUNNING, ERROR }

    private Server server;
    private ServerState state = ServerState.STOPPED;
    private ServerConfig config;
    private ServerDataModel dataModel;
    private CaptureManager captureManager;

    private final Map<Integer, Connection> connectionMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> connectionInfoMap = new ConcurrentHashMap<>();
    private int connectionIdCounter = 1;

    private Consumer<ServerState> stateCallback;
    private Consumer<String> errorCallback;
    private Consumer<ASdu> receivedAsduCallback;
    private Consumer<int[]> connectionListCallback;

    private Thread spontaneousThread;
    private volatile boolean spontaneousRunning = false;

    public void setDataModel(ServerDataModel dataModel) {
        this.dataModel = dataModel;
    }

    public void setCaptureManager(CaptureManager captureManager) {
        this.captureManager = captureManager;
    }

    public void setStateCallback(Consumer<ServerState> callback) {
        this.stateCallback = callback;
    }

    public void setErrorCallback(Consumer<String> callback) {
        this.errorCallback = callback;
    }

    public void setReceivedAsduCallback(Consumer<ASdu> callback) {
        this.receivedAsduCallback = callback;
    }

    public void setConnectionListCallback(Consumer<int[]> callback) {
        this.connectionListCallback = callback;
    }

    public ServerState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == ServerState.RUNNING;
    }

    public Map<Integer, String> getConnectionInfoMap() {
        return new HashMap<>(connectionInfoMap);
    }

    public void start(ServerConfig config) {
        // 先停止旧服务，确保干净的重启
        if (server != null) {
            stopSpontaneousThread();
            for (Connection conn : connectionMap.values()) {
                try { conn.close(); } catch (Exception e) { /* Ignore */ }
            }
            connectionMap.clear();
            connectionInfoMap.clear();
            try { server.stop(); } catch (Exception e) { /* Ignore */ }
            server = null;
        }

        this.config = config;
        setState(ServerState.STARTING);

        try {
            Server.Builder builder = Server.builder();
            builder.setPort(config.getPort());
            builder.setBindAddr(InetAddress.getByName(config.getBindAddress()));
            builder.setIoaFieldLength(config.getIoaFieldLength());
            builder.setCommonAddressFieldLength(config.getCommonAddressFieldLength());
            builder.setCotFieldLength(config.getCotFieldLength());
            builder.setMaxConnections(config.getMaxConnections());
            builder.setMaxTimeNoAckReceived(config.getT1());
            builder.setMaxTimeNoAckSent(config.getT2());
            builder.setMaxIdleTime(config.getT3());
            builder.setMaxNumOfOutstandingIPdus(config.getK());
            builder.setMaxUnconfirmedIPdusReceived(config.getW());

            server = builder.build();
            server.start(new ServerEventListener() {
                @Override
                public void connectionIndication(Connection connection) {
                    int connId = connectionIdCounter++;
                    String clientInfo = connection.getRemoteInetAddress() != null
                            ? connection.getRemoteInetAddress().getHostAddress() + ":" + connection.getPort()
                            : "unknown";
                    connectionInfoMap.put(connId, clientInfo);
                    connectionMap.put(connId, connection);

                    if (captureManager != null) {
                        connection.setFrameListener(captureManager);
                    }

                    connection.setConnectionListener(new ConnectionEventListener() {
                        @Override
                        public void newASdu(ASdu aSdu) {
                            handleAsdu(connection, aSdu, config.getCommonAddress());
                            if (receivedAsduCallback != null) {
                                receivedAsduCallback.accept(aSdu);
                            }
                        }

                        @Override
                        public void connectionClosed(IOException cause) {
                            connectionMap.remove(connId);
                            connectionInfoMap.remove(connId);
                            notifyConnectionList();
                        }

                        @Override
                        public void dataTransferStateChanged(boolean stopped) {
                            // State change handled internally
                        }
                    });

                    notifyConnectionList();
                }

                @Override
                public void serverStoppedListeningIndication(IOException e) {
                    setState(ServerState.STOPPED);
                }

                @Override
                public void connectionAttemptFailed(IOException e) {
                    if (errorCallback != null) {
                        errorCallback.accept("连接尝试失败: " + e.getMessage());
                    }
                }
            });

            if (config.isSpontaneousEnabled()) {
                startSpontaneousThread();
            }

            setState(ServerState.RUNNING);

        } catch (UnknownHostException e) {
            setState(ServerState.ERROR);
            if (errorCallback != null) errorCallback.accept("未知主机: " + e.getMessage());
        } catch (IOException e) {
            setState(ServerState.ERROR);
            if (errorCallback != null) errorCallback.accept("启动失败: " + e.getMessage());
        }
    }

    public void stop() {
        stopSpontaneousThread();

        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Ignore
            }
            server = null;
        }

        for (Connection conn : connectionMap.values()) {
            try {
                conn.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        connectionMap.clear();
        connectionInfoMap.clear();
        notifyConnectionList();

        setState(ServerState.STOPPED);
    }

    public void disconnectClient(int connId) {
        Connection conn = connectionMap.get(connId);
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                // Ignore
            }
            connectionMap.remove(connId);
            connectionInfoMap.remove(connId);
            notifyConnectionList();
        }
    }

    private void handleAsdu(Connection connection, ASdu aSdu, int stationAddress) {
        try {
            switch (aSdu.getTypeIdentification()) {
                case C_IC_NA_1:
                    handleInterrogation(connection, aSdu, stationAddress);
                    break;
                case C_CI_NA_1:
                    connection.sendConfirmation(aSdu, stationAddress);
                    connection.sendActivationTermination(aSdu, stationAddress);
                    break;
                case C_CS_NA_1:
                    connection.sendConfirmation(aSdu, stationAddress);
                    break;
                case C_TS_NA_1:
                    connection.sendConfirmation(aSdu, stationAddress);
                    break;
                case C_SC_NA_1:
                    handleSingleCommand(connection, aSdu, stationAddress);
                    break;
                case C_SE_NC_1:
                    handleSetShortFloatCommand(connection, aSdu, stationAddress);
                    break;
                case C_SE_NB_1:
                    handleSetScaledValueCommand(connection, aSdu, stationAddress);
                    break;
                case C_RD_NA_1:
                    handleReadCommand(connection, aSdu, stationAddress);
                    break;
                case C_RP_NA_1:
                    connection.sendConfirmation(aSdu, stationAddress);
                    break;
                default:
                    // Send unknown type response
                    connection.sendConfirmation(aSdu, stationAddress, true,
                            CauseOfTransmission.UNKNOWN_TYPE_ID);
                    break;
            }
        } catch (IOException e) {
            if (errorCallback != null) {
                errorCallback.accept("处理ASDU失败: " + e.getMessage());
            }
        }
    }

    private void handleInterrogation(Connection connection, ASdu request, int stationAddress) throws IOException {
        // Send activation confirmation
        connection.sendConfirmation(request, stationAddress);

        // Send data for each type
        if (dataModel != null) {
            for (ASduType type : dataModel.getUsedTypes()) {
                List<InformationObject> objects = dataModel.buildInterrogationObjects(type);
                for (InformationObject obj : objects) {
                    ASdu dataAsdu = new ASdu(type, false, CauseOfTransmission.INTERROGATED_BY_STATION,
                            false, false, request.getOriginatorAddress(), stationAddress, obj);
                    connection.send(dataAsdu);
                }
            }
        }

        // Send activation termination
        connection.sendActivationTermination(request, stationAddress);
    }

    private void handleSingleCommand(Connection connection, ASdu request, int stationAddress) throws IOException {
        InformationObject[] objects = request.getInformationObjects();
        if (objects != null && objects.length > 0) {
            int ioa = objects[0].getInformationObjectAddress();
            InformationElement[][] elements = objects[0].getInformationElements();
            if (elements != null && elements.length > 0 && elements[0].length > 0) {
                try {
                    IeSingleCommand cmd = (IeSingleCommand) elements[0][0];
                    boolean on = cmd.isCommandStateOn();
                    if (dataModel != null) {
                        dataModel.updateValue(ioa, on ? 1.0 : 0.0);
                    }
                } catch (ClassCastException e) {
                    // Ignore
                }
            }
        }
        connection.sendConfirmation(request, stationAddress);
    }

    private void handleSetShortFloatCommand(Connection connection, ASdu request, int stationAddress) throws IOException {
        InformationObject[] objects = request.getInformationObjects();
        if (objects != null && objects.length > 0) {
            int ioa = objects[0].getInformationObjectAddress();
            InformationElement[][] elements = objects[0].getInformationElements();
            if (elements != null && elements.length > 0 && elements[0].length > 0) {
                try {
                    IeShortFloat value = (IeShortFloat) elements[0][0];
                    if (dataModel != null) {
                        dataModel.updateValue(ioa, value.getValue());
                    }
                } catch (ClassCastException e) {
                    // Ignore
                }
            }
        }
        connection.sendConfirmation(request, stationAddress);
    }

    private void handleSetScaledValueCommand(Connection connection, ASdu request, int stationAddress) throws IOException {
        InformationObject[] objects = request.getInformationObjects();
        if (objects != null && objects.length > 0) {
            int ioa = objects[0].getInformationObjectAddress();
            InformationElement[][] elements = objects[0].getInformationElements();
            if (elements != null && elements.length > 0 && elements[0].length > 0) {
                try {
                    IeScaledValue value = (IeScaledValue) elements[0][0];
                    if (dataModel != null) {
                        dataModel.updateValue(ioa, value.getUnnormalizedValue());
                    }
                } catch (ClassCastException e) {
                    // Ignore
                }
            }
        }
        connection.sendConfirmation(request, stationAddress);
    }

    private void handleReadCommand(Connection connection, ASdu request, int stationAddress) throws IOException {
        InformationObject[] objects = request.getInformationObjects();
        if (objects != null && objects.length > 0 && dataModel != null) {
            int ioa = objects[0].getInformationObjectAddress();
            DataPointInfo point = dataModel.getDataPoint(ioa);
            if (point != null) {
                InformationObject obj = dataModel.buildInformationObject(point);
                if (obj != null) {
                    ASdu response = new ASdu(point.getAsduType(), false, CauseOfTransmission.REQUEST,
                            false, false, request.getOriginatorAddress(), stationAddress, obj);
                    connection.send(response);
                    return;
                }
            }
        }
        // If point not found, send unknown IOA response
        connection.sendConfirmation(request, stationAddress, true,
                CauseOfTransmission.UNKNOWN_INFORMATION_OBJECT_ADDRESS);
    }

    private void startSpontaneousThread() {
        spontaneousRunning = true;
        spontaneousThread = new Thread(() -> {
            while (spontaneousRunning) {
                try {
                    Thread.sleep(config.getSpontaneousInterval() * 1000);
                } catch (InterruptedException e) {
                    break;
                }

                if (!spontaneousRunning || connectionMap.isEmpty() || dataModel == null) {
                    continue;
                }

                List<DataPointInfo> changed = dataModel.getAndClearChangedPoints();
                if (changed.isEmpty()) continue;

                for (DataPointInfo point : changed) {
                    InformationObject obj = dataModel.buildInformationObject(point);
                    if (obj == null) continue;

                    ASdu spontaneousAsdu = new ASdu(point.getAsduType(), false,
                            CauseOfTransmission.SPONTANEOUS, false, false, 0,
                            config.getCommonAddress(), obj);

                    for (Connection conn : connectionMap.values()) {
                        try {
                            conn.send(spontaneousAsdu);
                        } catch (IOException e) {
                            // Connection may be closed
                        }
                    }
                }
            }
        }, "SpontaneousTransmitter");
        spontaneousThread.setDaemon(true);
        spontaneousThread.start();
    }

    private void stopSpontaneousThread() {
        spontaneousRunning = false;
        if (spontaneousThread != null) {
            spontaneousThread.interrupt();
            spontaneousThread = null;
        }
    }

    private void notifyConnectionList() {
        if (connectionListCallback != null) {
            connectionListCallback.accept(connectionMap.keySet().stream().mapToInt(i -> i).toArray());
        }
    }

    private void setState(ServerState newState) {
        this.state = newState;
        if (stateCallback != null) {
            stateCallback.accept(newState);
        }
    }

    public void broadcastDataPoint(DataPointInfo point) {
        if (connectionMap.isEmpty() || dataModel == null) return;

        InformationObject obj = dataModel.buildInformationObject(point);
        if (obj == null) return;

        ASdu asdu = new ASdu(point.getAsduType(), false, CauseOfTransmission.SPONTANEOUS,
                false, false, 0, config.getCommonAddress(), obj);

        for (Connection conn : connectionMap.values()) {
            try {
                conn.send(asdu);
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
