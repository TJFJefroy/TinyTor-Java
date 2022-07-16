package de.jef.tinytor;

public class DirectoryAuthority {
	private String name;
	private String ip;
	private int dir_port;
	private int tor_port;

	public DirectoryAuthority(String name, String ip, int dir_port, int tor_port) {
		this.name = name;
		this.ip = ip;
		this.dir_port = dir_port;
		this.tor_port = tor_port;
	}

	public String getConsensusUrl() {
		return String.format("http://%s:%s/tor/status-vote/current/consensus", this.ip, this.dir_port);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getDir_port() {
		return dir_port;
	}

	public void setDir_port(int dir_port) {
		this.dir_port = dir_port;
	}

	public int getTor_port() {
		return tor_port;
	}

	public void setTor_port(int tor_port) {
		this.tor_port = tor_port;
	}

}
