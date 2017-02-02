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
package org.firmata4j;

import java.io.IOException;

/**
 * Represents an I2C device and encapsulates logic to communicate to it.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public interface I2CDevice {

    /**
     * Returns address of I2C device.
     *
     * @return address of I2C device.
     */
    public byte getAddress();

    /**
     * Sets delay between writing and reading data to/from I2C device.
     *
     * @param delay delay between the moment the device is written to and the moment when the data can be read from it
     * @throws IOException when delay cannot be set due to communication fail
     */
    public void setDelay(int delay) throws IOException;

    /**
     * Sends data to the I2C device.
     *
     * @param data data to send
     * @throws IOException when the data cannot be sent due to communication fail
     */
    public void tell(byte... data) throws IOException;

    /**
     * Registers a listener as a receiver of regular updates from I2C device (on all registers).
     *
     * @param listener the object that receives updates
     */
    public void subscribe(I2CListener listener);

    /**
     * Registers a listener as a receiver of regular updates from I2C device on specified register.
     *
     * @param register
     * @param listener
     */
    void subscribe(byte register, I2CListener listener);

    /**
     * Unregisters a listener from receiving of regular updates from I2C device.
     *
     * @param listener the object that used to receive updates
     */
    public void unsubscribe(I2CListener listener);

    /**
     * Unregisters a listener from receiving of regular updates from I2C device.
     *
     * @param register
     * @param listener the object that used to receive updates
     */
    void unsubscribe(byte register, I2CListener listener);

    /**
     * Tells I2C device to stop sending data continuously.
     *
     * @throws IOException when I2C device cannot be asked to stop due to communication fail
     */
    public void stopReceivingUpdates() throws IOException;

    /**
     * Tells I2C device to stop sending data continuously for specified register.
     *
     * @param register
     * @throws IOException when I2C device cannot be asked to stop due to communication fail
     */
    public void stopReceivingUpdates(byte register) throws IOException;

    /**
     * Tells I2C device to send data.
     *
     * @param register which register to ask.
     * @param responseLength length of expected message in bytes.
     * @param continuous send data continuously or just once.
     * @throws IOException
     */
    void ask(byte register, byte responseLength, boolean continuous) throws IOException;

    /**
     * Tells I2C device to send data register=0.
     *
     * @param responseLength length of expected message in bytes.
     * @param continuous send data continuously or just once.
     * @throws IOException
     */
    void ask(byte responseLength, boolean continuous) throws IOException;

}
