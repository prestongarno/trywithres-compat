package com.prestongarno.testing.util;

/**
 * Created by preston on 4/27/17.
 */
public class TestRuntimeException extends RuntimeException {
	public TestRuntimeException() {
		System.out.println("Throwing a runtime exception in block...");
	}

	@Override
	public String toString() {
		return "TestRuntimeException{}";
	}
}
