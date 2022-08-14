package de.jef.tinytor;

import static de.jef.tinytor.TinyTor.log;

import java.io.IOException;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Random;

import org.json.JSONObject;

import com.google.common.primitives.Bytes;

import de.jef.tinytor.enums.CommandType;
import de.jef.tinytor.enums.RelayCommand;

public class Circuit {
	private TorSocket torSocket;
	private long circuitId;
	private ArrayList<OnionRouter> onionRouters;
	private int streamId;

	public Circuit(TorSocket torSocket) {
		this.torSocket = torSocket;

		Random r = new Random();

		if (torSocket.getMaxProtocolVersion() < 4) {

			this.circuitId = r.nextLong(0, ((long) Math.pow(2, 16)) - 1);

		} else {

			this.circuitId = r.nextLong((long) Math.pow(2, 31), ((long) Math.pow(2, 32)) - 1);

		}

		this.onionRouters = new ArrayList<OnionRouter>();
		this.streamId = 0;
	}

	public TorSocket getTorSocket() {
		return torSocket;
	}

	public long getCircuitId() {
		return circuitId;
	}

	public ArrayList<OnionRouter> getOnionRouters() {
		return onionRouters;
	}

	public void create(OnionRouter guardRelay) throws Exception {

		log.info("Creating new circuit...");
		log.info("Circuit ID:" + circuitId);
		var keyAgreement = new KeyAgreementNTOR(guardRelay);

		var json = new JSONObject();
		json.put("type", 2);
		json.put("length", keyAgreement.getOnionSkin().length);
		json.put("data", keyAgreement.getOnionSkin());

		this.torSocket.sendCell(new Cell(getCircuitId(), CommandType.CREATE2.getValue(), json));

		Cell cell = this.torSocket.retrieveCell();

		if (cell.getCommand() != CommandType.CREATED2.getValue()) {
			log.severe("Received command is not a CREATED2.");
			throw new Exception("Received command is not a CREATED2.");

		}

		keyAgreement.completeHandshake((byte[]) cell.getPayload().get("Y"), (byte[]) cell.getPayload().get("auth"));
		onionRouters.add(guardRelay);
		log.info("Handshake verified, onion router's shared secret has been set.");

	}

	public byte[] createRelayCell(int command, int stream_id, byte[] payload) {
		var relayCell = Bytes.concat(new byte[] { (byte) command }, Utils.uShortToBytes(0),
				Utils.uShortToBytes(stream_id), new byte[4], Utils.uShortToBytes(payload.length), payload,
				new byte[498 - payload.length]);

		var calDigest = Arrays.copyOf(getOnionRouters().get(getOnionRouters().size() - 1).getForwardDigest(relayCell),
				4);
		relayCell[5] = calDigest[0];
		relayCell[6] = calDigest[1];
		relayCell[7] = calDigest[2];
		relayCell[8] = calDigest[3];

		return encryptPayload(relayCell);
	}

	public void startStream(String address, int port) throws Exception {

		this.streamId++;

		log.info("Starting a stream with id: " + streamId);


		var relayPayload = Bytes.concat((address + ":" + port).getBytes(StandardCharsets.UTF_8), new byte[5]);

		var relayCell = createRelayCell(RelayCommand.RELAY_BEGIN.getValue(), streamId, relayPayload);

		var json = new JSONObject();
		json.put("encrypted_payload", relayCell);

		var cell = new Cell(getCircuitId(), CommandType.RELAY.getValue(), json);

		getTorSocket().sendCell(cell);

		var responseCell = new RelayCell(getTorSocket().retrieveCell());
		responseCell.setPayload(decryptPayload(responseCell.getPayload()));

		var parsedResponse = responseCell.parseCell();

		if (parsedResponse.getInt("command") != RelayCommand.RELAY_CONNECTED.getValue()) {

			log.severe("Creating a connection to the address failed");
			throw new Exception("Creating a connection to the address failed");
		}
	}

	public byte[] sendHttpGet() throws IOException {
		var relayPayload = "GET / HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

		var relayCell = createRelayCell(RelayCommand.RELAY_DATA.getValue(), streamId, relayPayload);

		var json = new JSONObject();
		json.put("encrypted_payload", relayCell);

		var cell = new Cell(getCircuitId(), CommandType.RELAY.getValue(), json);

		getTorSocket().sendCell(cell);

		var responseData = getTorSocket().retrieveRelayData(this);
		return responseData;

	}

	public void extend(OnionRouter onionRouter) throws Exception {

		log.info("Extending the circuit to " + onionRouter.getNickname() + "...");

		var keyAgreement = new KeyAgreementNTOR(onionRouter);

		var relayPayload = Bytes.concat(new byte[] { 2, 0, 6 },
				Inet4Address.getByName(onionRouter.getIp()).getAddress(), Utils.uShortToBytes(onionRouter.getTorPort()),
				new byte[] { 2, 20 }, HexFormat.of().parseHex(onionRouter.getIdentity()), Utils.uShortToBytes(2),
				Utils.uShortToBytes(keyAgreement.getOnionSkin().length), keyAgreement.getOnionSkin());

		var relayCell = createRelayCell(RelayCommand.RELAY_EXTEND2.getValue(), 0, relayPayload);

		var json = new JSONObject();
		json.put("encrypted_payload", relayCell);

		var cell = new Cell(getCircuitId(), CommandType.RELAY_EARLY.getValue(), json);

		getTorSocket().sendCell(cell);


		var responseCell = new RelayCell(getTorSocket().retrieveCell());

		if (responseCell.getCommand() != CommandType.RELAY.getValue()) {
			log.severe("Received command is not a RELAY.");
			throw new Exception("Received command is not a RELAY.");
		}

		responseCell.setPayload(decryptPayload(responseCell.getPayload()));

		var parsedResponse = responseCell.parseCell();

		keyAgreement.completeHandshake((byte[]) parsedResponse.get("Y"), (byte[]) parsedResponse.get("auth"));
		
		onionRouters.add(onionRouter);
	}

	@SuppressWarnings("unchecked")
	public byte[] encryptPayload(byte[] datat) {
		byte[] data = datat.clone();
		var cop = (ArrayList<OnionRouter>) this.onionRouters.clone();
		Collections.reverse(cop);

		for (OnionRouter or : cop) {
			data = or.encrypt(data);
		}
		return data;
	}

	public byte[] decryptPayload(byte[] datat) {
		var data = datat.clone();
		

		for (OnionRouter or : onionRouters) {
			data = or.decrypt(data);

			if (Arrays.equals(Arrays.copyOfRange(data, 1, 3), new byte[2])) {

				var digest = Arrays.copyOf(or.getBackwardDigest(data), 4);

				if (Arrays.equals(Arrays.copyOfRange(data, 5, 9), digest)) {
					return data;
				}

			}

		}
		return data;

	}

}
