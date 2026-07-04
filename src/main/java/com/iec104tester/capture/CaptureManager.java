package com.iec104tester.capture;

import com.openmuc.j60870.APdu;
import com.openmuc.j60870.FrameListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages packet capture sessions. Implements FrameListener to receive
 * all sent and received APDUs from Connection.
 */
public class CaptureManager implements FrameListener {

    private static final int MAX_PACKETS = 100000;

    private final CopyOnWriteArrayList<PacketRecord> packets = new CopyOnWriteArrayList<>();
    private volatile boolean capturing = false;
    private final List<Consumer<PacketRecord>> packetCallbacks = new ArrayList<>();
    private final List<Consumer<Integer>> countCallbacks = new ArrayList<>();

    public void startCapture() {
        capturing = true;
    }

    public void stopCapture() {
        capturing = false;
    }

    public boolean isCapturing() {
        return capturing;
    }

    public void clearPackets() {
        packets.clear();
        notifyCount();
    }

    public List<PacketRecord> getPackets() {
        return new ArrayList<>(packets);
    }

    public int getPacketCount() {
        return packets.size();
    }

    public PacketRecord getPacket(int index) {
        if (index >= 0 && index < packets.size()) {
            return packets.get(index);
        }
        return null;
    }

    public void addPacketCallback(Consumer<PacketRecord> callback) {
        packetCallbacks.add(callback);
    }

    public void addCountCallback(Consumer<Integer> callback) {
        countCallbacks.add(callback);
    }

    @Override
    public void onFrame(APdu apdu, byte[] rawBytes, boolean isReceived, long timestamp) {
        if (!capturing) {
            return;
        }

        PacketRecord record = PacketRecord.fromFrame(apdu, rawBytes, isReceived, timestamp);

        if (packets.size() >= MAX_PACKETS) {
            packets.remove(0);
        }
        packets.add(record);

        for (Consumer<PacketRecord> callback : packetCallbacks) {
            callback.accept(record);
        }
        notifyCount();
    }

    private void notifyCount() {
        int count = packets.size();
        for (Consumer<Integer> callback : countCallbacks) {
            callback.accept(count);
        }
    }

    public void loadPackets(List<PacketRecord> loadedPackets) {
        packets.clear();
        packets.addAll(loadedPackets);
        notifyCount();
    }
}
