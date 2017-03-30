package com.ysoft.firmata.impl.fsm;

import com.ysoft.firmata.impl.parser.FirmataToken;
import com.ysoft.firmata.impl.parser.WaitingForMessageState;

/**
 *
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public abstract class AbstractCustomState extends AbstractState {

    @Override
    protected void publish(Event event) {
        event.setBodyItem(FirmataToken.CUSTOM_SYSEX_BYTE, getTransitionByte());
        super.publish(event);
    }

    @Override
    public void process(byte b) {
        if (b == FirmataToken.END_SYSEX) {
            Event event = new Event(FirmataToken.CUSTOM_SYSEX_MESSAGE, EventType.FIRMATA_MESSAGE_EVENT_TYPE);
            boolean publishEvent = handleEndSysexState(event);
            transitTo(WaitingForMessageState.class, b);
            if (publishEvent) {
                publish(event);
            }
        } else {
            bufferize(b);
        }
    }

    /**
     * Handle end of custom sysex message. Extract data from buffer to event.
     *
     * @param event
     * @return true - publish event, false - don't publish and transit to
     * {@link  WaitingForMessageState}
     */
    protected abstract boolean handleEndSysexState(Event event);

}
