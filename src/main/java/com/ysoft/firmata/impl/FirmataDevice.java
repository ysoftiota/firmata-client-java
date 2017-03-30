/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Oleg Kurbatov (o.v.kurbatov@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.ysoft.firmata.impl;

import com.ysoft.firmata.DeviceConfiguration;
import com.ysoft.firmata.I2CDevice;
import com.ysoft.firmata.IODevice;
import com.ysoft.firmata.IODeviceEventListener;
import com.ysoft.firmata.IOEvent;
import com.ysoft.firmata.Pin;
import com.ysoft.firmata.impl.parser.WaitingForMessageState;
import com.ysoft.firmata.impl.fsm.Event;
import com.ysoft.firmata.impl.fsm.FiniteStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.codec.binary.Hex;
import com.ysoft.firmata.AbstractCustomSysexEvent;
import com.ysoft.firmata.CustomSysexEventListener;
import com.ysoft.firmata.DeviceCommunicationException;
import com.ysoft.firmata.impl.parser.FirmataToken;
import static com.ysoft.firmata.impl.parser.FirmataToken.*;
import purejavacomm.CommPortIdentifier;
import purejavacomm.PortInUseException;
import purejavacomm.PureJavaSerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;
import purejavacomm.UnsupportedCommOperationException;

/**
 * Implements {@link IODevice} that is using Firmata protocol.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class FirmataDevice implements IODevice, SerialPortEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmataDevice.class);

    private final BlockingQueue<byte[]> byteQueue = new ArrayBlockingQueue<>(128);
    private final FirmataParser parser;
    private final Thread parserExecutor;
    private final Set<IODeviceEventListener> listeners = Collections.synchronizedSet(new LinkedHashSet<IODeviceEventListener>());
    private final Set<CustomSysexEventListener> customSysexListeners = Collections.synchronizedSet(new LinkedHashSet<CustomSysexEventListener>());
    private final List<FirmataPin> pins = Collections.synchronizedList(new ArrayList<FirmataPin>());
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicInteger initializedPins = new AtomicInteger(0);
    private final AtomicInteger longestI2CDelay = new AtomicInteger(0);
    private final Map<Byte, FirmataI2CDevice> i2cDevices = new HashMap<>();
    protected PureJavaSerialPort commPort;
    private DeviceConfiguration deviceConfiguration;

    private volatile Map<String, Object> firmwareInfo;
    private volatile Map<Integer, Integer> analogMapping;

    /**
     * Constructs FirmataDevice instance on specified port.
     *
     * @param deviceConfiguration firmata configuration.
     */
    public FirmataDevice(DeviceConfiguration deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
        this.parser = new FirmataParser(byteQueue, deviceConfiguration);
        this.parserExecutor = new Thread(parser, "firmata-parser-thread");
    }

    @Override
    public void start() throws IOException {
        if (!started.getAndSet(true)) {
            parserExecutor.start();
            /*
            The startup strategy is to open the port and immediately
            send the REPORT_FIRMWARE message.  When we receive the
            firmware name reply, then we know the board is ready to
            communicate.
            For boards like Arduino which use DTR to reset, they may
            reboot the moment the port opens.  They will not hear this
            REPORT_FIRMWARE message, but when they finish booting up
            they will send the firmware message.
            For boards that do not reboot when the port opens, they
            will hear this REPORT_FIRMWARE request and send the
            response.  If this REPORT_FIRMWARE request isn't sent,
            these boards will not automatically send this info.
            Arduino boards that reboot on DTR will act like a board
            that does not reboot, if DTR is not raised when the
            port opens.  This program attempts to avoid raising
            DTR on windows.  (is this possible on Linux and Mac OS-X?)
            Either way, when we hear the REPORT_FIRMWARE reply, we
            know the board is alive and ready to communicate.
             */
            CommPortIdentifier commPortIdentifier = deviceConfiguration.getCommPortIdentifier();
            try {
                commPort = (PureJavaSerialPort) commPortIdentifier.open(
                        FirmataDevice.class.getName(),
                        2000);
            } catch (PortInUseException e) {
                LOGGER.error("Cannot open communications port {}. Cannot obtain ownership!", commPortIdentifier.getName());
                throw new IOException("Cannot start firmata device", e);
            }

            try {
                commPort.setSerialPortParams(deviceConfiguration.getSerialPortBaudRate(),
                        deviceConfiguration.getSerialPortDataBits().getValue(),
                        deviceConfiguration.getSerialPortStopBits().getValue(),
                        deviceConfiguration.getSerialPortParity().getValue());
            } catch (UnsupportedCommOperationException e) {
                LOGGER.error("Unable to configure communications port {} to baud rate {}.", commPortIdentifier.getName(),
                        deviceConfiguration.getSerialPortBaudRate());
                throw new IOException("Unable to configure communications port", e);
            }
            commPort.notifyOnDataAvailable(true);
            commPort.notifyOnOutputEmpty(true);
            try {
                commPort.addEventListener(this);
            } catch (TooManyListenersException ex) {
                throw new IllegalStateException(ex);
            }

            sendMessage(FirmataMessageFactory.REQUEST_FIRMWARE);
        }
    }

    @Override
    public void stop() throws IOException {
        shutdown();
        parserExecutor.interrupt();
        try {
            parserExecutor.join();
        } catch (InterruptedException ex) {
            LOGGER.warn("Cannot stop parser thread", ex);
        } finally {
            IOEvent event = new IOEvent(this);
            for (IODeviceEventListener l : listeners) {
                l.onStop(event);
            }
        }
    }

    @Override
    public void ensureInitializationIsDone() throws InterruptedException {
        if (!started.get()) {
            try {
                start();
            } catch (IOException ex) {
                throw new InterruptedException(ex.getMessage());
            }
        }
        long timePassed = 0L;
        long timeout = 100;
        while (!isReady()) {
            if (timePassed >= deviceConfiguration.getInitializationTimeout()) {
                throw new InterruptedException("Connection timeout");
            }
            timePassed += timeout;
            Thread.sleep(timeout);
        }
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public void addEventListener(IODeviceEventListener listener) {
        listeners.add(listener);
    }

    public void addCustomSysexEventListener(CustomSysexEventListener listener) {
        customSysexListeners.add(listener);
    }

    @Override
    public void removeEventListener(IODeviceEventListener listener) {
        listeners.remove(listener);
    }

    public void removeCustomSysexEventListener(CustomSysexEventListener listener) {
        customSysexListeners.remove(listener);
    }

    @Override
    public Set<Pin> getPins() {
        return new HashSet<>(pins);
    }

    @Override
    public int getPinsCount() {
        return pins.size();
    }

    @Override
    public Pin getPin(int index) {
        return pins.get(index);
    }

    @Override
    public synchronized I2CDevice getI2CDevice(byte address) throws DeviceCommunicationException {
        if (!i2cDevices.containsKey(address)) {
            i2cDevices.put(address, new FirmataI2CDevice(this, address));
        }
        sendMessage(FirmataMessageFactory.i2cConfigRequest(longestI2CDelay.get()));
        return i2cDevices.get(address);
    }

    @Override
    public String getProtocol() {
        return MessageFormat.format(
                "{0} - {1}.{2}",
                firmwareInfo.get(FirmataToken.FIRMWARE_NAME),
                firmwareInfo.get(FirmataToken.FIRMWARE_MAJOR),
                firmwareInfo.get(FirmataToken.FIRMWARE_MINOR));
    }

    @Override
    public void sendMessage(String message) throws DeviceCommunicationException {
        if (message.length() > 15) {
            LOGGER.warn("Firmata 2.3.6 implementation has input buffer only 32 bytes so you can safely send only 15 characters log messages");
        }
        sendMessage(FirmataMessageFactory.stringMessage(message));
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        // queueing data from input buffer to processing by FSM logic
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE && event.getNewValue()) {
            try {
                int available = commPort.getInputStream().available();
                final byte[] readBytes = new byte[available];

                commPort.getInputStream().read(readBytes);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Recieved serial event:'{}'", Hex.encodeHexString(readBytes));
                }
//                System.out.println("Recieved serial event:"+ Hex.encodeHexString(readBytes));
                while (!byteQueue.offer(readBytes)) {
                    // trying to place bytes to queue until it succeeds
                }
            } catch (IOException ex) {
                LOGGER.error("Cannot read from device", ex);
            }
        }
    }

    @Override
    public void sendCustomSysex(byte sysex, byte[] data) throws DeviceCommunicationException {
        sendMessage(FirmataMessageFactory.customSysex(sysex, FirmataUtils.encodeBytes(data)));
    }

    @Override
    public void sendCustomSysex(byte sysex, String stringMessage) throws DeviceCommunicationException {
        sendCustomSysex(sysex, stringMessage == null ? null : stringMessage.getBytes());
    }

    /**
     * Sends the message to connected Firmata device using open port.<br/>
     * This method is package-wide accessible to be used by {@link FirmataPin}.
     *
     * @param msg the Firmata message
     * @throws DeviceCommunicationException when writing fails
     */
    void sendMessage(byte[] msg) throws DeviceCommunicationException {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending message: '{}'", Hex.encodeHexString(msg));
            }
            commPort.getOutputStream().write(msg);
        } catch (IOException ex) {
            throw new DeviceCommunicationException("Cannot send message to device", ex);
        }
    }

    /**
     * Notifies the device listeners that a pin has changed.<br/>
     * This method is package-wide accessible to be used by {@link FirmataPin}.
     *
     * @param event the event to be send to the listeners
     */
    void pinChanged(IOEvent event) {
        listeners.forEach(listener -> listener.onPinChange(event));
    }

    /**
     * Sets delay between the moment an I2C device's register is written to and the moment when the data can be read
     * from that register. The delay is set per firmata-device (not per I2C device). So firmata-device uses the longest
     * delay.
     *
     * @param delay
     * @throws DeviceCommunicationException when sending of configuration to firmata-device failed
     */
    void setI2CDelay(int delay) throws DeviceCommunicationException {
        byte[] message = FirmataMessageFactory.i2cConfigRequest(delay);
        int longestDelaySoFar = longestI2CDelay.get();
        while (longestDelaySoFar < delay) {
            if (longestI2CDelay.compareAndSet(longestDelaySoFar, delay)) {
                sendMessage(message);
            }
            longestDelaySoFar = longestI2CDelay.get();
        }
    }

    /**
     * Tries to release all resources and properly terminate the connection to the hardware.
     *
     * @throws DeviceCommunicationException when communication could not be stopped properly
     */
    private void shutdown() throws DeviceCommunicationException {
        ready.set(false);
        sendMessage(FirmataMessageFactory.analogReport(false));
        sendMessage(FirmataMessageFactory.digitalReport(false));
        commPort.close();
    }

    /**
     * Describes reaction to protocol receiving.
     *
     * @param event the event of receiving protocol version
     */
    private void onProtocolReceive(Event event) {
        if (!event.getBodyItem(PROTOCOL_MAJOR).equals((int) FIRMATA_MAJOR_VERSION)) {
            LOGGER.error(
                    MessageFormat.format(
                            "Current version of firmata protocol on device ({0}.{1}) is not compatible with version of fimata4j ({2}.{3}).",
                            event.getBodyItem(PROTOCOL_MAJOR),
                            event.getBodyItem(PROTOCOL_MINOR),
                            FIRMATA_MAJOR_VERSION,
                            FIRMATA_MINOR_VERSION));
        } else if (!event.getBodyItem(PROTOCOL_MINOR).equals((int) FIRMATA_MINOR_VERSION)) {
            LOGGER.warn(
                    MessageFormat.format(
                            "Current version of firmata protocol on device ({0}.{1}) differs from version supported by frimata4j ({2}.{3})."
                            + " Though these are compatible you may experience some issues.",
                            event.getBodyItem(PROTOCOL_MAJOR),
                            event.getBodyItem(PROTOCOL_MINOR),
                            FIRMATA_MAJOR_VERSION,
                            FIRMATA_MINOR_VERSION));
        }
    }

    /**
     * Describes reaction to firmware data receiving.
     *
     * @param event the event of receiving firmware data
     */
    private void onFirmwareReceive(Event event) {
        firmwareInfo = event.getBody();
        try {
            sendMessage(FirmataMessageFactory.REQUEST_CAPABILITY);
        } catch (DeviceCommunicationException ex) {
            LOGGER.error("Error requesting device capabilities.", ex);
        }
    }

    /**
     * Describes rection to capabilities data receiving.
     *
     * @param event the event of receiving capabilities data
     */
    private void onCapabilitiesReceive(Event event) {
        byte pinId = (Byte) event.getBodyItem(PIN_ID);
        FirmataPin pin = new FirmataPin(this, pinId);
        for (byte i : (byte[]) event.getBodyItem(PIN_SUPPORTED_MODES)) {
            pin.addSupprotedMode(Pin.Mode.resolve(i));
        }
        pins.add(pin.getIndex(), pin);
        if (pin.getSupportedModes().isEmpty()) {
            // if the pin has no supported modes, its initialization is already done
            initializedPins.incrementAndGet();
        } else {
            // if the pin supports some modes, we ask for its current mode and value
            try {
                sendMessage(FirmataMessageFactory.pinStateRequest(pinId));
            } catch (DeviceCommunicationException ex) {
                LOGGER.error(String.format("Error requesting state of pin %d", pin.getIndex()), ex);
            }
        }
    }

    /**
     * Describes reaction to the pin state data receiving.
     *
     * @param event the event of receiving pin state data
     */
    private void onPinStateRecieve(Event event) {
        byte pinId = (Byte) event.getBodyItem(PIN_ID);
        FirmataPin pin = pins.get(pinId);
        if (pin.getMode() == null) {
            pin.initMode(Pin.Mode.resolve((Byte) event.getBodyItem(PIN_MODE)));
            pin.initValue((Long) event.getBodyItem(PIN_VALUE));
        } else {
            pin.updateValue((Long) event.getBodyItem(PIN_VALUE));
        }
        if (initializedPins.incrementAndGet() == pins.size()) {
            try {
                sendMessage(FirmataMessageFactory.ANALOG_MAPPING_REQUEST);
            } catch (DeviceCommunicationException e) {
                LOGGER.error("Error on request analog mapping", e);
            }
        }
    }

    /**
     * Describes reaction to the analog mapping data receiving.
     *
     * @param event the event of receiving analog mapping data
     */
    @SuppressWarnings("unchecked")
    private void onAnalogMappingReceive(Event event) {
        analogMapping = (Map<Integer, Integer>) event.getBodyItem(ANALOG_MAPPING);
        try {
            sendMessage(FirmataMessageFactory.analogReport(true));
            sendMessage(FirmataMessageFactory.digitalReport(true));
        } catch (DeviceCommunicationException ex) {
            LOGGER.error("Cannot enable reporting from device", ex);
        }
        ready.set(true);
        // all the pins are initialized so notification is sent to listeners
        IOEvent initIsDone = new IOEvent(this);
        listeners.forEach(l -> l.onStart(initIsDone));
    }

    /**
     * Describes reaction to the analog message data receiving.
     *
     * @param event the event of receiving analog message data
     */
    private void onAnalogMessageReceive(Event event) {
        int analogId = (Integer) event.getBodyItem(PIN_ID);
        if (analogMapping != null && analogMapping.get(analogId) != null) {
            int pinId = analogMapping.get(analogId);
            if (pinId < pins.size()) {
                FirmataPin pin = pins.get(pinId);
                if (Pin.Mode.ANALOG.equals(pin.getMode())) {
                    pin.updateValue((Integer) event.getBodyItem(PIN_VALUE));
                }
            }
        }
    }

    /**
     * Describes reaction to the digital message data receiving.
     *
     * @param event the event of receiving digital message data
     */
    private void onDigitalMessageReceive(Event event) {
        int pinId = (Integer) event.getBodyItem(PIN_ID);
        if (pinId < pins.size()) {
            FirmataPin pin = pins.get(pinId);
            if (Pin.Mode.INPUT.equals(pin.getMode())
                    || Pin.Mode.PULLUP.equals(pin.getMode())) {
                pin.updateValue((Integer) event.getBodyItem(PIN_VALUE));
            }
        }
    }

    private void onI2cMessageReceive(Event event) {
        byte address = (Byte) event.getBodyItem(I2C_ADDRESS);
        byte register = (Byte) event.getBodyItem(I2C_REGISTER);
        byte[] message = (byte[]) event.getBodyItem(I2C_MESSAGE);
        FirmataI2CDevice device = i2cDevices.get(address);
        if (device != null) {
            device.onReceive(register, message);
        }
    }

    private void onCustomSysexMessage(Event event) {
        byte b = (byte) event.getBodyItem(CUSTOM_SYSEX_BYTE);
        Class<? extends AbstractCustomSysexEvent> eventClass = deviceConfiguration.getCustomSysexEvent(b);
        try {
            AbstractCustomSysexEvent newEvent = eventClass.newInstance();
            newEvent.loadContent(event);
            customSysexListeners.forEach(l -> l.onCustomSysexMessage(b, newEvent));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void onStringMessageReceive(Event event) {
        String message = (String) event.getBodyItem(STRING_MESSAGE);
        IOEvent evt = new IOEvent(this);
        listeners.forEach(listener -> listener.onMessageReceive(evt, message));
    }

    private class FirmataParser extends FiniteStateMachine implements Runnable {

        private final BlockingQueue<byte[]> queue;

        public FirmataParser(BlockingQueue<byte[]> queue, DeviceConfiguration configuration) {
            super(WaitingForMessageState.class, configuration);
            this.queue = queue;
        }

        @Override
        public void onEvent(Event event) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Event name: {}, type: {}, timestamp: {}", new Object[]{event.getName(), event.getType(), event.getTimestamp()});
                for (Map.Entry<String, Object> entry : event.getBody().entrySet()) {
                    LOGGER.debug("{}: {}", entry.getKey(), entry.getValue());
                }
                LOGGER.debug("\n");
            }

            switch (event.getName()) {
                case PROTOCOL_MESSAGE:
                    onProtocolReceive(event);
                    break;
                case FIRMWARE_MESSAGE:
                    onFirmwareReceive(event);
                    break;
                case PIN_CAPABILITIES_MESSAGE:
                    onCapabilitiesReceive(event);
                    break;
                case PIN_STATE:
                    onPinStateRecieve(event);
                    break;
                case ANALOG_MAPPING_MESSAGE:
                    onAnalogMappingReceive(event);
                    break;
                case ANALOG_MESSAGE_RESPONSE:
                    onAnalogMessageReceive(event);
                    break;
                case DIGITAL_MESSAGE_RESPONSE:
                    onDigitalMessageReceive(event);
                    break;
                case STRING_MESSAGE:
                    onStringMessageReceive(event);
                    break;
                case I2C_MESSAGE:
                    onI2cMessageReceive(event);
                    break;
                case CUSTOM_SYSEX_MESSAGE:
                    onCustomSysexMessage(event);
                    break;
                case FiniteStateMachine.FSM_IS_IN_TERMINAL_STATE:
                    // should never happen but who knows
                    throw new IllegalStateException("Parser has reached the terminal state. It may be due receiving of unsupported command.");
            }
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    process(queue.take());
                } catch (InterruptedException ex) {
                    LOGGER.info("FirmataParser has stopped");
                    return;
                }
            }
        }

    }

}
