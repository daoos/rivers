package com.feiniu.task;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.GlobalParam.STATUS;
import com.feiniu.config.InstanceConfig;
import com.feiniu.node.CPU;
import com.feiniu.piper.PipePump;
import com.feiniu.util.Common;
import com.feiniu.util.FNException;
import com.feiniu.yarn.Resource;
 
/**
 * schedule task description,to manage task job
 * @author chengwen
 * @version 2.0
 * @date 2018-11-27 10:23
 */
public class FlowTask {

	private boolean recompute = true;
	private boolean masterControl = false;
	private String instance;
	private PipePump transDataFlow;
	/**
	 * seq for scan series datas
	 */
	private String L1seq = "";

	private final static Logger log = LoggerFactory.getLogger(FlowTask.class);

	public static FlowTask createTask(String instanceName, PipePump transDataFlow) {
		return new FlowTask(instanceName, transDataFlow, GlobalParam.DEFAULT_RESOURCE_SEQ);
	}

	public static FlowTask createTask(String instanceName, PipePump transDataFlow, String L1seq) {
		return new FlowTask(instanceName, transDataFlow, L1seq);
	}

	private FlowTask(String instance, PipePump transDataFlow, String L1seq) {
		this.instance = instance;
		this.transDataFlow = transDataFlow;
		this.L1seq = L1seq;
		if (transDataFlow.getInstanceConfig().getPipeParams().getInstanceName() != null)
			masterControl = true;
	}

	/**
	 * if no full job will auto open optimize job
	 */
	public void optimizeInstance() {
		String storeName = Common.getMainName(instance, L1seq);
		CPU.RUN(transDataFlow.getID(), "Pond", "optimizeInstance", true, storeName,
				Common.getStoreId(instance, L1seq, transDataFlow, true, false));
	}

	/**
	 * slave instance full job
	 */
	public void runFull() {
		if (Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.FULL.name(),STATUS.Ready,STATUS.Running)) {
			try {
				GlobalParam.SCAN_POSITION.get(Common.getMainName(instance, L1seq)).keepCurrentPos();
				String storeId;
				if (masterControl) {
					storeId = Resource.FLOW_INFOS
							.get(transDataFlow.getInstanceConfig().getPipeParams().getInstanceName(),
									GlobalParam.FLOWINFO.MASTER.name())
							.get(GlobalParam.FLOWINFO.FULL_STOREID.name());
				} else {
					storeId = Common.getStoreId(instance, L1seq, transDataFlow, false, false);
					CPU.RUN(transDataFlow.getID(), "Pond", "createStorePosition", true,
							Common.getMainName(instance, L1seq), storeId);
				}
				if(storeId!=null) {
					transDataFlow.run(instance, storeId, L1seq, true,
							masterControl);
					GlobalParam.SCAN_POSITION.get(Common.getMainName(instance, L1seq)).recoverKeep(); 
					Common.saveTaskInfo(instance, L1seq, storeId, GlobalParam.JOB_INCREMENTINFO_PATH);
				} 
			} catch (Exception e) {
				log.error(instance + " Full Exception", e);
			} finally {
				Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.FULL.name(),STATUS.Blank,STATUS.Ready);
			}
		}
	}

	public void runMasterFull() {
		if (Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.FULL.name(),STATUS.Ready,STATUS.Running)) {
			try {
				String storeId = Common.getStoreId(instance, L1seq, transDataFlow, false, false);
				if (!Resource.FLOW_INFOS.containsKey(instance, GlobalParam.FLOWINFO.MASTER.name())) {
					Resource.FLOW_INFOS.set(instance, GlobalParam.FLOWINFO.MASTER.name(),
							new HashMap<String, String>());
				}
				Resource.FLOW_INFOS.get(instance, GlobalParam.FLOWINFO.MASTER.name())
						.put(GlobalParam.FLOWINFO.FULL_STOREID.name(), storeId);

				CPU.RUN(transDataFlow.getID(), "Pond", "createStorePosition", true,
						Common.getMainName(instance, L1seq), storeId);

				Resource.FLOW_INFOS.get(instance, GlobalParam.FLOWINFO.MASTER.name()).put(
						GlobalParam.FLOWINFO.FULL_JOBS.name(),
						getNextJobs(transDataFlow.getInstanceConfig().getPipeParams().getNextJob()));

				for (String slave : transDataFlow.getInstanceConfig().getPipeParams().getNextJob()) {
					Resource.FlOW_CENTER.runInstanceNow(slave, "full");
				}
			} catch (Exception e) {
				log.error(instance + " Full Exception", e);
			} finally {
				Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.FULL.name(),STATUS.Blank,STATUS.Ready);
			}
		}
	}

	public void runMasterIncrement() {
		if (Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.INCREMENT.name(),STATUS.Ready,STATUS.Running)) {
			String storeId = Common.getStoreId(instance, L1seq, transDataFlow, true, recompute);
			if (!Resource.FLOW_INFOS.containsKey(instance, GlobalParam.FLOWINFO.MASTER.name())) {
				Resource.FLOW_INFOS.set(instance, GlobalParam.FLOWINFO.MASTER.name(),
						new HashMap<String, String>());
			}
			Resource.FLOW_INFOS.get(instance, GlobalParam.FLOWINFO.MASTER.name())
					.put(GlobalParam.FLOWINFO.INCRE_STOREID.name(), storeId);
			try {
				for (String slave : transDataFlow.getInstanceConfig().getPipeParams().getNextJob()) {
					Resource.FlOW_CENTER.runInstanceNow(slave, "increment");
				}
			} finally {
				Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.INCREMENT.name(),STATUS.Blank,STATUS.Ready);  
				recompute = false;
			}
		} else {
			log.info(instance + " flow have been closed!startIncrement flow failed!");
		}
	}

	/**
	 * slave instance increment job
	 */
	public void runIncrement() {  
		if (Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.INCREMENT.name(),STATUS.Ready,STATUS.Running)) {
			String storeId;
			if (masterControl) {
				storeId = Resource.FLOW_INFOS.get(transDataFlow.getInstanceConfig().getPipeParams().getInstanceName(),
						GlobalParam.FLOWINFO.MASTER.name()).get(GlobalParam.FLOWINFO.INCRE_STOREID.name());
				Common.setAndGetScanInfo(instance, L1seq, storeId);
			} else {
				storeId = Common.getStoreId(instance, L1seq, transDataFlow, true, recompute);
			}
 
			try {
				transDataFlow.run(instance, storeId, L1seq, false, masterControl); 
			} catch (FNException e) {
				if (!masterControl && e.getMessage().equals("storeId not found")) {
					storeId = Common.getStoreId(instance, L1seq, transDataFlow, true, true);
					try {
						transDataFlow.run(instance, storeId,L1seq, false, masterControl);
					} catch (FNException ex) {
						log.error(instance + " Increment Exception", ex);
					}
				}
				log.error(instance + " IncrementJob Exception", e);
			} finally {
				recompute = false;
				Common.setFlowStatus(instance,L1seq,GlobalParam.JOB_TYPE.INCREMENT.name(),STATUS.Blank,STATUS.Ready); 
			}
		} else {
			log.info(instance + " flow have been closed!Current Start Increment flow failed!");
		}
	}

	private static String getNextJobs(String[] nextJobs) {
		StringBuilder sf = new StringBuilder();
		for (String job : nextJobs) {
			InstanceConfig instanceConfig = Resource.nodeConfig.getInstanceConfigs().get(job);
			if (instanceConfig.openTrans()) {
				String[] _seqs = Common.getL1seqs(instanceConfig, true);
				for (String seq : _seqs) {
					if (seq == null)
						continue;
					sf.append(Common.getMainName(job, seq) + " ");
				}
			}
		}
		return sf.toString();
	} 
}
