package de.jef.tinytor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.Level;

import com.google.common.primitives.Bytes;

public class KeyAgreementNTOR {
	public static byte[] PROTOCOL_ID = "ntor-curve25519-sha256-1".getBytes(StandardCharsets.UTF_8);
	public static byte[] t_mac = Bytes.concat(PROTOCOL_ID, ":mac".getBytes(StandardCharsets.UTF_8));
	public static byte[] t_key = Bytes.concat(PROTOCOL_ID, ":key_extract".getBytes(StandardCharsets.UTF_8));
	public static byte[] t_verify = Bytes.concat(PROTOCOL_ID, ":verify".getBytes(StandardCharsets.UTF_8));
	public static byte[] m_expand = Bytes.concat(PROTOCOL_ID, ":key_expand".getBytes(StandardCharsets.UTF_8));
	private OnionRouter onionRouter;
	private byte[] x;
	private byte[] X;
	private byte[] B;
	private byte[] handshake;

	public KeyAgreementNTOR(OnionRouter onionRouter) {
		this.onionRouter = onionRouter;
		this.x = ED25519.generatePrivateKey();
		this.X = ED25519.x25519PublicFromPrivate(x);

		this.B = Base64.getDecoder().decode(onionRouter.getKeyNtor());
		this.handshake = Bytes.concat(HexFormat.of().parseHex(onionRouter.getIdentity()), this.B, this.X);
	}

	public byte[] getOnionSkin() {
		return this.handshake;
	}

	public static byte[] hmacSha256(byte[] prk, byte[] key) throws GeneralSecurityException {
		Mac sha256HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKey = new SecretKeySpec(prk, "HmacSHA256");
		sha256HMAC.init(secretKey);
		return sha256HMAC.doFinal(key);
	}

	public byte[] kdfRfc5869(byte[] key, int n) throws Exception {
		var prk = hmacSha256(KeyAgreementNTOR.t_key, key);
		var out = new ByteArrayOutputStream();
		var last = new byte[0];
		var i = 1;

		while (out.size() < n) {

			var m = Bytes.concat(last, Utils.int2byte(i++));
			last = hmacSha256(prk, m);
			out.write(last);
		}
		return Arrays.copyOfRange(out.toByteArray(), 0, n);
	}

	public void completeHandshake(byte[] Y, byte[] auth) throws Exception {

		var secretInput = Bytes.concat(ED25519.x25519(x, Y), ED25519.x25519(x, B),
				HexFormat.of().parseHex(onionRouter.getIdentity()), B, X, Y,
				"ntor-curve25519-sha256-1".getBytes(StandardCharsets.UTF_8));

		var verify = hmacSha256(KeyAgreementNTOR.t_verify, secretInput);

		var authInput = Bytes.concat(verify, HexFormat.of().parseHex(onionRouter.getIdentity()), B, Y, X,
				KeyAgreementNTOR.PROTOCOL_ID, "Server".getBytes(StandardCharsets.UTF_8));

		if (!Arrays.equals(auth, hmacSha256(KeyAgreementNTOR.t_mac, authInput))) {
			TinyTor.log.log(Level.ERROR	, "Server handshake doesn't match verification");
			throw new Exception("Server handshake doesn't match verification");
		}
		
		onionRouter.setSharedSecret(kdfRfc5869(secretInput, 72));

	}

}
