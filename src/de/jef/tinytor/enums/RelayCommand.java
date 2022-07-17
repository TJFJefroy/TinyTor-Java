package de.jef.tinytor.enums;

public enum RelayCommand {
	RELAY_BEGIN(1), RELAY_DATA(2), RELAY_END(3), RELAY_CONNECTED(4), RELAY_SENDME(5), RELAY_EXTEND(6),
	RELAY_EXTENDED(7), RELAY_TRUNCATE(8), RELAY_TRUNCATED(9), RELAY_DROP(10), RELAY_RESOLVE(11), RELAY_RESOLVED(12),
	RELAY_BEGIN_DIR(13), RELAY_EXTEND2(14), RELAY_EXTENDED2(15);

	private int value;
	RelayCommand(int value) {
		this.value = value;
	}
	public int getValue() {
		return value;
	}
}
