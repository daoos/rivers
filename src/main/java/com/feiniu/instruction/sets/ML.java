package com.feiniu.instruction.sets;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feiniu.instruction.Context;
import com.feiniu.instruction.Instruction;
import com.feiniu.model.computer.SampleSets;
import com.feiniu.model.reader.DataPage;
import com.feiniu.node.CPU;
import com.feiniu.reader.util.DataSetReader;
import com.feiniu.util.Common;

public class ML extends Instruction {
	
	private final static Logger log = LoggerFactory.getLogger("ML");
	
	public static DataPage train(Context context, Object[] args) {
		if (!isValid(3, args)) {
			log.error("train parameter not match!");
			return null;
		}
		try {
			Class<?> clz = Class.forName("com.feiniu.ml.algorithm."+String.valueOf(args[0])); 
			Method m = clz.getMethod("train", Context.class,SampleSets.class,Map.class);   
			return (DataPage) m.invoke(null,context,args[1],args[2]);
		}catch (Exception e) {
			log.error("train Exception",e);
		} 
		return null; 
	}

	/**
	 * @param args
	 *            parameter order is:String contextId, String types, String instance, DataPage pageData
	 * @throws Exception
	 */
	public static DataPage computeDataSet(Context context, Object[] args) {
		DataPage res = new DataPage();
		if (!isValid(4, args)) {
			log.error("computeDataSet parameter not match!");
			return res;
		}
		String contextId = String.valueOf(args[0]);
		String types = String.valueOf(args[1]);
		String instance = String.valueOf(args[2]);
		DataPage dp = (DataPage) args[3];
		
		if (dp.size() == 0)
			return res; 
		
		DataSetReader DSReader = new DataSetReader();
		DSReader.init(dp);
		long start = Common.getNow();
		int num = 0;
		if (DSReader.status()) {
			try {
				SampleSets samples = SampleSets.getInstance(dp.getData().size());
				while (DSReader.nextLine()) {
					samples.addPoint(DSReader.getLineData(), context.getInstanceConfig().getComputeParams());
					num++;
				}
				res = (DataPage) CPU.RUN(contextId, "ML", "train", false,
						context.getInstanceConfig().getComputeParams().getAlgorithm(), samples,
						context.getInstanceConfig().getWriteFields());
				log.info(Common.formatLog("onepage"," -- " + types + " compute onepage ", instance,
						context.getInstanceConfig().getComputeParams().getAlgorithm(), "", num,
						DSReader.getDataBoundary(), DSReader.getScanStamp(), Common.getNow() - start,  ""));
			} catch (Exception e) {
				log.error("computeDataSet Exception", e);
			} finally {
				DSReader.close();
			}
		}
		return res;
	} 
}