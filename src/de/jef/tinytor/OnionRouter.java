package de.jef.tinytor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.primitives.Bytes;

public class OnionRouter {
	private final IvParameterSpec iv = new IvParameterSpec(
			new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

	private String nickname;
	private String ip;
	private int dirPort;
	private int torPort;
	private String identity;
	private String keyNtor;
	private SecretKeySpec decryptionKey;
	private MessageDigest backwardDigest;
	private SecretKeySpec encryptionKey;
	private MessageDigest forwardDigest;
	private Cipher forwardCipher;
	private Cipher backwardCipher;
	private ArrayList<String> flags;

	public OnionRouter(String nickname, String ip, int dirPort, int torPort, String identity) {
		this.nickname = nickname;
		this.ip = ip;
		this.dirPort = dirPort;
		this.torPort = torPort;
		this.identity = identity;

	}

	public String getDescriptorUrl() {
		return String.format("http://%s:%s/tor/server/fp/%s", this.ip, this.dirPort, this.identity);
	}

	public void parseDescriptor() throws MalformedURLException, IOException {
		URLConnection con = new URL(getDescriptorUrl()).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0");

		Scanner in = new Scanner(con.getInputStream());
		while (in.hasNextLine()) {
			String line = in.nextLine();

			if (line.startsWith("ntor-onion-key ")) {
				this.keyNtor = line.split("ntor-onion-key")[1].strip();

				if (!this.keyNtor.endsWith("=")) {
					keyNtor += "=";
				}
				break;
			}

		}
		in.close();
	}

	public void setSharedSecret(byte[] data) throws InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, NoSuchPaddingException {
		var forward_digest = Arrays.copyOfRange(data, 0, 20);
		var backward_digest = Arrays.copyOfRange(data, 20, 40);
		this.encryptionKey = new SecretKeySpec(Arrays.copyOfRange(data, 40, 56), "AES");
		this.decryptionKey = new SecretKeySpec(Arrays.copyOfRange(data, 56, 72), "AES");

		this.forwardDigest = setDigest(forward_digest);
		this.backwardDigest = setDigest(backward_digest);

		this.forwardCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.forwardCipher.init(Cipher.ENCRYPT_MODE, this.encryptionKey, iv);

		this.backwardCipher = Cipher.getInstance("AES/CTR/NoPadding");
		this.backwardCipher.init(Cipher.DECRYPT_MODE, this.decryptionKey, iv);
	}

	public MessageDigest setDigest(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(data);
		return md;
	}

	public byte[] getForwardDigest(byte[] data) {
		this.forwardDigest.update(data);
		return this.forwardDigest.digest();
	}

	public byte[] getBackwardDigest(byte[] data) {
		byte[] result = Bytes.concat(Arrays.copyOfRange(data, 0, 5), new byte[] { 0, 0, 0, 0 },
				Arrays.copyOfRange(data, 9, data.length));
		this.backwardDigest.update(result);
		return this.backwardDigest.digest();
	}

	public byte[] encrypt(byte[] relay_payload) {
		return this.forwardCipher.update(relay_payload);
	}

	public byte[] decrypt(byte[] relay_payload) {
		return this.forwardCipher.update(relay_payload);
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getDirPort() {
		return dirPort;
	}

	public void setDirPort(int dirPort) {
		this.dirPort = dirPort;
	}

	public int getTorPort() {
		return torPort;
	}

	public void setTorPort(int torPort) {
		this.torPort = torPort;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getKeyNtor() {
		return keyNtor;
	}

	public void setKeyNtor(String keyNtor) {
		this.keyNtor = keyNtor;
	}

	public SecretKeySpec getDecryptionKey() {
		return decryptionKey;
	}

	public void setDecryptionKey(SecretKeySpec decryptionKey) {
		this.decryptionKey = decryptionKey;
	}

	public MessageDigest getBackwardDigest() {
		return backwardDigest;
	}

	public void setBackwardDigest(MessageDigest backwardDigest) {
		this.backwardDigest = backwardDigest;
	}

	public SecretKeySpec getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(SecretKeySpec encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public MessageDigest getForwardDigest() {
		return forwardDigest;
	}

	public void setForwardDigest(MessageDigest forwardDigest) {
		this.forwardDigest = forwardDigest;
	}

	public Cipher getForwardCipher() {
		return forwardCipher;
	}

	public void setForwardCipher(Cipher forwardCipher) {
		this.forwardCipher = forwardCipher;
	}

	public Cipher getBackwardCipher() {
		return backwardCipher;
	}

	public void setBackwardCipher(Cipher backwardCipher) {
		this.backwardCipher = backwardCipher;
	}

	public ArrayList<String> getFlags() {
		return flags;
	}

	public void setFlags(ArrayList<String> flags) {
		this.flags = flags;
	}

	public IvParameterSpec getIv() {
		return iv;
	}

}
