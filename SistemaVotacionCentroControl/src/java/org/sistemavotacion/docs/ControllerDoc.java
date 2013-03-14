package org.sistemavotacion.docs;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.WordUtils;
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass;
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager;
import org.codehaus.groovy.tools.groovydoc.FileOutputTool;


public class ControllerDoc {

	private DefaultGrailsControllerClass defaultGrailsControllerClass;
	
	Map<String, String> methodUriMap = new HashMap<String, String>();
	Map<String, ControllerActionDoc> methodControllerActionDocMap = 
			new HashMap<String, ControllerActionDoc>();
	private String controllerDocFilePath = null;
	private String infoController = null;
	private String descController = null;
	private String serverURL = null;
	private boolean isPluginController = false;
	
	private String templatePath = null;
	
	public ControllerDoc(DefaultGrailsControllerClass defaultGrailsControllerClass, 
			String controllerDocViewBaseDir, String serverURL) {
		this.defaultGrailsControllerClass = defaultGrailsControllerClass;
		this.controllerDocFilePath = controllerDocViewBaseDir + java.io.File.separator + 
				defaultGrailsControllerClass.getLogicalPropertyName() + java.io.File.separator +
				"index.gsp";
		this.serverURL = serverURL;
	}
	
	public ControllerDoc(DefaultGrailsControllerClass defaultGrailsControllerClass, 
			String controllerDocViewBaseDir, String serverURL, String templatePath) {
		this(defaultGrailsControllerClass, controllerDocViewBaseDir, serverURL);
		this.templatePath = templatePath;
	}
	
	public String getUncapitalizeName() {
		return  WordUtils.uncapitalize(defaultGrailsControllerClass.getName());
	}
	
	//mensajeSMIME
	public String getLogicalPropertyName() { 
		return defaultGrailsControllerClass.getLogicalPropertyName();
	}
	
	//MensajeSMIMEController
	public String getNaturalName() {
		return  defaultGrailsControllerClass.getNaturalName().replace(" ", "");
	}
	
	public Collection<ControllerActionDoc> getControllerActions() {
		return methodControllerActionDocMap.values();
	}

	public Object getReferenceInstance() {
		return defaultGrailsControllerClass.getReferenceInstance();
	}
	
	public void addControllerAction(String method, String uri) {
		if(methodUriMap.containsKey(method)) {
			String oldURI = methodUriMap.get(method);
			if(uri != null && uri.length() > oldURI.length()) {
				methodUriMap.remove(method);
				methodUriMap.put(method, uri);
				methodControllerActionDocMap.remove(method);
				ControllerActionDoc controllerActionDoc = 
						new ControllerActionDoc(method, uri);
				methodControllerActionDocMap.put(method, controllerActionDoc);
			}
		} else {
			methodUriMap.put(method, uri);
			ControllerActionDoc controllerActionDoc = 
					new ControllerActionDoc(method, uri);
			methodControllerActionDocMap.put(method, controllerActionDoc);
		}
		
	}

	public String getControllerDocFilePath() {
		return controllerDocFilePath;
	}

	public void setControllerDocFilePath(String controllerDocFilePath) {
		this.controllerDocFilePath = controllerDocFilePath;
	}

	public boolean isPluginController() {
		return isPluginController;
	}

	public void setPluginController(boolean isPluginController) {
		this.isPluginController = isPluginController;
	}
	
	public String generateDoc() throws Exception {
		if (templatePath == null) throw new Exception(" --- Template NULL --- ");
		TemplateEngine engine = new GStringTemplateEngine();
		ClasspathResourceManager resourceManager = new ClasspathResourceManager();
		String renderedSrc = null;
		Properties properties = new Properties();
		try {
			Template template = engine.createTemplate(resourceManager.getReader(templatePath));
			Map<String, Object> binding = new HashMap<String, Object>();
			binding.put("controllerDoc", this);
			binding.put("props", properties);
			renderedSrc = template.make(binding).toString();
			FileOutputTool output = new FileOutputTool();
			output.writeToOutput(controllerDocFilePath, renderedSrc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return renderedSrc;
	}
	
	public String generateDoc(File templateFile) {
		TemplateEngine engine = new GStringTemplateEngine();
		String renderedSrc = null;
		Properties properties = new Properties();
		try {
			Template template = engine.createTemplate(templateFile);
			Map<String, Object> binding = new HashMap<String, Object>();
			binding.put("controllerDoc", this);
			binding.put("props", properties);
			renderedSrc = template.make(binding).toString();
			FileOutputTool output = new FileOutputTool();
			output.writeToOutput(controllerDocFilePath, renderedSrc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return renderedSrc;
	}

	public String getInfoController() {
		if(infoController == null) return getLogicalPropertyName();
		return infoController;
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

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

}
