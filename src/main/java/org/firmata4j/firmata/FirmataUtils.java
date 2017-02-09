package org.firmata4j.firmata;

import java.util.Arrays;

/**
 * Methods that helps with encoding/decoding firmata messages.
 *
 * @author Stepan Novacek &lt;stepan.novacek@ysoft.com&gt;
 */
public class FirmataUtils {

    /**
     * Encodes string message for firmata protocol.
     *
     * @param msg
     * @return
     */
    public static byte[] encodeString(String msg) {
        byte[] bytes = msg.getBytes();
        return encodeBytes(bytes);
    }

    /**
     * Encodes byte array for firmata protocol.
     *
     * @param bytes
     * @return
     */
    public static byte[] encodeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        byte[] result = new byte[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            result[i * 2] = (byte) (b & 0x7F);
            result[i * 2 + 1] = (byte) ((b >> 7) & 0x7F);
        }
        return result;
    }

    /**
     * Decode byte array from Firmata MSB/LSB byte pairs. Dencode byte array
     *
     * @param bytes byte array to decode.
     * @param from the initial index of the range, inclusive
     * @param to the final index of the range, exclusive.
     * @return decoded bytes.
     */
    public static byte[] decodeBytes(byte[] bytes, int from, int to) {
        return decodeBytes(Arrays.copyOfRange(bytes, from, to));
    }

    /**
     * Decode byte array from Firmata MSB/LSB byte pairs. Dencode byte array
     *
     * @param bytes
     * @return decoded bytes.
     */
    public static byte[] decodeBytes(byte[] bytes) {
        int outSize = new Double(Math.floor(bytes.length / 2)).intValue();
        int outIndex = 0;
        byte[] result = new byte[outSize];
        for (int index = 0; index < bytes.length; index = index + 2) {
            result[outIndex++] = (byte) (((bytes[index + 1] << 7) & 0x80) | (bytes[index] & 0x7F));
        }
        return result;
    }

}
