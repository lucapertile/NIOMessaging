/*
 * Copyright (c) 2014.  Luca Pertile
 *
 * "THE BEER-WARE LICENSE" (Revision 3.14159)
 * As long as you retain this notice you can do whatever you want with this stuff.
 * If we meet some day, and you think this stuff is worth it, you can buy me a beer in return
 * There is no warranty.
 */

package com.niomessaging.chat.tests;

import java.net.*;
import java.nio.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.channels.*;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class SingleTopicNIOClientForTesting implements Runnable {

	private HashMap<String, String> users;

	private Selector selector;

	//TODO: automate the test of different buffer sizes
	private ByteBuffer buf = ByteBuffer.allocate(256);

	private Logger LOGGER = LogManager
			.getLogger(SingleTopicNIOClientForTesting.class.getName());

	private String username;

	public SocketChannel socketChannel;
	
	public final String DELIMITER ="ç#@";

	public SelectionKey key;

	private String payload;

	public int messagesToSend;

	SingleTopicNIOClientForTesting(String payload, int messagesToSend,
			String username) throws IOException {

		this.selector = Selector.open();

		this.messagesToSend = messagesToSend;

		this.payload = payload;

		this.username = username;

	}

	@Override
	public void run() {

		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);

			socketChannel.register(selector, SelectionKey.OP_CONNECT);

			socketChannel.connect(new InetSocketAddress("127.0.0.1", 10001));

			while (!Thread.interrupted()) {
				int i=0;
				while(i<messagesToSend){

				selector.select();

				Iterator<SelectionKey> keys = selector.selectedKeys()
						.iterator();

				while (keys.hasNext()) {

					key = keys.next();
					keys.remove();

					if (key.isConnectable()) {
						// once we get into a channel where we can connect we
						// then set the channel to OP_WRITE
						onConnect(key);

					}
					if (key.isWritable()) {
						    i++;
							send(key, username+DELIMITER+payload+" #"+i );
					

					}

				}
				}
				
			}

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	private void onConnect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();

		if (channel.isConnectionPending()) {
			channel.finishConnect();
		}
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_WRITE);
		//send(key, username);

	}

	private void send(SelectionKey key, String msg) throws IOException {

		SocketChannel channel = (SocketChannel) key.channel();

		channel.write(ByteBuffer.wrap(msg.getBytes()));

		// Then we switch in order to get ready to write again now..
		key.interestOps(SelectionKey.OP_WRITE);
	}

}
