package de.jef.tinytor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.primitives.Bytes;

public class PMessageDigest {
	private byte[] buffer;
	private String algo;

	public PMessageDigest(String Algo) {
		algo = Algo;
		this.buffer = new byte[0];
	}

	public void update(byte[] data){
		this.buffer = Bytes.concat(this.buffer,data);

	}

	public byte[] digest() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(algo);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(buffer);
		return md.digest();

	}

}
