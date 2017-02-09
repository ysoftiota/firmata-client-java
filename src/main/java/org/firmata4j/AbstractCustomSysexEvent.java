package org.firmata4j;

import org.firmata4j.firmata.parser.FirmataToken;
import org.firmata4j.fsm.Event;
import org.firmata4j.fsm.FiniteStateMachine;

/**
 * Event from custom sysex call handling.
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public abstract class AbstractCustomSysexEvent extends IOEvent {
    
    private byte sysexByte;

    /**
     * 
     * @return identification byte of sysex call.
     */
    public byte getSysexByte() {
        return sysexByte;
    }

    /**
     * Loads data specific for this sysex from FSM event.
     * @param event event from {@link FiniteStateMachine}.
     */
    public void loadContent(Event event) {
        loadCustomContent(event);
        sysexByte = (byte) event.getBodyItem(FirmataToken.CUSTOM_SYSEX_BYTE);
    }

    /**
     * Load properties from event hashmap {@link Event#getBody() }.
     * @param event event from {@link FiniteStateMachine}.
     */
    protected abstract void loadCustomContent(Event event);

}
