package edu.gvsu.prestongarno.testing.util;

/**
 * Created by preston on 4/27/17.
 */
public class OnCloseResourceException extends Exception {
	public OnCloseResourceException(){
		super("An exception occurred when closing this resource!");
	}
}
