package de.jef.tinytor;

public class Utils {
	public static byte[] uIntToBytes(final long data) {
		return new byte[] { (byte) ((data >> 56) & 0xff), (byte) ((data >> 48) & 0xff), (byte) ((data >> 40) & 0xff),
				(byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
				(byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff), };
	}

	public static byte[] uShortToBytes(final int data) {
		return new byte[] { (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
				(byte) ((data >> 0) & 0xff), };
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
