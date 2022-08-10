
package de.jef.tinytor;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Utils {
	public static byte[] uIntToBytes(final long data) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(data);
		return Arrays.copyOfRange(bytes, 4, 8);
	}

	public static byte[] uShortToBytes(final int data) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(data);
		return Arrays.copyOfRange(bytes, 2, 4);
	}
	
	public static byte[] uByteToBytes( int n ){
	    byte[] b = new byte[1];
	    b[0] = (byte) (n & 0xff);
	    return b;
	  }
	
	public static long intToUInt(int x) {
	    return x & 0x00000000ffffffffL;
	}
	
	public static byte[] int2byte(int value) {
        if (value < 0) {
            return new byte[] { (byte) (value >>> 24 & 0xFF), (byte) (value >>> 16 & 0xFF),
                    (byte) (value >>> 8 & 0xFF), (byte) (value & 0xFF) };
        } else if (value <= 0xFF) {
            return new byte[] { (byte) (value & 0xFF) };
        } else if (value <= 0xFFFF) {
            return new byte[] { (byte) (value >>> 8 & 0xFF), (byte) (value & 0xFF) };
        } else if (value <= 0xFFFFFF) {
            return new byte[] { (byte) (value >>> 16 & 0xFF), (byte) (value >>> 8 & 0xFF), (byte) (value & 0xFF) };
        } else {
            return new byte[] { (byte) (value >>> 24 & 0xFF), (byte) (value >>> 16 & 0xFF),
                    (byte) (value >>> 8 & 0xFF), (byte) (value & 0xFF) };
        }
    }

}
