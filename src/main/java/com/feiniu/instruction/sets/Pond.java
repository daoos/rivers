package com.feiniu.instruction.sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feiniu.config.GlobalParam;
import com.feiniu.config.GlobalParam.Mechanism;
import com.feiniu.config.GlobalParam.STATUS;
import com.feiniu.instruction.Context;
import com.feiniu.instruction.Instruction;
import com.feiniu.util.Common;

/**
 * 
 * @author chengwen
 * @version 1.0
 * @date 2018-10-26 09:25
 */
public class Pond extends Instruction {

	private final static Logger log = LoggerFactory.getLogger("Pond");

	/**
	 * @param args
	 *            parameter order is: String mainName,String storeId
	 */
	public static boolean createStorePosition(Context context, Object[] args) {
		if (!isValid(2, args)) {
			log.error("Pond createStorePosition parameter not match!");
			return false;
		} 
		context.getWriter().PREPARE(false, false);
		boolean state = false;
		if (context.getWriter().ISLINK()) {
			try { 
				String mainName = String.valueOf(args[0]); 
				String storeId = String.valueOf(args[1]);  
				if(context.getInstanceConfig().getPipeParams().getWriteMechanism()==Mechanism.AB) {
					state = context.getWriter().create(mainName, storeId, context.getInstanceConfig().getWriteFields());    
				}else {
					context.getWriter().removeInstance(mainName, storeId);
					state = context.getWriter().create(mainName, storeId, context.getInstanceConfig().getWriteFields());    
				} 
			}finally {
				context.getWriter().REALEASE(false,state?false:true);
			}
		}
		return state;
	}

	/**
	 * @param args
	 *            parameter order is: String storeId,String keyColumn,String keyVal
	 */
	public static void deleteByKey(Context context, Object[] args) {
		boolean freeConn = false;
		if (!isValid(3, args)) {
			log.error("deleteByKey parameter not match!");
			return;
		}
		context.getWriter().PREPARE(false, false);
		if (context.getWriter().ISLINK()) {
			try {  
				String storeId = String.valueOf(args[0]);
				String keyColumn = String.valueOf(args[1]); 
				String keyVal = String.valueOf(args[2]);
				context.getWriter().delete(context.getInstanceConfig().getName(),storeId,keyColumn,keyVal);
			} catch (Exception e) {
				log.error("DeleteByQuery Exception", e);
				freeConn = true;
			} finally {
				context.getWriter().REALEASE(false,freeConn);
			}
		}
	}

	/**
	 * @param args
	 *            parameter order is: String mainName, String storeId
	 */
	public static void optimizeInstance(Context context, Object[] args) {
		if (!isValid(2, args)) {
			log.error("optimizeInstance parameter not match!");
			return;
		}
		context.getWriter().PREPARE(false, false);
		if (context.getWriter().ISLINK()) {
			try {
				String mainName = String.valueOf(args[0]); 
				String storeId = String.valueOf(args[1]);
				context.getWriter().optimize(mainName, storeId);
			} finally {
				context.getWriter().REALEASE(false,false);
			}
		}
	}
	
	/**
	 * @param args 
	 *            parameter order is: String instance, String seq, String storeId
	 */
	public static boolean switchInstance(Context context, Object[] args) {
		if (!isValid(3, args)) {
			log.error("switchInstance parameter not match!");
			return false;
		}
		String removeId = ""; 
		String mainName,storeId; 
		mainName = Common.getInstanceName(String.valueOf(args[0]),String.valueOf(args[1]));
		storeId = String.valueOf(args[2]); 
		int waittime=0; 
		if(Common.checkFlowStatus(mainName,"",GlobalParam.JOB_TYPE.INCREMENT,STATUS.Running)) {
			Common.setFlowStatus(mainName,"",GlobalParam.JOB_TYPE.INCREMENT.name(), STATUS.Blank, STATUS.Termination);
			while (!Common.checkFlowStatus(mainName,"",GlobalParam.JOB_TYPE.INCREMENT,STATUS.Ready)) {
				try {
					waittime++;
					Thread.sleep(2000);
					if (waittime > 30) {
						break;
					}
				} catch (InterruptedException e) {
					log.error("currentThreadState InterruptedException", e);
				}
			}  
		} 
		Common.setFlowStatus(mainName,"",GlobalParam.JOB_TYPE.INCREMENT.name(), STATUS.Blank, STATUS.Termination); 
		context.getWriter().PREPARE(false, false);  
		if (context.getWriter().ISLINK()) {
			try {
				if(context.getInstanceConfig().getPipeParams().getWriteMechanism()==Mechanism.AB) {
					if (storeId.equals("a")) {
						context.getWriter().optimize(mainName, "a");
						removeId = "b";
					} else {
						context.getWriter().optimize(mainName, "b");
						removeId = "a";
					}
					context.getWriter().removeInstance(mainName, removeId);
				} 
				context.getWriter().setAlias(mainName, storeId, context.getInstanceConfig().getAlias());
				return true;
			} catch (Exception e) {
				log.error("switchInstance Exception", e);
			} finally { 
				Common.saveTaskInfo(String.valueOf(args[0]), String.valueOf(args[1]), storeId, GlobalParam.JOB_INCREMENTINFO_PATH);
				Common.setFlowStatus(mainName,"",GlobalParam.JOB_TYPE.INCREMENT.name(),STATUS.Blank,STATUS.Ready);
				context.getWriter().REALEASE(false,false); 
			}
		}
		return false;
	}
 
	/**
	 * @param args
	 *            parameter order is: String mainName, boolean isIncrement 
	 */
	public static String getNewStoreId(Context context, Object[] args) {
		String taskId = null;
		if (!isValid(2, args)) {
			log.error("getNewStoreId parameter not match!");
			return null;
		}
		String mainName = String.valueOf(args[0]);
		boolean isIncrement = (boolean) args[1]; 
		context.getWriter().PREPARE(false, false);
		boolean release = false;
		if (context.getWriter().ISLINK()) {
			try {
				taskId = context.getWriter().getNewStoreId(mainName, isIncrement, context.getInstanceConfig());
			} catch (Exception e) {
				release = true;
			}finally {
				context.getWriter().REALEASE(false,release);
			}
		}
		return taskId;
	}
}
