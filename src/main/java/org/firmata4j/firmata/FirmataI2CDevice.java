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
package org.firmata4j.firmata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.firmata4j.DeviceCommunicationException;
import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;

/**
 * Represents an I2C device and encapsulates communication logic using Firmata protocol.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class FirmataI2CDevice implements I2CDevice {

    private final FirmataDevice masterDevice;

    private final byte address;

    private Map<Byte, Set<I2CListener>> registerSubscribers = Collections.synchronizedMap(new HashMap<>());
    private final Set<I2CListener> subscribers = Collections.synchronizedSet(new HashSet<I2CListener>());

    FirmataI2CDevice(FirmataDevice masterDevice, byte address) {
        this.masterDevice = masterDevice;
        this.address = address;
    }

    @Override
    public byte getAddress() {
        return address;
    }

    @Override
    public void setDelay(int delay) throws DeviceCommunicationException {
        masterDevice.setI2CDelay(delay);
    }

    @Override
    public void tell(byte... data) throws DeviceCommunicationException {
        masterDevice.sendMessage(FirmataMessageFactory.i2cWriteRequest(address, data));
    }

    @Override
    public void ask(byte register, byte responseLength, boolean continuous) throws DeviceCommunicationException {
        masterDevice.sendMessage(FirmataMessageFactory.i2cReadRequest(address, register, responseLength, continuous));
    }

    @Override
    public void ask(byte responseLength, boolean continuous) throws DeviceCommunicationException {
        ask((byte) 0, responseLength, continuous);
    }

    @Override
    public void subscribe(byte register, I2CListener listener) {
        Set<I2CListener> l = registerSubscribers.get(register);
        if (l == null) {
            l = Collections.synchronizedSet(new HashSet<>());
            registerSubscribers.put(register, l);
        }
        l.add(listener);
    }

    @Override
    public void subscribe(I2CListener listener) {
        subscribers.add(listener);
    }

    @Override
    public void unsubscribe(I2CListener listener) {
        subscribers.remove(listener);
    }

    @Override
    public void unsubscribe(byte register, I2CListener listener) {
        Set<I2CListener> listeners = registerSubscribers.get(register);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public void stopReceivingUpdates() throws DeviceCommunicationException {
        stopReceivingUpdates((byte) 0);
    }

    @Override
    public void stopReceivingUpdates(byte register) throws DeviceCommunicationException {
        masterDevice.sendMessage(FirmataMessageFactory.i2cStopContinuousRequest(address, register));
    }

    /**
     * {@link FirmataDevice} calls this method when receives a message from I2C device.
     *
     * @param register The register acts as a tag on returned data helping to identify the matching returned message to
     * the request. Continuous updates are received with 0 register.
     * @param message actual data from I2C device
     */
    void onReceive(byte register, byte[] message) {
        I2CEvent evt = new I2CEvent(this, register, message);
        Set<I2CListener> registerSubscriber = registerSubscribers.get(register);
        if (registerSubscriber != null) {
            registerSubscriber.forEach(listener -> listener.onReceive(evt));
        }
        subscribers.forEach(listener -> listener.onReceive(evt));
    }

}
