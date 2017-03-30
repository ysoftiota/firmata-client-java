package com.ysoft.firmata;

/**
 * Listener to handle events from custom sysex commands.
 * Custom sysex must be registered using {@link DeviceConfiguration#addCustomSysex(byte, java.lang.Class, java.lang.Class).
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 * @param <T> Event type. {@link AbstractCustomSysexEvent}
 */
public interface CustomSysexEventListener<T extends AbstractCustomSysexEvent> {
    
    /**
     * Invoked when device revieves registered custom sysex call.
     * @param sysexByte identifier of sysex call.
     * @param event event.
     */
    void onCustomSysexMessage(byte sysexByte, T event);
    
}
