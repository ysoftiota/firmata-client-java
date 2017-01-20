package org.firmata4j;

import java.util.HashMap;
import java.util.Map;
import org.firmata4j.fsm.State;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;

/**
 * Configuration of device.
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public class DeviceConfiguration {

    public static final int BAUD_RATE_300 = 300;
    public static final int BAUD_RATE_1200 = 1200;
    public static final int BAUD_RATE_2400 = 2400;
    public static final int BAUD_RATE_4800 = 4800;
    public static final int BAUD_RATE_9600 = 9600;
    public static final int BAUD_RATE_14400 = 14400;
    public static final int BAUD_RATE_19200 = 19200;
    public static final int BAUD_RATE_28800 = 28800;
    public static final int BAUD_RATE_38400 = 38400;
    public static final int BAUD_RATE_57600 = 57600;
    public static final int BAUD_RATE_115200 = 115200;

    private int serialPortBaudRate = BAUD_RATE_57600;
    private DataBits serialPortDataBits = DataBits.DATABITS_8;
    private StopBits serialPortStopBits = StopBits.STOPBITS_1;
    private Parity serialPortParity = Parity.PARITY_NONE;
    private long initializationTimeout = 15000L;

    private final CommPortIdentifier commPortIdentifier;
    private Map<Byte, Class<? extends State>> customSysexStates = new HashMap<>();
    private Map<Byte, Class<? extends AbstractCustomSysexEvent>> customSysexEvents = new HashMap<>();

    /**
     * Constructor.
     * @param portName comm port identification ("COM5, "/dev/ttyACM").
     */
    public DeviceConfiguration(String portName) {
        try {
            this.commPortIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        } catch (NoSuchPortException ex) {
            throw new IllegalArgumentException("Communications port " + portName + " not found or connected.");
        }
    }

    /**
     * Register handers for custom sysex commands.
     * @param sysex sysex identification byte.
     * @param sysexState FSM state which can handle this command.
     * @param event event class.
     * @return 
     */
    public DeviceConfiguration addCustomSysex(byte sysex, Class<? extends State> sysexState, Class<? extends AbstractCustomSysexEvent> event) {
        customSysexStates.put(sysex, sysexState);
        customSysexEvents.put(sysex, event);
        return this;
    }

    /**
     * Set baud rate for comm port.
     * @param serialPortBaudRate
     * @return 
     */
    public DeviceConfiguration setSerialPortBaudRate(int serialPortBaudRate) {
        this.serialPortBaudRate = serialPortBaudRate;
        return this;
    }

    /**
     * Set data bits.
     * @param serialPortDataBits
     * @return 
     */
    public DeviceConfiguration setSerialPortDataBits(DataBits serialPortDataBits) {
        if (serialPortDataBits == null) {
            throw new IllegalArgumentException("null value not allowed");
        }
        this.serialPortDataBits = serialPortDataBits;
        return this;
    }

    /**
     * Set initialization timeout.
     * @param initializationTimeout
     * @return 
     */
    public DeviceConfiguration setInitializationTimeout(long initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
        return this;
    }
    
    /**
     * Set stop bits.
     * @param serialPortStopBits
     * @return 
     */
    public DeviceConfiguration setSerialPortStopBits(StopBits serialPortStopBits) {
        if (serialPortStopBits == null) {
            throw new IllegalArgumentException("null value not allowed");
        }
        this.serialPortStopBits = serialPortStopBits;
        return this;
    }

    /**
     * Set parity.
     * @param serialPortParity
     * @return 
     */
    public DeviceConfiguration setSerialPortParity(Parity serialPortParity) {
        if (serialPortParity == null) {
            throw new IllegalArgumentException("null value not allowed");
        }
        this.serialPortParity = serialPortParity;
        return this;
    }

    public CommPortIdentifier getCommPortIdentifier() {
        return commPortIdentifier;
    }

    public int getSerialPortBaudRate() {
        return serialPortBaudRate;
    }

    public DataBits getSerialPortDataBits() {
        return serialPortDataBits;
    }

    public StopBits getSerialPortStopBits() {
        return serialPortStopBits;
    }

    public Parity getSerialPortParity() {
        return serialPortParity;
    }

    public long getInitializationTimeout() {
        return initializationTimeout;
    }

    public Class<? extends State> getCustomSysexState(byte b) {
        return customSysexStates.get(b);
    }
    
    public Class<? extends AbstractCustomSysexEvent> getCustomSysexEvent(byte b) {
        return customSysexEvents.get(b);
    }

    public enum Parity {

        /**
         * EVEN parity scheme. The parity bit is added so there are an even number of TRUE bits.
         */
        PARITY_EVEN(2),
        /**
         * MARK parity scheme.
         */
        PARITY_MARK(3),
        /**
         * No parity bit.
         */
        PARITY_NONE(0),
        /**
         * ODD parity scheme. The parity bit is added so there are an odd number of TRUE bits.
         */
        PARITY_ODD(1),
        /**
         * SPACE parity scheme.
         */
        PARITY_SPACE(4);

        private int value;

        public int getValue() {
            return value;
        }

        private Parity(int value) {
            this.value = value;
        }

    }

    public enum DataBits {
        /**
         * 5 data bit format.
         */
        DATABITS_5(5),
        /**
         * 6 data bit format.
         */
        DATABITS_6(6),
        /**
         * 7 data bit format.
         */
        DATABITS_7(7),
        /**
         * 8 data bit format.
         */
        DATABITS_8(8);

        private int value;

        public int getValue() {
            return value;
        }

        private DataBits(int value) {
            this.value = value;
        }
    }

    public enum StopBits {

        /**
         * Number of STOP bits - 1.
         */
        STOPBITS_1(1),
        /**
         * Number of STOP bits - 1-1/2. Some UARTs permit 1-1/2 STOP bits only with 5 data bit format, but permit 1 or 2
         * STOP bits with any format.
         */
        STOPBITS_1_5(3),
        /**
         * Number of STOP bits - 2.
         */
        STOPBITS_2(2);

        private int value;

        public int getValue() {
            return value;
        }

        private StopBits(int value) {
            this.value = value;
        }
    }
    
}
