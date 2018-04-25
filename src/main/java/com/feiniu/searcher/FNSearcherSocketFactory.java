package com.feiniu.searcher;

import java.util.HashMap;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.NodeConfig;
import com.feiniu.model.param.WarehouseNosqlParam;
import com.feiniu.model.param.WarehouseParam;
import com.feiniu.searcher.flow.ESFlow;
import com.feiniu.searcher.flow.MysqlFlow;
import com.feiniu.searcher.flow.SearcherFlowSocket;
import com.feiniu.searcher.flow.SolrFlow;

public class FNSearcherSocketFactory {

	public static SearcherFlowSocket getSearcherFlow(final WarehouseParam param, final NodeConfig nodeConfig,
			String seq) {
		if (param instanceof WarehouseNosqlParam) {
			return getNosqlFlowSocket(param, nodeConfig, seq);
		} else {
			return getSqlFlowSocket(param, nodeConfig, seq);
		}
	}

	private static SearcherFlowSocket getNosqlFlowSocket(WarehouseParam params, NodeConfig NodeConfig, String seq) {
		HashMap<String, Object> connectParams = params.getConnectParams(seq);
		connectParams.put("nodeConfig", NodeConfig);
		connectParams.put("handler", params.getHandler());
		connectParams.put("analyzer", GlobalParam.SEARCH_ANALYZER);
		SearcherFlowSocket searcher = null;
		switch (params.getType()) {
		case ES:
			searcher = ESFlow.getInstance(connectParams);
			break;
		case SOLR:
			searcher = SolrFlow.getInstance(connectParams);
			break;
		default:
			break;
		}
		return searcher;
	}

	private static SearcherFlowSocket getSqlFlowSocket(WarehouseParam params, NodeConfig NodeConfig, String seq) {
		HashMap<String, Object> connectParams = params.getConnectParams(seq);
		SearcherFlowSocket searcher = null;
		switch (params.getType()) {
		case MYSQL:
			searcher = MysqlFlow.getInstance(connectParams);
			break;
		default:
			break;
		}
		return searcher;
	}
}
