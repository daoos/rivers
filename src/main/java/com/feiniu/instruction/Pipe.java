package com.feiniu.instruction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipe extends Instruction {
	
	private final static Logger log = LoggerFactory.getLogger("Pipe");
	/**
	 * @param args
	 *            parameter order is: String mainName, String storeId
	 */
	public static void create(Context context, Object[] args) {
		if (!isValid(2, args)) {
			log.error("Pipe create parameter not match!");
			return;
		}
		
	}
	public static void remove(Context context, Object[] args) {
		if (!isValid(2, args)) {
			log.error("Pipe remove parameter not match!");
			return;
		}
		
	}
}
