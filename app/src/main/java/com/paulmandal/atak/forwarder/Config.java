package com.paulmandal.atak.forwarder;

public class Config {
    /**
     * Pre-shared Key Length, 16 for AES128, 32 for AES256
     */
    public static final int PSK_LENGTH = 32;

    /**
     * How long to wait after attempting to stop the service when the Paired button is clicked
     */
    public static final int DELAY_AFTER_STOPPING_SERVICE = 5000;

    /**
     * How long to wait for radioConfig to be available when writing to a Tracker device
     */
    public static final int RADIO_CONFIG_MISSING_RETRY_TIME_MS = 10000;

    /**
     * Meshtastic Radio Config
     */
    public static final int POSITION_BROADCAST_INTERVAL_S = 3600;
    public static final int LCD_SCREEN_ON_S = 5;
    public static final int WAIT_BLUETOOTH_S = 86400;
    public static final int PHONE_TIMEOUT_S = 86400;
    public static final int DEVICE_CONNECTION_TIMEOUT = 30000;

    /**
     * Tweaks to message handling
     */
    public static final int MESHTASTIC_MESSAGE_CHUNK_LENGTH = 200;
    public static final int MESSAGE_AWAIT_TIMEOUT_MS = 70000;
    public static final int REJECT_STALE_NODE_CHANGE_TIME_MS = 1800000;

    /**
     * TAG prefix for all debug messages
     */
    public static final String DEBUG_TAG_PREFIX = "ATAKDBG.";
}
