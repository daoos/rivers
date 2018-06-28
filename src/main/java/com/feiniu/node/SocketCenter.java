package com.feiniu.node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.NodeConfig;
import com.feiniu.instruction.flow.TransDataFlow;
import com.feiniu.model.param.WarehouseNosqlParam;
import com.feiniu.model.param.WarehouseParam;
import com.feiniu.model.param.WarehouseSqlParam;
import com.feiniu.reader.flow.ReaderFlowSocket;
import com.feiniu.reader.flow.ReaderFlowSocketFactory;
import com.feiniu.searcher.Searcher;
import com.feiniu.searcher.SearcherFactory;
import com.feiniu.searcher.flow.SearcherFlowSocket;
import com.feiniu.util.Common;
import com.feiniu.writer.WriterFactory;
import com.feiniu.writer.flow.WriterFlowSocket;

/**
 * data-flow router reader searcher and writer control center
 * seq only support for reader to read series data source,and create 
 * one or more instance in writer single destination.
 * @author chengwen
 * @version 1.0 
 */
public final class SocketCenter{  
	 
	private Map<String,TransDataFlow> writerChannelMap = new ConcurrentHashMap<String, TransDataFlow>();
	private Map<String, WriterFlowSocket> destinationWriterMap = new ConcurrentHashMap<String, WriterFlowSocket>();
	private Map<String, SearcherFlowSocket> searcherFlowMap = new ConcurrentHashMap<String, SearcherFlowSocket>(); 
	private Map<String, Searcher> searcherMap = new ConcurrentHashMap<String, Searcher>();
	
	private final static Logger log = LoggerFactory.getLogger(SocketCenter.class);  
	  
	
	public Searcher getSearcher(String instanceName) { 
		if(!searcherMap.containsKey(instanceName)) {
			if (!GlobalParam.nodeTreeConfigs.getSearchConfigs().containsKey(instanceName))
				return null; 
			NodeConfig nodeConfig = GlobalParam.nodeTreeConfigs.getSearchConfigs().get(instanceName);
			Searcher searcher = Searcher.getInstance(instanceName, nodeConfig, getSearcherFlow(instanceName));
			searcherMap.put(instanceName, searcher);
		}
		return searcherMap.get(instanceName);
	}
	
	/**
	 * get Writer auto look for sql and nosql maps
	 * @param seq for series data source sequence
	 * @param instanceName data source main tag name
	 * @param needClear for reset resource
	 * @param tag  Marking resource
	 */ 
	public TransDataFlow getWriterChannel(String instanceName, String seq,boolean needClear,String tag) { 
		NodeConfig paramConfig = GlobalParam.nodeTreeConfigs.getNodeConfigs().get(instanceName);
		if(!writerChannelMap.containsKey(Common.getInstanceName(instanceName, seq,null)+tag) || needClear){ 
			TransDataFlow writer=null;
			String readFrom = paramConfig.getPipeParam().getDataFrom(); 
			ReaderFlowSocket<?> flowSocket;
			if(GlobalParam.nodeTreeConfigs.getNoSqlParamMap().get(readFrom)!=null){ 
				Map<String, WarehouseNosqlParam> dataMap = GlobalParam.nodeTreeConfigs.getNoSqlParamMap();
				if (!dataMap.containsKey(readFrom)){
					log.error("data source config " + readFrom + " not exists");
					return null;
				}   
				flowSocket = ReaderFlowSocketFactory.getChannel(dataMap.get(readFrom),seq);
			}else{ 
				Map<String, WarehouseSqlParam> dataMap = GlobalParam.nodeTreeConfigs.getSqlParamMap();
				if (!dataMap.containsKey(readFrom)){
					log.error("data source config " + readFrom + " not exists");
					return null;
				}   
				flowSocket = ReaderFlowSocketFactory.getChannel(dataMap.get(readFrom),seq);
			} 
			writer = TransDataFlow.getInstance(flowSocket,getDestinationWriter(instanceName,seq), paramConfig);
			writerChannelMap.put(Common.getInstanceName(instanceName, seq,null), writer);
		}
		return writerChannelMap.get(Common.getInstanceName(instanceName, seq,null)); 
	}   
	
	public WarehouseParam getWHP(String destination) {
		WarehouseParam param=null;
		if(GlobalParam.nodeTreeConfigs.getNoSqlParamMap().containsKey(destination)) {
			param = GlobalParam.nodeTreeConfigs.getNoSqlParamMap().get(destination);
		}else if(GlobalParam.nodeTreeConfigs.getSqlParamMap().containsKey(destination)) {
			param = GlobalParam.nodeTreeConfigs.getSqlParamMap().get(destination);
		}
		return param;
	}
	
	public WriterFlowSocket getDestinationWriter(String instanceName,String seq) {
		if (!destinationWriterMap.containsKey(instanceName)){
			WarehouseParam param = getWHP(GlobalParam.nodeTreeConfigs.getNodeConfigs().get(instanceName).getPipeParam().getWriteTo()); 
			if (param == null)
				return null;
			destinationWriterMap.put(instanceName, WriterFactory.getWriter(param,seq));
		}    
		return destinationWriterMap.get(instanceName);
	}
	
	private SearcherFlowSocket getSearcherFlow(String secname) {
		if (searcherFlowMap.containsKey(secname))
			return searcherFlowMap.get(secname); 
		WarehouseParam param = getWHP(GlobalParam.nodeTreeConfigs.getSearchConfigs().get(secname).getPipeParam().getSearcher());
		if (param == null)
			return null;
		
		NodeConfig paramConfig = GlobalParam.nodeTreeConfigs.getSearchConfigs().get(secname);
		SearcherFlowSocket searcher = SearcherFactory.getSearcherFlow(param, paramConfig,null);
		searcherFlowMap.put(secname, searcher); 
		return searcher;
	}
	
}