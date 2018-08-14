package com.feiniu.model.param;

import java.util.HashMap;

import com.feiniu.config.GlobalParam.DATA_TYPE;

public class WarehouseNosqlParam implements WarehouseParam{
	
	private DATA_TYPE type = DATA_TYPE.UNKNOWN;
	private String name;
	private String alias;
	private String ip;
	private String defaultValue;
	private String handler;
	private String[] seqs = {};
	
	public DATA_TYPE getType() {
		return type;
	}
	
	public void setType(String type) {
		if (type.equalsIgnoreCase("SOLR"))
			this.type = DATA_TYPE.SOLR;
		else if (type.equalsIgnoreCase("ES"))
			this.type = DATA_TYPE.ES;
		else if (type.equalsIgnoreCase("HBASE"))
			this.type = DATA_TYPE.HBASE;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	@Override
	public String getHandler() {
		return handler;
	}
	public void setHandler(String handler) {
		this.handler = handler;
	}
	public String getDefaultValue() {
		return this.defaultValue;
	}
	public void setDefaultValue(String defaultvalue) {
		this.defaultValue = defaultvalue;
	} 
	public String getAlias() {
		if(this.alias == null){
			this.alias = this.name;
		}
		return this.alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	@Override
	public String[] getSeq() {
		return this.seqs;
	}
	@Override
	public void setSeq(String seqs) {
		this.seqs = seqs.split(",");
	}

	@Override
	public String getPoolName(String seq) { 
		return ((seq != null) ? this.alias.replace("#{seq}", seq):this.alias)+"_"+this.type+"_"+this.ip;
	}

	@Override
	public HashMap<String, Object> getConnectParams(String seq) {
		HashMap<String, Object> connectParams = new HashMap<String, Object>();
		String name = (seq != null) ? getName().replace("#{seq}", seq) : getName();
		connectParams.put("alias", getAlias());
		connectParams.put("defaultValue", getDefaultValue()); 
		connectParams.put("ip", getIp());
		connectParams.put("name", name); 
		connectParams.put("type", getType()); 
		connectParams.put("poolName", getPoolName(seq));
		return connectParams;
	}	 
}
