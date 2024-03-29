package de.jef.tinytor;

import static de.jef.tinytor.TinyTor.log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import com.google.common.primitives.Bytes;

import de.jef.tinytor.enums.CommandType;
import de.jef.tinytor.enums.RelayCommand;

public class TorSocket {

	private OnionRouter guardRelay;
	private SSLSocket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private int[] protocolVersion;
	private String ourPublicIp;

	public TorSocket(OnionRouter guardRelay) {
		this.guardRelay = guardRelay;

		this.protocolVersion = new int[] { 3 };
		this.ourPublicIp = "0";

	}

	public OnionRouter getGuardRelay() {
		return this.guardRelay;
	}

	public int getMaxProtocolVersion() {
		int res = protocolVersion[0];

		for (int i = 1; i < protocolVersion.length; i++) {
			res = Math.max(res, protocolVersion[i]);
		}
		return res;
	}

	public void connect() throws IOException, GeneralSecurityException {

		log.info("Connecting socket to the guard relay...");
		
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		
		socket = (SSLSocket) sc.getSocketFactory().createSocket(guardRelay.getIp(), guardRelay.getTorPort());

		out = new DataOutputStream(socket.getOutputStream());
		in = new DataInputStream(socket.getInputStream());

		socket.startHandshake();

		sendVersions();
		retrieveVersions();
		retrieveCerts();
		retrieveNetInfo();
		sendNetInfo();
	}

	public void sendCell(Cell cell) throws IOException {

		out.write(cell.getBytes(getMaxProtocolVersion()));
		out.flush();
	}

	public Cell retrieveCell() throws IOException {
		return retrieveCell(false);
	}

	public Cell retrieveCell(boolean ignoreResponse) throws IOException {

		long circuitId;

		if (getMaxProtocolVersion() < 4) {
			circuitId = in.readUnsignedShort();
		} else {
			circuitId = Utils.intToUInt(in.readInt());
		}

		int command = in.readUnsignedByte();

		byte[] payload = new byte[0];

		if (Cell.isVariableLengthCommand(command)) {
			var payloadLength = in.readUnsignedShort();
			payload = new byte[payloadLength];
			in.readFully(payload);
		} else {
			payload = new byte[Cell.MAXPAYLOADSIZE];
			in.readFully(payload);
		}

		if (!ignoreResponse) {
			if (command == CommandType.VERSIONS.getValue()) {
				ArrayList<Integer> versions = new ArrayList<>();
				DataInputStream pin = new DataInputStream(new ByteArrayInputStream(payload));
				while (true) {
					try {
						versions.add(pin.readUnsignedShort());
					} catch (Exception e) {
						break;
					}
				}
				pin.close();

				JSONObject json = new JSONObject();
				json.put("versions", versions.stream().mapToInt(i -> i).toArray());

				return new Cell(circuitId, command, json);

			} else if (command == CommandType.NETINFO.getValue()) {

				var ourAddressLength = payload[5];
				var ourAddress = InetAddress
						.getByAddress(
								Arrays.copyOfRange(Arrays.copyOfRange(payload, 6, payload.length), 0, ourAddressLength))
						.getHostAddress();

				JSONObject json = new JSONObject();
				json.put("our_address", ourAddress);

				return new Cell(circuitId, command, json);

			} else if (command == CommandType.CREATED2.getValue()) {

				DataInputStream pin = new DataInputStream(new ByteArrayInputStream(payload));

				var dataLength = pin.readUnsignedShort();
				var data = new byte[dataLength];

				pin.readFully(data);
				pin.close();

				byte[] y = Arrays.copyOf(data, 32);
				byte[] auth = Arrays.copyOfRange(data, 32, data.length);

				JSONObject json = new JSONObject();
				json.put("Y", y);
				json.put("auth", auth);

				return new Cell(circuitId, command, json);

			} else if (command == CommandType.RELAY.getValue()) {

				JSONObject json = new JSONObject();
				json.put("encrypted_payload", payload);

				return new Cell(circuitId, command, json);

			} else if (command == CommandType.DESTROY.getValue()) {
				int reason = payload[0];

				JSONObject json = new JSONObject();
				json.put("reason", reason);

				log.warning(String.format("Circuit %s destroyed, reason: %s", circuitId, reason));
				return new Cell(circuitId, command, json);
			} else {

				log.info("-*-*-*-*-*- UNKNOWN_CELL -*-*-*-*-*-");
				log.info("Circuit ID: " + circuitId);
				log.info("Command type: " + command);
				log.info("Payload: " + payload);
				log.info("-*-*-*-*-*- UNKNOWN_CELL -*-*-*-*-*-");

				JSONObject json = new JSONObject();
				json.put("payload", payload);

				return new Cell(circuitId, command, json);

			}
		}
		return null;
	}

	public void sendVersions() throws IOException {
		log.info("Sending VERSIONS cell...");

		JSONObject json = new JSONObject();
		json.put("versions", new int[] { 3, 4 });

		var cell = new Cell(0, CommandType.VERSIONS.getValue(), json);
		sendCell(cell);
	}

	public void retrieveVersions() throws IOException {
		log.info("Retrieving VERSIONS cell...");

		var versionsCell = retrieveCell();

		log.info("Supported link protocol versions: " + Arrays.toString((int[])versionsCell.getPayload().get("versions")));

		this.protocolVersion = (int[]) versionsCell.getPayload().get("versions");
	}

	public void retrieveCerts() throws IOException {
		log.info("Retrieving CERTS cell...");
		retrieveCell(true);

		log.info("Retrieving AUTH_CHALLENGE cell...");
		retrieveCell(true);
	}

	public void retrieveNetInfo() throws IOException {
		log.info("Retrieving NET_INFO cell...");

		var cell = retrieveCell();

		this.ourPublicIp = cell.getPayload().getString("our_address");
		log.info("Our public IP address: " + ourPublicIp);
	}

	public byte[] retrieveRelayData(Circuit circuit) throws IOException {
		var response = new byte[0];

		while (true) {
			var cell = new RelayCell(retrieveCell());

			if (cell.getCommand() == CommandType.RELAY.getValue()) {
				cell.setPayload(circuit.decryptPayload(cell.getPayload()));

				var parsedResponse = cell.parseCell();

				if (parsedResponse.getInt("command") == RelayCommand.RELAY_DATA.getValue()) {

					var dataLength = parsedResponse.getInt("length");
					response = Bytes.concat(response, Arrays.copyOf((byte[]) parsedResponse.get("data"), dataLength));
					if (dataLength < RelayCell.MAXRELAYCELLDATA) {
						return response;
					}
				}
			}
		}
	}
	
	public void sendNetInfo() throws IOException {
        log.info("Sending NET_INFO cell...");

        JSONObject json = new JSONObject();
		json.put("timestamp", System.currentTimeMillis()/1000);
		json.put("other_ip", this.guardRelay.getIp());
		json.put("our_ip", ourPublicIp);

		var cell = new Cell(0, CommandType.NETINFO.getValue(), json);
		sendCell(cell);
	}

}
