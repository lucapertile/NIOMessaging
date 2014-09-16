package com.niomessaging.chat;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This is the server side thread of the chat service. 
 * Even if not directly specified in the assignment, I've assumed that frameworks like Netty were not allowed for this exercise,
 * therefore I've decided to try a raw JavaNIO solution to handle everything asynchronously. 
 * Unfortunately I couldn't implement an HTTP wrapper on top of this on time in order to provide a WEB API.
 * I've constrained server/client to talk together under 127.0.0.1:10001
 * 
 * @author Luca Pertile
 *
 */

public class SingleTopicNIOServer implements Runnable {

	private final int port;
	
	public final String DELIMITER ="ç#@";

	private HashMap<String, String> users = new HashMap<String, String>();

	private ServerSocketChannel SSChannel;

	private Selector selector;

	private ByteBuffer tempBuffer = ByteBuffer.allocate(256);


	SingleTopicNIOServer(int port) throws IOException {

		this.SSChannel = ServerSocketChannel.open();

		this.port = port;

		this.SSChannel.socket().bind(new InetSocketAddress(port));

		this.SSChannel.configureBlocking(false);

		this.selector = Selector.open();

		this.SSChannel.register(selector, SelectionKey.OP_ACCEPT);

	}

	@Override
	public void run() {
	 	while(true){
		try {

			System.out.println("ChallengeChat server starting on port " + this.port);

			Iterator<SelectionKey> keys;
			SelectionKey key;

			while (this.SSChannel.isOpen()) {
				// Wait for an event occurring in one of the registered channels
				selector.select();

				keys = selector.selectedKeys().iterator();

				while (keys.hasNext()) {

					key = keys.next();

					keys.remove();

					// Subscribe new user to the topic
					if (key.isAcceptable())
						onSubscribe(key);

					// Get the Object/Text from the Channel
					if (key.isReadable())
						onMessage(key);
				}
			}
		} catch (IOException e) {
			System.out.println("IOException, server of port " + this.port
					+ " terminating. Stack trace:");
			e.printStackTrace();
		}
		
	 	}
	 
	}

	//welcome and error messages on subscribe
	
	private final ByteBuffer welcomeMessage = ByteBuffer
			.wrap("Welcome to the ChallengeChat!\n".getBytes());

	private final ByteBuffer nullUserErrorMessage = ByteBuffer
			.wrap("ERROR name must be not null!\n".getBytes());

	private final ByteBuffer duplicateUserErrorMessage = ByteBuffer
			.wrap("ERROR name already registered!\n".getBytes());

	private final ByteBuffer welcomeBackMessage = ByteBuffer
			.wrap("Welcome back!\n".getBytes());

	
	
	//method called only when a new user connects to the chat room
	private void onSubscribe(SelectionKey key) throws IOException {

		
		SocketChannel socketChannel = ((ServerSocketChannel) key.channel())
				.accept();
		
       
		String address = (new StringBuilder(socketChannel.socket()
				.getInetAddress().toString())).append(":")
				.append(socketChannel.socket().getPort()).toString();

		socketChannel.configureBlocking(false);

		socketChannel.register(selector, SelectionKey.OP_READ, address);
		
		

		
	}
    
	
	private String readMessage(SocketChannel socketChannel, SelectionKey key,String address)
			throws IOException {
	
	


		StringBuilder sb = new StringBuilder();

		tempBuffer.clear();
		int read = 0;

		while ((read = socketChannel.read(tempBuffer)) > 0) {
			tempBuffer.flip();

			byte[] bytes = new byte[tempBuffer.limit()];
			tempBuffer.get(bytes);

			sb.append(new String(bytes));
			tempBuffer.clear();
		}
		String message;
		// Client has hanged up nicely the connection
		if (read == -1) {

			message = users.get(address) + " has left.\n";
			users.remove(address);
			socketChannel.close();

		} else {
			message =  sb.toString();
		}

		return message;

	}

	// Reads a message and broadcasts to all the clients (Topic)
	private void onMessage(SelectionKey key) throws IOException {
		SocketChannel ch = (SocketChannel) key.channel();
		StringBuilder sb = new StringBuilder();
		
		String address = (new StringBuilder(ch.socket()
				.getInetAddress().toString())).append(":")
				.append(ch.socket().getPort()).toString();

		try {
	
			String[] message = readMessage(ch, key,address).split(DELIMITER);
			String name = message[0];
			String content="";
			
	        System.out.println("value: "+name);
			
			// If we don't have a user we store it
			if (!users.containsKey(address) && !users.containsValue(name) ) {

				users.put(address, name);

			
			} 
			
        /*
			// Print current users in chat

			for (String usr : users.keySet()) {

				System.out.println("=======================");

				System.out.println("NAME: " + users.get(address) + " ADDRESS: " + address);

				System.out.println("=======================");

			}	
			
          */// The first message we have a name only, so we start printing from the second
			if (message.length > 1) {
				content = message[1];
				broadcastMessage(name + ": " + content);
			}

		} catch (StringIndexOutOfBoundsException se) {
			se.printStackTrace();
		}
	}

	//sends the message to every channel
	private void broadcastMessage(String message) throws IOException {

		ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes());

		for (SelectionKey key : selector.keys()) {

			if (key.isValid() && key.channel() instanceof SocketChannel) {

				SocketChannel socketChannel = (SocketChannel) key.channel();

				socketChannel.write(messageBuffer);

				messageBuffer.rewind();
			}
		}
	}
	
public static void main(String[] args) {
		
	try{	
		SingleTopicNIOServer server = new SingleTopicNIOServer(10001);
		(new Thread(server)).start();
	}catch(IOException e){
		//Eating IOException
		e.printStackTrace();
		
	}
	
}

}