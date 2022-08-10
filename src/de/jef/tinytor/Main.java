package de.jef.tinytor;

public class Main {
public static void main(String[] args) throws Exception {
	byte[] data = new TinyTor().httpGet("google.com");
	System.out.println(data);
}
}
