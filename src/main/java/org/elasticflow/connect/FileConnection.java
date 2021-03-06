package org.elasticflow.connect;

import java.io.RandomAccessFile;

import org.elasticflow.param.pipe.ConnectParams;
import org.elasticflow.param.warehouse.WarehouseNosqlParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileConnection extends FnConnectionSocket<RandomAccessFile> {
	
	private RandomAccessFile conn;

	private final static Logger log = LoggerFactory.getLogger("File Socket");
 
	public static FnConnectionSocket<?> getInstance(ConnectParams connectParams){
		FnConnectionSocket<?> o = new FileConnection();
		o.init(connectParams);  
		return o;
	}
	
	@Override
	public boolean connect() {
		if (!status()) {
			try { 
				this.conn = new RandomAccessFile(String.valueOf(((WarehouseNosqlParam) connectParams.getWhp()).getPath()), "rw");
				return true;
			} catch (Exception e) {
				 log.error("connect Exception,",e);
			}
		} 
		return false;
	}
	
	@Override
	public RandomAccessFile getConnection(boolean searcher) {
		connect();
		return conn;
	}

	@Override
	public boolean status() {
		if(this.conn != null) {
			return true;
		}
		return false;
	}

	@Override
	public boolean free() {
		try {
			this.conn.close();
			this.conn = null;
		} catch (Exception e) {
			 log.error("free connect Exception,",e);
		}
		return false;
	} 
}
