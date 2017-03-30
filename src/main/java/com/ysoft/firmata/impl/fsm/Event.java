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
package com.ysoft.firmata.impl.fsm;

import java.util.HashMap;
import java.util.Map;

/**
 * The event of very loose structure. It provides possibility to build an event of structure that meets the needs of a
 * particular FSM application.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class Event {

    private final long timestamp;
    private final String name;
    private final EventType type;
    private final Map<String, Object> body = new HashMap<>();


    /**
     * Constructs the event of specified type with specified name.
     *
     * @param name the name of the event
     * @param type the type of the event
     */
    public Event(String name, EventType type) {
        timestamp = System.currentTimeMillis();
        this.name = name;
        this.type = type;
    }


    /**
     * Returns the name of the event.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the timestamp of the event.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the type of the event.
     */
    public EventType getType() {
        return type;
    }

    /**
     * Returns the body of the event.
     */
    public Map<String, Object> getBody() {
        return new HashMap<String, Object>(body);
    }

    /**
     * Returns the item of the event's body.
     *
     * @param key the key of event item
     * @return the event item
     */
    public Object getBodyItem(String key) {
        return body.get(key);
    }

    /**
     * Sets the item of the event's body.
     *
     * @param key the key of event item
     * @param value the event item
     */
    public void setBodyItem(String key, Object value) {
        body.put(key, value);
    }
}
