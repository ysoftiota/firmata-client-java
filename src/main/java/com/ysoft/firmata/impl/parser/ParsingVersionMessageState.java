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

package com.ysoft.firmata.impl.parser;

import com.ysoft.firmata.impl.fsm.Event;
import com.ysoft.firmata.impl.fsm.AbstractState;

import static com.ysoft.firmata.impl.parser.FirmataToken.*;
import com.ysoft.firmata.impl.fsm.EventType;

/**
 * This state parses version report message that contains the version of the
 * protocol the hardware supports. The version is handed with the event and the
 * FSM is transfered to {@link WaitingForMessageState}.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class ParsingVersionMessageState extends AbstractState {

    private int counter, major;

    @Override
    public void process(byte b) {
        if (counter == 0) {
            major = b;
            counter++;
        } else {
            int minor = b;
            Event event = new Event(PROTOCOL_MESSAGE, EventType.FIRMATA_MESSAGE_EVENT_TYPE);
            event.setBodyItem(PROTOCOL_MAJOR, major);
            event.setBodyItem(PROTOCOL_MINOR, minor);
            transitTo(WaitingForMessageState.class, b);
            publish(event);
        }
    }
}
