package de.jef.tinytor.enums;

public enum CommandType {
	PADDING(0), CREATE(1), CREATED(2), RELAY(3), DESTROY(4), CREATE_FAST(5), CREATED_FAST(6), NETINFO(8),
	RELAY_EARLY(9), CREATE2(10), CREATED2(11), VERSIONS(7), VPADDING(128), CERTS(129), AUTH_CHALLENGE(130),
	AUTHENTICATE(131);

	private int id;

	CommandType(int id) {
		this.id = id;
	}

	public int getValue() {
		return id;
	}
}
