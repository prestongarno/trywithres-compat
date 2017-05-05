package com.prestongarno.testing.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;

/**
 * Created by preston on 4/27/17.
 ****************************************/
public class TestAcloseable implements AutoCloseable {

	private boolean failOnAction;
	private boolean failOnClose;
	private boolean closed;

	public TestAcloseable() { this(false, false); }

	/*****************************************
	 *
	 * Test class to simulate using Autocloseables
	 *
	 * @param failOnAction makes this autocloseable fail
	 *                       when doRiskyThings() is called. Default value is false
	 * @param failOnClose makes this autocloseable throw a
	 *                      OnCloseResourceException (checked) when the close method is called. Default value is false
	 ****************************************/
	public TestAcloseable(boolean failOnAction, boolean failOnClose) {
		this.failOnAction = failOnAction;
		this.failOnClose = failOnClose;
	}

	public boolean isClosed() {
		return closed;
	}

	public void doRiskyThings() {
		closed = false;
		if (failOnAction) {
			throw new TestRuntimeException();
		} else System.out.println("Performing work normally...");
	}

	@Override
	public void close() throws OnCloseResourceException {
		if(!failOnClose) {
			System.out.println("Closing this Autocloseable...");
			closed = true;
		} else {
			System.out.println("Throwing exception on close()...");
			throw new OnCloseResourceException();
		}
	}
}
