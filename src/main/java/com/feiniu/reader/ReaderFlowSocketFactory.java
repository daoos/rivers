package com.feiniu.reader;

import java.lang.reflect.Method;
import java.util.HashMap;

import com.feiniu.config.GlobalParam.DATA_TYPE;
import com.feiniu.flow.Socket;
import com.feiniu.model.param.WarehouseNosqlParam;
import com.feiniu.model.param.WarehouseParam;
import com.feiniu.model.param.WarehouseSqlParam;
import com.feiniu.reader.flow.HbaseFlow;
import com.feiniu.reader.flow.MysqlFlow;
import com.feiniu.reader.flow.OracleFlow;
import com.feiniu.util.Common;
 
/**
 * @param args getInstance function parameters:WarehouseParam param, String seq,String handler
 * @author chengwen
 * @version 1.0
 */
public class ReaderFlowSocketFactory implements Socket<ReaderFlowSocket<?>>{
	
	private static ReaderFlowSocketFactory o = new ReaderFlowSocketFactory();
	
	public static ReaderFlowSocket<?> getInstance(Object... args) {
		return o.getSocket(args);
	}
 
	@Override
	public ReaderFlowSocket<?> getSocket(Object... args) {
		WarehouseParam param = (WarehouseParam) args[0];
		String seq = (String) args[1];
		String handler = (String) args[2];  
		if(param instanceof WarehouseSqlParam) {
			return sqlChannel((WarehouseSqlParam) param,seq,handler);
		}else {
			return noSqlChannel((WarehouseNosqlParam) param,seq,handler);
		} 
	} 
 
	private static ReaderFlowSocket<?> sqlChannel(final WarehouseSqlParam params, String seq,String handler){ 
		HashMap<String, Object> connectParams = params.getConnectParams(seq); 
		ReaderFlowSocket<?> reader = null;
		
		if(handler!=null) {
			try {
				Class<?> clz = Class.forName("com.feiniu.reader.handler."+handler);
				Method m = clz.getMethod("getInstance",HashMap.class);
				reader = (ReaderFlowSocket<?>) m.invoke(null,connectParams);
			}catch (Exception e) {
				Common.LOG.error("getNoSqlFlow Exception!",e);
			} 
			return reader;
		}
		switch (params.getType()) {
			case MYSQL:
				reader = MysqlFlow.getInstance(connectParams);
				break;
			case ORACLE:
				connectParams.put("sid", "CORD"); 
				reader = OracleFlow.getInstance(connectParams);
				break;
			default:
				Common.LOG.error("ReaderFlowSocket sqlChannel Type Not Support!");
				break;
		} 
		return reader;
	}
	
	private static ReaderFlowSocket<?> noSqlChannel(WarehouseNosqlParam wParam, String seq,String handler){
		HashMap<String, Object> connectParams = wParam.getConnectParams(seq); 
		if (wParam.getType() == DATA_TYPE.HBASE){ 
			return HbaseFlow.getInstance(connectParams);
		} 
		return null;
	}
	
}