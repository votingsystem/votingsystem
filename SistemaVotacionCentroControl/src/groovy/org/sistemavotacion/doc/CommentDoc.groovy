package org.sistemavotacion.doc;

import java.util.HashMap;
import java.util.Map;

public class CommentDoc {
	
	private Map<String, String> paramsMap = new HashMap<String, String>();
	private String description;
	private String result;
	private String httpMethod;
	
	public CommentDoc () {
		
	}

	public Map<String, String> getParamsMap() {
		return paramsMap;
	}
	
	public void addParam(String paramName, String paramDescription) {
		paramsMap.put(paramName, paramDescription);
	}

	public void setParamsMap(Map<String, String> paramsMap) {
		this.paramsMap = paramsMap;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

}
