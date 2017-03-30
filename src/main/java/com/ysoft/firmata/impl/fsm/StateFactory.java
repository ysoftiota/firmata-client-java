
package com.ysoft.firmata.impl.fsm;

import com.ysoft.firmata.DeviceConfiguration;

/**
 *
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public class StateFactory {
    
    private DeviceConfiguration deviceConfiguration;
    private FiniteStateMachine finiteStateMachine;

    StateFactory(DeviceConfiguration deviceConfiguration, FiniteStateMachine finiteStateMachine) {
        this.deviceConfiguration = deviceConfiguration;
        this.finiteStateMachine = finiteStateMachine;
    }
    
    public <T extends State> T createState(Class<T> stateClass, Byte transitionByte) {
        try {
            T state = stateClass.newInstance();
            state.setDeviceConfiguration(deviceConfiguration);
            state.setFiniteStateMashine(finiteStateMachine);
            state.setTransitionByte(transitionByte);
            return state;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } 
    }
    

}
