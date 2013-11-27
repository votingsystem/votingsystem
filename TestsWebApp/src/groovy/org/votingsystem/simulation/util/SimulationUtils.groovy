package org.votingsystem.simulation.util

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS

class SimulationUtils {
	
	public static String getFormattedErrorList(List<String> errorList) {
		if(errorList.isEmpty()) return null;
		else {
			StringBuilder result = new StringBuilder("");
			for(String error:errorList) {
				result.append(error + "\n");
			}
			return result.toString();
		}
	}

	public static String getUserDirPath (String userNIF) {
		int subPathLength = 3;
		String basePath = "/";
		while (userNIF.length() > 0) {
			if(userNIF.length() <= subPathLength) subPathLength = userNIF.length();
			String subPath = userNIF.substring(0, subPathLength);
			userNIF = userNIF.substring(subPathLength);
			basePath = basePath + subPath + File.separator;
		}
		if(!basePath.endsWith("/")) basePath = basePath + "/";
		return basePath;
	}
}
