package de.jef.tinytor;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TinyTor {
	public static final Logger log = LogManager.getLogger(TinyTor.class);
	private Consensus consensus;

	public TinyTor() {
		this.consensus = new Consensus();

		while (true) {
			try {

				var directoryAuthority = consensus.getRandomDirectoryAuthority();
				var consensusUrl = directoryAuthority.getConsensusUrl();

				log.debug("Using directory authority " + directoryAuthority.getName() + "...");
				log.debug("Consensus URL: " + consensusUrl);
				log.debug("Parsing the consensus...");

				consensus.parseConsensus(consensusUrl);
				break;

			} catch (Exception e) {
				log.error("Failed to parse the consensus:" + e);
				log.error("Retrying with a different directory authority...");
			}
		}
	}

	public byte[] httpGet(String url) throws Exception {
		OnionRouter guardRelay;

		while (true) {
			try {
				guardRelay = consensus.getRandomGuardRelay();

				log.debug("Using guard relay " + guardRelay.getNickname() + "...");
				log.debug("Descriptor URL: " + guardRelay.getDescriptorUrl());
				log.debug("Parsing the guard relays keys...");

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
