package com.feiniu.writer.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feiniu.config.InstanceConfig;
import com.feiniu.model.SearcherModel;
import com.feiniu.model.PipeDataUnit;
import com.feiniu.model.param.TransParam;
import com.feiniu.writer.WriterFlowSocket;

/**
 * HBase flow Writer Manager
 * @author chengwen
 * @version 1.0 
 */
@ThreadSafe
public class HBaseFlow extends WriterFlowSocket { 
	 
	private List<Put> data = new CopyOnWriteArrayList<Put>();  
	private Table conn;
	private final static Logger log = LoggerFactory.getLogger("HBaseFlow"); 
	
	public static HBaseFlow getInstance(HashMap<String, Object> connectParams) {
		HBaseFlow o = new HBaseFlow();
		o.INIT(connectParams);
		return o;
	}
	
	@Override
	public void INIT(HashMap<String, Object> connectParams) {
		this.connectParams = connectParams;  
		String tableColumnFamily = (String) this.connectParams.get("defaultValue");
		if (tableColumnFamily != null && tableColumnFamily.length() > 0) {
			String[] strs = tableColumnFamily.split(":");
			if (strs != null && strs.length > 0)
				this.connectParams.put("tableName", strs[0]);
			if (strs != null && strs.length > 1)
				this.connectParams.put("columnFamily", strs[1]);
		}
		this.poolName = String.valueOf(connectParams.get("poolName"));;
		retainer.set(0);
	} 
	
	@Override
	public boolean LINK(){
		synchronized(retainer){
			if(retainer.get()==0){
				GETSOCKET(false);
				if(!super.LINK())
					return false; 
				this.conn = (Table) this.FC.getConnection(false);
			} 
			retainer.addAndGet(1); 
			return true;
		} 
	} 
	  
	@Override
	public void write(String keyColumn,PipeDataUnit unit,Map<String, TransParam> transParams, String instantcName, String storeId,boolean isUpdate) throws Exception { 
		if (unit.getData().size() == 0){
			log.info("Empty IndexUnit for " + instantcName + " " + storeId);
			return;
		}  
		String id = unit.getKeyColumnVal(); 
		Put put = new Put(Bytes.toBytes(id));
		
		for(Entry<String, Object> r:unit.getData().entrySet()){
			String field = r.getKey(); 
			if (r.getValue() == null)
				continue;
			String value = String.valueOf(r.getValue());
			if (field.equalsIgnoreCase("update_time") && value!=null)
				value = String.valueOf(System.currentTimeMillis());
			
			if (value == null)
				continue;
			
			TransParam transParam = transParams.get(field);
			if (transParam == null)
				transParam = transParams.get(field.toLowerCase());
			if (transParam == null)
				transParam = transParams.get(field.toUpperCase());
			if (transParam == null)
				continue; 
			put.addColumn(Bytes.toBytes((String)connectParams.get("columnFamily")), Bytes.toBytes(transParam.getAlias()),
					Bytes.toBytes(value));  
		} 
		synchronized (data) {
			data.add(put); 
		}
	} 

	@Override
	public void delete(SearcherModel<?, ?, ?> query, String instance, String storeId) throws Exception {
		
	}

	@Override
	public void removeInstance(String instanceName, String batchId) {
		
	}

	@Override
	public void setAlias(String instanceName, String batchId, String aliasName) {

	}

	@Override
	public void flush() throws Exception { 
		synchronized (data) {
			this.conn.put(data);
			data.clear();
		} 
	}

	@Override
	public void optimize(String instantcName, String batchId) {
		
	}
 
	@Override
	public boolean create(String instantcName, String batchId, Map<String, TransParam> transParams) {
		return true;
	}

	@Override
	public String getNewStoreId(String instanceName,boolean isIncrement,String dbseq, InstanceConfig instanceConfig) {
		// TODO Auto-generated method stub
		return "a";
	}
 
}
