package de.jef.tinytor;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.logging.log4j.Level;
import org.json.JSONObject;

import com.google.common.primitives.Bytes;

import de.jef.tinytor.enums.CommandType;

public class Cell {
	public static final int CELLSIZE = 514;
	public static final int MAXPAYLOADSIZE = 509;
	private long circuitId;
	private int command;
	private JSONObject payload;

	public Cell(long circuitId, int command, JSONObject payload) {
		this.circuitId = circuitId;
		this.command = command;
		this.payload = payload;
	}

	public byte[] getBytes(int maxProtocolVersion) throws IOException {
		byte[] payloadbytes = new byte[0];
		if (this.command == CommandType.VERSIONS.getValue()) {

			payloadbytes = new byte[] { 0, 3, 0, 4 };

		} else if (this.command == CommandType.NETINFO.getValue()) {

			var timestamp = Utils.uIntToBytes(payload.getLong("timestamp"));
			var otherOrAddress = Bytes.concat(new byte[] { 4, 4 },
					InetAddress.getByName(this.payload.getString("other_ip")).getAddress());
			var addressN = new byte[] { 1 };
			var thisOrAddress = Bytes.concat(new byte[] { 4, 4 },
					InetAddress.getByName(this.payload.getString("our_ip")).getAddress());

			payloadbytes = Bytes.concat(timestamp, otherOrAddress, addressN, thisOrAddress);

		} else if (this.command == CommandType.CREATE2.getValue()) {

			payloadbytes = Bytes.concat(Utils.uShortToBytes(this.payload.getInt("type")),
					Utils.uShortToBytes(this.payload.getInt("length")), (byte[]) this.payload.get("data"));

		} else if (this.command == CommandType.RELAY_EARLY.getValue() || this.command == CommandType.RELAY.getValue()) {

			payloadbytes = (byte[]) this.payload.get("encrypted_payload");

		} else {

			TinyTor.log.log(Level.ERROR, "Invalid payload format for command: " + this.command);

		}

		if (isVariableLengthCommand(this.command)) {

			byte[] header;
			if (maxProtocolVersion < 4) {

				header = Bytes.concat(Utils.uShortToBytes((int) this.circuitId), Utils.uByteToBytes(this.command),
						Utils.uShortToBytes(payloadbytes.length));
			} else {

				header = Bytes.concat(Utils.uIntToBytes(this.circuitId), Utils.uByteToBytes(this.command),
						Utils.uShortToBytes(payloadbytes.length));
			}

			return Bytes.concat(header, payloadbytes);

		} else {

			if (maxProtocolVersion < 4) {

				payloadbytes = Bytes.concat(Utils.uShortToBytes((int) this.circuitId), Utils.uByteToBytes(this.command),
						payloadbytes, new byte[509 - payloadbytes.length]);

			} else {

				payloadbytes = Bytes.concat(Utils.uIntToBytes(this.circuitId), Utils.uByteToBytes(this.command),
						payloadbytes, new byte[509 - payloadbytes.length]);

			}

			return payloadbytes;

		}
	}

	public boolean isVariableLengthCommand(int command) {
		if (command == CommandType.VERSIONS.getValue() || command >= 128) {
			return true;
		} else {
			return false;
		}
	}

	public long getCircuitId() {
		return circuitId;
	}

	public int getCommand() {
		return command;
	}

	public JSONObject getPayload() {
		return payload;
	}
}
