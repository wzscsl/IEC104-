package com.openmuc.j60870;

/**
 * Listener for capturing all sent and received APDUs (I/S/U format frames).
 * Used by the IEC104 testing tool for packet capture functionality.
 *
 * @since 1.0
 */
public interface FrameListener {

    /**
     * Called when a frame is sent or received.
     *
     * @param apdu       the parsed APDU object
     * @param rawBytes   the raw bytes of the frame
     * @param isReceived true if the frame was received, false if it was sent
     * @param timestamp  the timestamp when the event occurred (milliseconds since epoch)
     */
    void onFrame(APdu apdu, byte[] rawBytes, boolean isReceived, long timestamp);
}
