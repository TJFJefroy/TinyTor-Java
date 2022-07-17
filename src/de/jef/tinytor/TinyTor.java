package de.jef.tinytor;



import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class TinyTor {
	public static final Logger log = LogManager.getLogger(TinyTor.class);

	public static void main(String[] args) {
		Configurator.setLevel(log, Level.INFO);
		System.out.println("hello world");
	}
}
