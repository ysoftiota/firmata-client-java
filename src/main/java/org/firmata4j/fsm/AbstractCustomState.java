
package org.firmata4j.fsm;

import org.firmata4j.firmata.parser.FirmataToken;

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

}
