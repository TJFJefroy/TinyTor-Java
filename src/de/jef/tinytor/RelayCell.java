package de.jef.tinytor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.json.JSONObject;

import de.jef.tinytor.enums.RelayCommand;

public class RelayCell {
	private long circuitId;
	private int command;
	private byte[] payload;

	public RelayCell(Cell cell) {
		this.circuitId = cell.getCircuitId();
		this.command = cell.getCommand();
		this.payload = (byte[]) cell.getPayload().get("encrypted_payload");
	}

	public JSONObject parseCell() throws IOException {

		DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));

		var relayCommand = in.readUnsignedByte();
		var recognized = in.readUnsignedShort();
		var streamId = in.readUnsignedShort();

		var digest = new byte[4];
		in.readFully(digest);

		var length = in.readUnsignedShort();

		var data = new byte[498];
		in.readFully(data);
		in.close();

		JSONObject responseData = new JSONObject();
		responseData.put("command", relayCommand);
		responseData.put("recognized", recognized);
		responseData.put("stream_id", streamId);
		responseData.put("digest", digest);
		responseData.put("length", length);
		responseData.put("data", data);

		if (relayCommand == RelayCommand.RELAY_EXTENDED2.getValue()) {

			in = new DataInputStream(new ByteArrayInputStream(data));

			var data_length = in.readUnsignedShort();
			byte[] data2 = new byte[data_length + 2];
			in.readFully(data2);
			in.close();

			in = new DataInputStream(new ByteArrayInputStream(data2));

			byte[] y = new byte[32];
			byte[] auth = new byte[data2.length - 32];

			in.readFully(y);
			in.readFully(auth);
			in.close();

			responseData.put("Y", y);
			responseData.put("auth", auth);

		} else if (relayCommand == RelayCommand.RELAY_DATA.getValue()
				|| relayCommand == RelayCommand.RELAY_CONNECTED.getValue()
				|| relayCommand == RelayCommand.RELAY_END.getValue()) {
		} else {
			TinyTor.log.log(Level.WARN, "Unsupported relay cell: " + relayCommand);
		}

		return responseData;
	}
}
