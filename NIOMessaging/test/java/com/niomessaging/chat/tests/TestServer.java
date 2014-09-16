package com.niomessaging.chat.tests;

import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;

import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.google.code.tempusfugit.concurrency.annotations.Concurrent;
import com.niomessaging.chat.SingleTopicNIOClient;

/**
 * 
 * Test class in order to understand the performance/hidden bugs when trying manually
 * It relies on a slimmed down version of the client without GUI. Unfortunately it is still incomplete.
 * The idea was to test multiple users using tempus-fugit.
 * 
 * @author Luca Pertile
 *
 */

//@RunWith(ConcurrentTestRunner.class)
public class TestServer {



	@Test
	public void testLoadSingleUser() throws IOException {
		SingleTopicNIOClientForTesting client = new SingleTopicNIOClientForTesting(
				"sample\n", 100000,"TESTER");
		Thread thread = new Thread(client);
		thread.start();
	}

}
