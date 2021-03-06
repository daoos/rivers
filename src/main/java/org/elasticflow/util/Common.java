package org.elasticflow.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.mortbay.jetty.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.elasticflow.config.GlobalParam;
import org.elasticflow.config.GlobalParam.JOB_TYPE;
import org.elasticflow.config.GlobalParam.KEY_PARAM;
import org.elasticflow.config.GlobalParam.STATUS;
import org.elasticflow.config.InstanceConfig;
import org.elasticflow.field.RiverField;
import org.elasticflow.model.InstructionTree;
import org.elasticflow.model.RiverRequest;
import org.elasticflow.model.reader.ScanPosition;
import org.elasticflow.node.CPU;
import org.elasticflow.param.warehouse.WarehouseParam;
import org.elasticflow.piper.PipePump;
import org.elasticflow.yarn.Resource; 

/**
 * 
 * @author chengwen
 * @version 1.2
 * @date 2018-10-11 11:00
 */
public final class Common {
	
	public static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public final static Logger LOG = LoggerFactory.getLogger("Elasticflow");

	private static Set<String> defaultParamSet = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(KEY_PARAM.start.toString());
			add(KEY_PARAM.count.toString());
			add(KEY_PARAM.fl.toString());
			add(KEY_PARAM.facet.toString());
			add(KEY_PARAM.sort.toString());
			add(KEY_PARAM.group.toString());
			add(KEY_PARAM.facet_count.toString());
			add(KEY_PARAM.detail.toString());
		}
	};

	public static boolean isDefaultParam(String p) {
		if (defaultParamSet.contains(p))
			return true;
		else
			return false;
	}
	
	public static void getXmlParam(Object Obj,Node param, Class<?> c) throws Exception {
		Element element = (Element) param; 
		setConfigObj(Obj,c,element.getElementsByTagName("name").item(0).getTextContent(),element.getElementsByTagName("value").item(0).getTextContent()); 
	}
	
	public static Object getXmlObj(Node param, Class<?> c) throws Exception {
		Element element = (Element) param; 
		Constructor<?> cons = c.getConstructor();
		Object o = cons.newInstance(); 
		Field[] fields = c.getDeclaredFields(); 
		for (int f = 0; f < fields.length; f++) {
			Field field = fields[f];
			String value = null;
			String fieldName = field.getName();
			NodeList list = element.getElementsByTagName(fieldName);

			if (list != null && list.getLength() > 0) {
				Node node = list.item(0);
				value = node.getTextContent();
			} else {
				value = element.getAttribute(fieldName);
			}

			if (param.getNodeName().equals(fieldName)) {
				value = param.getTextContent();
			}
			setConfigObj(o,c,fieldName,value); 
		}
		return o;
	}
	
	public static void setConfigObj(Object Obj,Class<?> c,String fieldName,String value) throws Exception {
		if (value != null && value.length() > 0) {
			String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase()
					+ fieldName.substring(1);
			Method setMethod = c.getMethod(setMethodName, new Class[] { String.class });
			setMethod.invoke(Obj, new Object[] { value });
		}
	}

	public static List<String> getKeywords(String queryStr) {
		List<String> ret = new ArrayList<String>(); 
		try {
			TokenStream tokenStream = IK.participle(queryStr);
			tokenStream.addAttribute(CharTermAttribute.class); 
			tokenStream.reset();
			while (tokenStream.incrementToken()) {
				String text = tokenStream.getAttribute(CharTermAttribute.class).toString();
				ret.add(text);
			}
			tokenStream.end();
			tokenStream.close(); 
		} catch (Exception e) {
			ret.add(queryStr);
		}
		return ret;
	}
 
	public static long getNow() {
		return System.currentTimeMillis() / 1000;
	}

	public static String seconds2time(long second) {
		long h = 0;
		long m = 0;
		long s = 0;
		long temp = second % 3600;
		if (second > 3600) {
			h = second / 3600;
			if (temp != 0) {
				if (temp > 60) {
					m = temp / 60;
					if (temp % 60 != 0) {
						s = temp % 60;
					}
				} else {
					s = temp;
				}
			}
		} else {
			m = second / 60;
			if (second % 60 != 0) {
				s = second % 60;
			}
		}

		String ret = "";
		if (h >= 10)
			ret += h + "h";
		else if (h > 0)
			ret += "0" + h + "h";

		if (m >= 10)
			ret += m + "m";
		else if (m > 0)
			ret += "0" + m + "m";

		if (s >= 10)
			ret += s + "s";
		else if (s >= 0)
			ret += "0" + s + "s";

		return ret;
	}

	public static List<String> stringToList(String str, String seperator) { 
		return Arrays.asList(str.split(seperator));
	}
	
	public static String arrayToString(String[] strs, String seperator) {
		StringBuilder sf = new StringBuilder();
		if (strs.length > 0) { 
			for (String s : strs) {
				sf.append(",");
				sf.append(s);
			}
			return sf.substring(1);
		} 
		return sf.toString();
	}
 

	/**
	 * seq for split data
	 * 
	 * @param indexname
	 * @param L1seq,for
	 *            series data source fetch
	 * @return
	 */
	public static String getTaskStorePath(String instanceName, String L1seq,String location) {
		return GlobalParam.INSTANCE_PATH + "/" + instanceName + "/" + ((L1seq != null && L1seq.length() > 0) ? L1seq + "/" : "")
				+location;
	}

	/**
	 * 
	 * @param instance
	 * @param L1seq
	 * @param storeId
	 * @param location
	 */
	public static void saveTaskInfo(String instance, String L1seq,String storeId,String location) {
		String instanceName = getMainName(instance, L1seq);
		GlobalParam.SCAN_POSITION.get(instanceName).updateStoreId(storeId);
		ZKUtil.setData(getTaskStorePath(instanceName, L1seq,location),
				GlobalParam.SCAN_POSITION.get(instanceName).getString());
	} 

	/**
	 * @param instanceName
	 *            data source main tag name
	 * @param storeId a/b or time mechanism tags
	 * @return String
	 */
	public static String getStoreName(String instanceName, String storeId) {
		if (storeId != null && storeId.length() > 0) {
			return instanceName + "_" + storeId;
		} else {
			return instanceName;
		}

	}

	/** 
	 * @param L1seq
	 *            for data source sequence tag 
	 * @param instance
	 *            data source main tag name 
	 * @return String
	 */
	public static String getMainName(String instance, String L1seq) {
		if (L1seq != null && L1seq.length()>0) {
			return instance + L1seq;
		} else {
			return instance;
		} 
	}
	
	public static String getResourceTag(String instance,String L1seq,String tag,boolean ignoreSeq) {
		StringBuilder tags = new StringBuilder();
		if (!ignoreSeq && L1seq != null && L1seq.length()>0) {
			tags.append(instance).append(L1seq);
		} else {
			tags.append(instance).append(GlobalParam.DEFAULT_RESOURCE_SEQ);
		} 
		return tags.append(tag).toString();
	}
	
	public static String getFullStartInfo(String instance, String L1seq) {
		String info = "0";
		String path = Common.getTaskStorePath(instance, L1seq,GlobalParam.JOB_FULLINFO_PATH);
		byte[] b = ZKUtil.getData(path,true); 
		if (b != null && b.length > 0) {
			String str = new String(b); 
			if (str.length() > 1) {
				info = str;
			}
		}
		return info;
	} 
	
	/**
	 * for Master/slave job get and set LastUpdateTime
	 * @param instance
	 * @param L1seq
	 * @param storeId  Master store id
	 */
	public static void setAndGetScanInfo(String instance, String L1seq,String storeId) {
		String mainName = getMainName(instance, L1seq);
		synchronized (GlobalParam.SCAN_POSITION) {
			if(!GlobalParam.SCAN_POSITION.containsKey(mainName)) {
				String path = Common.getTaskStorePath(mainName, L1seq,GlobalParam.JOB_INCREMENTINFO_PATH);
				byte[] b = ZKUtil.getData(path,true);
				if (b != null && b.length > 0) {
					String str = new String(b); 
					GlobalParam.SCAN_POSITION.put(mainName, new ScanPosition(str,instance,storeId));  
				}else {
					GlobalParam.SCAN_POSITION.put(mainName, new ScanPosition(instance,storeId));
				}
				saveTaskInfo(instance, L1seq,storeId,GlobalParam.JOB_INCREMENTINFO_PATH);
			}
		}
		
	}

	/**
	 * get increment store tag name and will auto create new one with some conditions.
	 * 
	 * @param isIncrement
	 * @param reCompute
	 *            force to get storeid recompute from destination engine
	 * @param L1seq
	 *            for series data source sequence
	 * @param instance
	 *            data source main tag name
	 * @return String
	 */
	public static String getStoreId(String instance, String L1seq, PipePump transDataFlow, boolean isIncrement,
			boolean reCompute) {
		if (isIncrement) { 
			return getIncrementStoreId(instance,L1seq,transDataFlow,reCompute);
		} else {
			return  (String) CPU.RUN(transDataFlow.getID(), "Pond", "getNewStoreId",true, getMainName(instance, L1seq), false);
		}
	} 
	
	private static synchronized String getIncrementStoreId(String instance, String L1seq, PipePump transDataFlow,boolean reCompute) {
		String storeId = getStoreId(instance,L1seq,true); 
		if (storeId.length() == 0 || reCompute) {
			storeId = (String) CPU.RUN(transDataFlow.getID(), "Pond", "getNewStoreId",false, getMainName(instance, L1seq), true); 
			if (storeId == null)
				storeId = "a";
			saveTaskInfo(instance,L1seq,storeId,GlobalParam.JOB_INCREMENTINFO_PATH);
		}
		return storeId;
	}
	/**
	 * get store tag name
	 * @param instanceName
	 * @param L1seq 
	 * @return String
	 */
	public static String getStoreId(String instance, String L1seq,boolean reload) { 
		String instanceName = getMainName(instance, L1seq);
		if(reload) {
			String path = Common.getTaskStorePath(instance, L1seq,GlobalParam.JOB_INCREMENTINFO_PATH);
			byte[] b = ZKUtil.getData(path, true);
			if (b != null && b.length > 0) {
				String str = new String(b);
				GlobalParam.SCAN_POSITION.put(instanceName, new ScanPosition(str,instance,L1seq));  
			}else {
				GlobalParam.SCAN_POSITION.put(instanceName, new ScanPosition(instance,L1seq));
			}
		}  
		return GlobalParam.SCAN_POSITION.get(instanceName).getStoreId();
	}
	
	/**
	 * get read data source seq flags
	 * @param instanceName
	 * @param fillDefault if empty fill with system default blank seq
	 * @return
	 */
	public static String[] getL1seqs(InstanceConfig instanceConfig,boolean fillDefault){
		String[] seqs = {};
		WarehouseParam whParam;
		if(Resource.nodeConfig.getNoSqlWarehouse().get(instanceConfig.getPipeParams().getReadFrom())!=null){
			whParam = Resource.nodeConfig.getNoSqlWarehouse().get(
					instanceConfig.getPipeParams().getReadFrom());
		}else{
			whParam = Resource.nodeConfig.getSqlWarehouse().get(
					instanceConfig.getPipeParams().getReadFrom());
		}
		if (null != whParam) {
			seqs = whParam.getL1seq();
		}  
		if (fillDefault && seqs.length == 0) {
			seqs = new String[1];
			seqs[0] = GlobalParam.DEFAULT_RESOURCE_SEQ;
		} 
		return seqs;
	} 
	
	/**
	 * 
	 * @param heads
	 * @param instanceName
	 * @param storeId
	 * @param seq table seq
	 * @param total
	 * @param dataBoundary
	 * @param lastUpdateTime
	 * @param useTime
	 * @param types
	 * @param moreinfo
	 */
	
	public static String formatLog(String types,String heads,String instanceName, String storeId,
			String seq, int total, String dataBoundary, String lastUpdateTime,
			long useTime, String moreinfo) {
		String useTimeFormat = Common.seconds2time(useTime);
		StringBuilder str = new StringBuilder("["+heads+" "+instanceName + "_" + storeId+"] "+(!seq.equals("") ? " table:" + seq : ""));
		String update;
		if(lastUpdateTime.length()>9 && lastUpdateTime.matches("[0-9]+")){ 
			update = SDF.format(lastUpdateTime.length()<12?new Long(lastUpdateTime+"000"):new Long(lastUpdateTime));
		}else{
			update = lastUpdateTime;
		} 
		switch (types) {
		case "complete":
			str.append(" docs:" + total);
			str.append(" position:" + update);
			str.append(" useTime:" + useTimeFormat);
			break;
		case "start": 
			str.append(" position:" + update);
			break;
		default:
			str.append(" docs:" + total+ (dataBoundary.length()<1 ? "" : " dataBoundary:" + dataBoundary)
			+ " position:" + update + " useTime:"	+ useTimeFormat);
			break;
		} 
		return str.append(moreinfo).toString();
	}
 
	public static ArrayList<InstructionTree> compileCodes(String code,String contextId){
		ArrayList<InstructionTree> res = new ArrayList<>();
		for(String line:code.trim().split("\\n")) {  
			InstructionTree instructionTree=null; 
			InstructionTree.Node tmp=null;
			if(line.indexOf("//")>-1)
				line=line.substring(0, line.indexOf("//"));
			for(String str:line.trim().split("->")) {  
				if(instructionTree==null) {
					instructionTree = new InstructionTree(str,contextId);
					tmp = instructionTree.getRoot();
				}else { 
					String[] params = str.trim().split(",");
					for(int i=0;i<params.length;i++) {
						if(i==params.length-1) {
							tmp = instructionTree.addNode(params[i], tmp);
						}else {
							instructionTree.addNode(params[i], tmp);
						}
					}
					 
				} 
			} 
			res.add(instructionTree);
		}
		return res;
	} 
	
	/**
	 * 
	 * @param instance
	 * @param seq
	 * @param type tag for flow status,with job_type
	 * @param needState equal 0 no need check
	 * @param plusState
	 * @param removeState
	 * @return boolean,lock status
	 */
	public static boolean setFlowStatus(String instance,String L1seq,String type,STATUS needState, STATUS setState) {
		synchronized (Resource.FLOW_STATUS.get(instance, L1seq, type)) {
			if (needState.equals(STATUS.Blank) || (Resource.FLOW_STATUS.get(instance, L1seq, type).get() == needState.getVal())) {
				Resource.FLOW_STATUS.get(instance, L1seq, type).set(setState.getVal()); 
				return true;
			} else {
				LOG.info(instance + " " + type + " not in state "+needState.name()+"!");
				return false;
			}
		}
	}  
	
	public static boolean checkFlowStatus(String instance,String seq,JOB_TYPE type,STATUS state) {
		if((Resource.FLOW_STATUS.get(instance, seq, type.name()).get() & state.getVal())>0)
			return true; 
		return false;
	} 
	
	public static Object parseFieldValue(String v, RiverField fd) throws Exception {
		if (fd == null)
			return null; 
		if (v==null) {
			v = fd.getDefaultvalue();
		} 
		Class<?> c = Class.forName(fd.getParamtype());  
		Method method = c.getMethod("valueOf", String.class);
		return method.invoke(c,String.valueOf(v));
	}
	
	/**
	 * jetty request convert to RiverRequest
	 * @param input
	 * @return
	 */
	public static RiverRequest getRequest(Request input) {
		RiverRequest rr = RiverRequest.getInstance(); 
		Request rq = (Request) input;
		String path = rq.getPathInfo();
		String pipe = path.substring(1); 
		rr.setPipe(pipe);  
		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String,String>> iter = rq.getParameterMap().entrySet().iterator();
		while (iter.hasNext()) { 
			Map.Entry<String,String> entry = iter.next();
			String key = (String) entry.getKey();
			String value = rq.getParameter(key);
			rr.addParam(key, value);
		}
		return rr;
	}
}
