package com.feiniu.instruction;

import java.util.List;

import com.feiniu.config.GlobalParam;
import com.feiniu.instruction.flow.TransDataFlow;
import com.feiniu.util.Common;
import com.feiniu.util.ZKUtil;

public class TaskControl extends Instruction{ 
	
	public static void moveFullPosition(Context context, Object[] args) {
		if (!isValid(3, args)) {
			Common.LOG.error("moveFullPosition parameter not match!");
			return ;
		} 
		int start = Integer.parseInt(args[0].toString());
		int days = Integer.parseInt(args[1].toString());
		int ride = Integer.parseInt(args[2].toString());
		String[] seqs = Common.getSeqs(context.getInstanceConfig(),true);  
		for(String seq:seqs) {
			String info = Common.getFullStartInfo(context.getInstanceConfig().getName(), seq);
			String saveInfo="";
			if(info!=null && info.length()>5) {
				for(String tm:info.split(",")) {
					if(Integer.parseInt(tm)<start) {
						saveInfo += String.valueOf(start+days*3600*24*ride)+",";
					}else {
						saveInfo += String.valueOf(Integer.parseInt(tm)+days*3600*24*ride)+",";
					} 
				}
			}else {
				saveInfo = String.valueOf(start + days*3600*24*ride);
			}
			ZKUtil.setData(Common.getTaskStorePath(context.getInstanceConfig().getName(), seq,GlobalParam.JOB_FULLINFO_PATH),saveInfo);
		} 
	}
	
	public static void setIncrementPosition(Context context, Object[] args) {
		if (!isValid(1, args)) {
			Common.LOG.error("moveFullPosition parameter not match!");
			return ;
		} 
		
		int position = Integer.parseInt(args[0].toString());
		String[] seqs = Common.getSeqs(context.getInstanceConfig(),true);  
		for(String seq:seqs) { 
			String saveInfo=""; 
			List<String> table_seq = context.getInstanceConfig().getPipeParam().getSqlParam().getSeq();
			TransDataFlow transDataFlow = GlobalParam.SOCKET_CENTER.getTransDataFlow(context.getInstanceConfig().getName(), seq, false,GlobalParam.DEFAULT_RESOURCE_TAG);
			String storeId = Common.getStoreId(context.getInstanceConfig().getName(), seq, transDataFlow, true, false);
			if(storeId==null)
				break;
			if(table_seq.size()>0) {
				for(int i=0;i<table_seq.size();i++) {
					saveInfo += String.valueOf(position)+",";
				}
			}else {
				saveInfo = String.valueOf(position);
			}
			
			GlobalParam.LAST_UPDATE_TIME.set(context.getInstanceConfig().getName(),seq, saveInfo);
			Common.saveTaskInfo(context.getInstanceConfig().getName(), seq, storeId,GlobalParam.JOB_INCREMENTINFO_PATH);
		}
	}
}