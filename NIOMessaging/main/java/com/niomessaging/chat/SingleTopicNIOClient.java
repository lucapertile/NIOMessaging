package com.niomessaging.chat;

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




import javax.swing.text.DefaultCaret;

import java.util.*;

/**
 * This is the Client side of the chat service. It prompts for a user at the beginning. 
 * It prints the user and notifies of a server/user disconnection. It assumes a server to be listening under 127.0.0.1:10001.
 * It is possible to start multiple clients simply by running multiple copies of this class directly in Eclipse.
 * 
 * @author Luca Pertile
 *
 */


public class SingleTopicNIOClient implements Runnable {

	

	private Selector selector;
    
	//TODO: automate the test of different buffer sizes
	private ByteBuffer tempBuffer = ByteBuffer.allocate(256);

	private String username;

	public SocketChannel socketChannel;

	public  SelectionKey key;

	private JFrame frame = new JFrame("Chat Client");

	private JTextField textField = new JTextField(80);
	
	public final String DELIMITER ="ç#@";

	private JTextArea chatDisplay = new JTextArea(8, 80);
	
	

	SingleTopicNIOClient() throws IOException {

		this.selector = Selector.open();

		textField.setEditable(true);
		chatDisplay.setEditable(false);
		
		frame.getContentPane().add(new JScrollPane(chatDisplay), "Center");

		frame.getContentPane().add(textField, "South");

		frame.pack();

		textField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				try {
					write(key, username+DELIMITER+textField.getText());
				} catch (IOException e1) {
                    
					e1.printStackTrace();
				}

				textField.setText("");
			}
		});

	}

	private String getUsername() {
		return JOptionPane.showInputDialog(frame, "Choose a username:",
				"Username selection", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void run() {
		// starts the gui
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		//sets the user name
		this.username = getUsername();

		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);

			socketChannel.register(selector, SelectionKey.OP_CONNECT);

			socketChannel.connect(new InetSocketAddress("127.0.0.1", 10001));
			
			

			while (!Thread.interrupted()) {

				selector.select();

				Iterator<SelectionKey> keys = selector.selectedKeys()
						.iterator();

				while (keys.hasNext()) {
					key = keys.next();
					keys.remove();

					if (!key.isValid())
						continue;

					if (key.isConnectable()) {
						System.out.println("I am connected to the server");
						onConnect(key);
					}
					if (key.isWritable()) {
						 write(key,username);
					}
					if (key.isReadable()) {
						chatDisplay.append(readMessage(socketChannel, key)+ "\n");
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
		//here I send the first message with a name only
		write(key,username);
		
		
	}

	private void write(SelectionKey key, String msg) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		channel.write(ByteBuffer.wrap(msg.getBytes()));

		// Then we switch in order to get ready to read now..
		key.interestOps(SelectionKey.OP_READ);
	}

	private String readMessage(SocketChannel socketChannel, SelectionKey key)
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
		// Server has hanged up nicely the connection
		if (read == -1) {

			message = key.attachment() + " server disconnected.\n";
			socketChannel.close();

		} else {
			message = sb.toString();
		}

		return message;

	}

	public static void main(String[] args) throws IOException {

		SingleTopicNIOClient client = new SingleTopicNIOClient();
		Thread thread = new Thread(client);
		thread.start();
		

	}

}
