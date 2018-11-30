package com.feiniu.searcher.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.InstanceConfig;
import com.feiniu.model.ResponseState;
import com.feiniu.model.RiverRequest;
import com.feiniu.node.SocketCenter;
import com.feiniu.service.FNService;
import com.feiniu.service.HttpService;
import com.feiniu.util.Common;
import com.feiniu.yarn.Resource;

/**
 * searcher open http port support service
 * @author chengwen
 *
 */
public class SearcherService{
	 
	@Autowired
	private SocketCenter SocketCenter;    
 
	private FNService FS;
	 
	public boolean start() {
		HashMap<String, Object> serviceParams = new HashMap<String, Object>();
		serviceParams.put("confident_port", GlobalParam.StartConfig.get("searcher_service_confident_port"));
		serviceParams.put("max_idle_time", GlobalParam.StartConfig.get("searcher_service_max_idle_time"));
		serviceParams.put("port", GlobalParam.StartConfig.get("searcher_service_port"));
		serviceParams.put("thread_pool", GlobalParam.StartConfig.get("searcher_service_thread_pool"));
		serviceParams.put("httpHandle", new httpHandle());
		FS=HttpService.getInstance(serviceParams);		
		FS.start();
		return true;
	}
	
	public boolean close(){
		if(FS!=null){
			FS.close();
		} 
		return true;
	} 
	
	public ResponseState process(RiverRequest request) throws InstantiationException, IllegalAccessException, ClassNotFoundException { 
		long startTime = System.currentTimeMillis();
		ResponseState response = null; 
		String pipe = request.getPipe(); 
		Map<String, InstanceConfig> configMap = Resource.nodeConfig.getSearchConfigs();
		if (configMap.containsKey(pipe)) {  
			response = SocketCenter.getSearcher(pipe,"","",false).startSearch(request);
		} 
		long endTime = System.currentTimeMillis();
		if (response != null){
			response.setStartTime(startTime);
			response.setEndTime(endTime);
		}  
		return response;
	}

	class httpHandle extends AbstractHandler {
		@Override
		public void handle(String target, HttpServletRequest request,
				HttpServletResponse response, int dispatch) throws IOException,
				ServletException {
			Request rq = (request instanceof Request) ? (Request) request
					: HttpConnection.getCurrentConnection().getRequest();

			response.setContentType("application/json;charset=utf8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setHeader("PowerBy", "rivers"); 
			rq.setHandled(true);
			RiverRequest _request = Common.getRequest(rq);

			if (Resource.nodeConfig.getSearchConfigs().containsKey(
					_request.getPipe())) {
				ResponseState rps = null;
				try {
					rps = process(_request);
				} catch (Exception e) {
					Common.LOG.error("httpHandle error,",e);
				}
				if (rps != null)
					response.getWriter().println(rps.getResponse(true));
			} else {
				response.getWriter().println("The Alias is Not Exists OR Not Start Up!");
			}
			
			response.getWriter().flush();
			response.getWriter().close();
		}
	}  
}
