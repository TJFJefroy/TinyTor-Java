package de.jef.tinytor;

import java.util.ArrayList;
import java.util.logging.Logger;

public class TinyTor {
	public static final Logger log = Logger.getLogger(TinyTor.class+"");
	private Consensus consensus;

	public TinyTor() {
		this.consensus = new Consensus();

		while (true) {
			try {

				var directoryAuthority = consensus.getRandomDirectoryAuthority();
				var consensusUrl = directoryAuthority.getConsensusUrl();

				log.info("Using directory authority " + directoryAuthority.getName() + "...");
				log.info("Consensus URL: " + consensusUrl);
				log.info("Parsing the consensus...");

				consensus.parseConsensus(consensusUrl);
				break;

			} catch (Exception e) {
				log.severe("Failed to parse the consensus:" + e);
				log.severe("Retrying with a different directory authority...");
			}
		}
	}

	public byte[] httpGet(String url) throws Exception {
		OnionRouter guardRelay;

		while (true) {
			try {
				guardRelay = consensus.getRandomGuardRelay();

				log.info("Using guard relay " + guardRelay.getNickname() + "...");
				log.info("Descriptor URL: " + guardRelay.getDescriptorUrl());
				log.info("Parsing the guard relays keys...");

				guardRelay.parseDescriptor();
				break;
			} catch (Exception e) {
				e.printStackTrace();
				log.info("Retrying with a different guard relay...");
			}
		}

		var torSocket = new TorSocket(guardRelay);
		torSocket.connect();

		var circuit = new Circuit(torSocket);
		circuit.create(guardRelay);

		OnionRouter extendRelay, extendRelay2;

		while (true) {

			extendRelay = consensus.getRandomOnionRouter();
			if (!contains(circuit.getOnionRouters(), extendRelay)) {
				break;
			}
		}

		extendRelay.parseDescriptor();
		circuit.extend(extendRelay);

		while (true) {
			extendRelay2 = consensus.getRandomExitRouter();
			if (!contains(circuit.getOnionRouters(), extendRelay2)) {
				break;
			}
		}

		extendRelay2.parseDescriptor();
		circuit.extend(extendRelay2);

		circuit.startStream(url, 80);

		var response = circuit.sendHttpGet();
		return response;

	}

	public boolean contains(ArrayList<OnionRouter> list, OnionRouter onionRouter) {
		for (OnionRouter or : list) {
			if (or.getIdentity().equals(onionRouter.getIdentity())) {
				return true;
			}
		}
		return false;
	}
}
