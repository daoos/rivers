package com.feiniu.writer;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.InstanceConfig;
import com.feiniu.field.RiverField;
import com.feiniu.flow.Flow;
import com.feiniu.model.reader.PipeDataUnit;
import com.feiniu.util.FNException;

/**
 * Flow into Pond Manage
 * create with a/b switch mechanism
 * @author chengwen
 * @version 2.0
 */
@NotThreadSafe
public abstract class WriterFlowSocket extends Flow{
	
	/**batch submit documents*/
	protected Boolean isBatch = true;   
	
	@Override
	public void INIT(HashMap<String, Object> connectParams) {
		this.connectParams = connectParams;
		this.poolName = String.valueOf(connectParams.get("poolName"));
		this.isBatch = GlobalParam.WRITE_BATCH; 
	}   
	
	public abstract boolean create(String instance, String storeId, Map<String,RiverField> transParams);
	
	public abstract String getNewStoreId(String mainName,boolean isIncrement,InstanceConfig instanceConfig);

	public abstract void write(String keyColumn,PipeDataUnit unit,Map<String, RiverField> transParams,String instance, String storeId,boolean isUpdate) throws FNException;

	public abstract void delete(String instance, String storeId,String keyColumn,String keyVal) throws FNException;
  
	public abstract void removeInstance(String instance, String storeId);
	
	public abstract void setAlias(String instance, String storeId, String aliasName);

	public abstract void flush() throws Exception;

	public abstract void optimize(String instance, String storeId);
}
