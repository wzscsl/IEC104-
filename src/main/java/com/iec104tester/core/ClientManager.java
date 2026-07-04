package com.iec104tester.core;

import com.iec104tester.capture.CaptureManager;
import com.iec104tester.model.ConnectionConfig;
import com.openmuc.j60870.*;
import com.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

/**
 * Manages the IEC 104 client connection lifecycle.
 */
public class ClientManager {

    public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private Connection connection;
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private ConnectionConfig config;
    private CaptureManager captureManager;
    private Consumer<ConnectionState> stateCallback;
    private Consumer<ASdu> asduCallback;
    private Consumer<String> errorCallback;
    private ConnectionEventListener eventListener;

    public void setCaptureManager(CaptureManager captureManager) {
        this.captureManager = captureManager;
    }

    public void setStateCallback(Consumer<ConnectionState> callback) {
        this.stateCallback = callback;
    }

    public void setAsduCallback(Consumer<ASdu> callback) {
        this.asduCallback = callback;
    }

    public void setErrorCallback(Consumer<String> callback) {
        this.errorCallback = callback;
    }

    public ConnectionState getState() {
        return state;
    }

    public boolean isConnected() {
        return state == ConnectionState.CONNECTED && connection != null && !connection.isClosed();
    }

    public Connection getConnection() {
        return connection;
    }

    public void connect(ConnectionConfig config) {
        // 先关闭旧连接，确保干净的重连
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (Exception e) {
                // Ignore
            }
            connection = null;
        }

        this.config = config;
        setState(ConnectionState.CONNECTING);

        try {
            ClientConnectionBuilder builder = new ClientConnectionBuilder(config.getHost());
            builder.setPort(config.getPort());
            builder.setConnectionTimeout(config.getConnectionTimeout());
            builder.setMessageFragmentTimeout(config.getMessageFragmentTimeout());
            builder.setMaxTimeNoAckReceived(config.getT1());
            builder.setMaxTimeNoAckSent(config.getT2());
            builder.setMaxIdleTime(config.getT3());
            builder.setMaxNumOfOutstandingIPdus(config.getK());
            builder.setMaxUnconfirmedIPdusReceived(config.getW());
            builder.setCotFieldLength(config.getCotFieldLength());
            builder.setCommonAddressFieldLength(config.getCommonAddressFieldLength());
            builder.setIoaFieldLength(config.getIoaFieldLength());

            connection = builder.build();
            connection.setOriginatorAddress(config.getOriginatorAddress());

            if (captureManager != null) {
                connection.setFrameListener(captureManager);
            }

            eventListener = new ConnectionEventListener() {
                @Override
                public void newASdu(ASdu aSdu) {
                    if (asduCallback != null) {
                        asduCallback.accept(aSdu);
                    }
                }

                @Override
                public void connectionClosed(IOException cause) {
                    setState(ConnectionState.DISCONNECTED);
                    if (errorCallback != null && cause != null) {
                        errorCallback.accept("连接关闭: " + cause.getMessage());
                    }
                }

                @Override
                public void dataTransferStateChanged(boolean stopped) {
                    // Not used for client
                }
            };

            connection.startDataTransfer(eventListener);
            setState(ConnectionState.CONNECTED);

        } catch (UnknownHostException e) {
            setState(ConnectionState.ERROR);
            if (errorCallback != null) errorCallback.accept("未知主机: " + e.getMessage());
        } catch (IOException e) {
            setState(ConnectionState.ERROR);
            if (errorCallback != null) errorCallback.accept("连接失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        connection = null;
        setState(ConnectionState.DISCONNECTED);
    }

    private void setState(ConnectionState newState) {
        this.state = newState;
        if (stateCallback != null) {
            stateCallback.accept(newState);
        }
    }

    // Convenience methods for sending commands

    public void sendInterrogation(int commonAddress, int qualifier) throws IOException {
        if (isConnected()) {
            connection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION,
                    new IeQualifierOfInterrogation(qualifier));
        }
    }

    public void sendClockSync(int commonAddress) throws IOException {
        if (isConnected()) {
            connection.synchronizeClocks(commonAddress, new IeTime56(System.currentTimeMillis()));
        }
    }

    public void sendSingleCommand(int commonAddress, int ioa, boolean on, int qualifier, boolean select) throws IOException {
        if (isConnected()) {
            CauseOfTransmission cot = select ? CauseOfTransmission.ACTIVATION : CauseOfTransmission.ACTIVATION;
            connection.singleCommand(commonAddress, cot, ioa, new IeSingleCommand(on, qualifier, select));
        }
    }

    public void sendDoubleCommand(int commonAddress, int ioa, com.openmuc.j60870.ie.IeDoubleCommand.DoubleCommandState state, int qualifier, boolean select) throws IOException {
        if (isConnected()) {
            connection.doubleCommand(commonAddress, CauseOfTransmission.ACTIVATION, ioa,
                    new com.openmuc.j60870.ie.IeDoubleCommand(state, qualifier, select));
        }
    }

    public void sendShortFloatCommand(int commonAddress, int ioa, float value, int ql, boolean select) throws IOException {
        if (isConnected()) {
            connection.setShortFloatCommand(commonAddress, CauseOfTransmission.ACTIVATION, ioa,
                    new IeShortFloat(value), new IeQualifierOfSetPointCommand(ql, select));
        }
    }

    public void sendScaledValueCommand(int commonAddress, int ioa, int value, int ql, boolean select) throws IOException {
        if (isConnected()) {
            connection.setScaledValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, ioa,
                    new IeScaledValue(value), new IeQualifierOfSetPointCommand(ql, select));
        }
    }

    public void sendReadCommand(int commonAddress, int ioa) throws IOException {
        if (isConnected()) {
            connection.readCommand(commonAddress, ioa);
        }
    }

    public void sendTestCommand(int commonAddress) throws IOException {
        if (isConnected()) {
            connection.testCommand(commonAddress);
        }
    }

    public void sendResetProcessCommand(int commonAddress, int qualifier) throws IOException {
        if (isConnected()) {
            connection.resetProcessCommand(commonAddress, new IeQualifierOfResetProcessCommand(qualifier));
        }
    }

    public void sendCounterInterrogation(int commonAddress, int request, int freeze) throws IOException {
        if (isConnected()) {
            connection.counterInterrogation(commonAddress, CauseOfTransmission.ACTIVATION,
                    new IeQualifierOfCounterInterrogation(request, freeze));
        }
    }
}
