package com.ysoft.firmata;

/**
 *
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public class DeviceCommunicationException extends RuntimeException{

    public DeviceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

}
