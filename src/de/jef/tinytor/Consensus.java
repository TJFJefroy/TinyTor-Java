package de.jef.tinytor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Scanner;


public class Consensus {
	private ArrayList<DirectoryAuthority> directoryAuthorities;
	private ArrayList<OnionRouter> parsedConsensus;

	public Consensus() {
		this.directoryAuthorities = new ArrayList<>();
		directoryAuthorities.add(new DirectoryAuthority("maatuska", "171.25.193.9", 443, 80));
		directoryAuthorities.add(new DirectoryAuthority("tor26", "86.59.21.38", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("longclaw", "199.58.81.140", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("dizum", "194.109.206.212", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("bastet", "204.13.164.118", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("gabelmoo", "131.188.40.189", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("moria1", "128.31.0.34", 9131, 9101));
		directoryAuthorities.add(new DirectoryAuthority("dannenberg", "193.23.244.244", 80, 443));
		directoryAuthorities.add(new DirectoryAuthority("faravahar", "154.35.175.225", 80, 443));

		this.parsedConsensus = new ArrayList<>();

	}

	public DirectoryAuthority getRandomDirectoryAuthority() {
		return directoryAuthorities.get((int) (Math.random() * (directoryAuthorities.size() - 1)));
	}
	
	public void parseConsensus(String consensus_url) throws MalformedURLException, IOException {
		parseConsensus(consensus_url, 200);
	}

	public void parseConsensus(String consensus_url, int limit) throws MalformedURLException, IOException {
		URLConnection con = new URL(consensus_url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:60.0) Gecko/20100101 Firefox/60.0");
		con.setReadTimeout(2 * 1000);
		con.setConnectTimeout(2*1000);

		var onionRouterAmount = 1;
		OnionRouter onionRouter = null;

		Scanner in = new Scanner(con.getInputStream());
		while (in.hasNextLine()) {
			String line = in.nextLine();
			if (line.startsWith("r ")) {
				String[] split_line = line.split(" ");

				String nickname = split_line[1];
				String identity = split_line[2];
				String ip = split_line[6];
				int tor_port = Integer.parseInt(split_line[7]);
				int dir_port = Integer.parseInt(split_line[8]);

				identity = HexFormat.of().formatHex(Base64.getDecoder().decode(identity + "="));

				if (dir_port == 0) {
					onionRouter = null;
					continue;
				}

				onionRouter = new OnionRouter(nickname, ip, dir_port, tor_port, identity);
			} else if (line.startsWith("s ")) {
				if (onionRouter != null) {
					ArrayList<String> flags = new ArrayList<>();
					for (String token : line.split(" ")) {
						if (token.equals("s")) {
							continue;
						}
						flags.add(token.toLowerCase().replaceFirst("\n", ""));
					}

					if (flags.contains("stable") && flags.contains("fast") && flags.contains("valid")
							&& flags.contains("running")) {
						onionRouterAmount += 1;
						onionRouter.setFlags(flags);
						this.parsedConsensus.add(onionRouter);
					}
				}
			}
			if (onionRouterAmount >= limit) {
				TinyTor.log.warn("Stopped after reading " + limit + " onion routers");
				break;
			}

		}
	}

	public OnionRouter getRandomGuardRelay() {
		ArrayList<OnionRouter> guard_relays = new ArrayList<>();

		for (OnionRouter onionRouter : this.parsedConsensus) {
			if (onionRouter.getFlags().contains("guard")) {
				guard_relays.add(onionRouter);
			}
		}

		return guard_relays.get((int) (Math.random() * (guard_relays.size() - 1)));
	}

	public OnionRouter getRandomOnionRouter() {
		return parsedConsensus.get((int) (Math.random() * (parsedConsensus.size() - 1)));
	}

	public OnionRouter getRandomExitRouter() {
		ArrayList<OnionRouter> exit_relays = new ArrayList<>();

		for (OnionRouter onionRouter : this.parsedConsensus) {
			if (onionRouter.getFlags().contains("exit")) {
				exit_relays.add(onionRouter);
			}
		}
		return exit_relays.get((int) (Math.random() * (exit_relays.size() - 1)));
	}

	public ArrayList<OnionRouter> getOnionRouters() {
		return parsedConsensus;
	}
}
