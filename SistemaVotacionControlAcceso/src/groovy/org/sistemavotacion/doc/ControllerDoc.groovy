package org.sistemavotacion.doc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.WordUtils;

public class ControllerDoc {

	Map<String, ControllerActionDoc> methodControllerActionDocMap = 
			new HashMap<String, ControllerActionDoc>();

	private String infoController = null;
	private String descController = null;
	private String controllerClassName = null;
	private String logicalPropertyName = null;
	
			
	public ControllerDoc(String className){
		this.controllerClassName = className;
		if(className != null && className.indexOf("Controller") >=0) {
			logicalPropertyName = WordUtils.uncapitalize(
				className.substring(0, className.indexOf("Controller")));
		}
	}
	
	public Collection<ControllerActionDoc> getControllerActions() {
		return methodControllerActionDocMap.values();
	}
	
	public void addControllerAction(String method, 
		ControllerActionDoc controllerActionDoc) {
		methodControllerActionDocMap.put(method, controllerActionDoc);
	}

	public String getInfoController() {
		if(infoController == null) return logicalPropertyName;
		return infoController;
	}
	
	public String getLogicalPropertyName() {
		return logicalPropertyName;
	}

	public void setInfoController(String infoController) {
		this.infoController = infoController;
	}

	public String getDescController() {
		return descController;
	}

	public void setDescController(String descController) {
		this.descController = descController;
	}
	
}