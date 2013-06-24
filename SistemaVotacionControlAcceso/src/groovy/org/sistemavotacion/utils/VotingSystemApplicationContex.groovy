package org.sistemavotacion.utils

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.sistemavotacion.smime.SignedMailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@Singleton class VotingSystemApplicationContex implements ApplicationContextAware{
	
	public enum Environment {DEVELOPMENT, PRODUCTION, TEST}
  
	private static Logger logger = LoggerFactory.
		getLogger(VotingSystemApplicationContex.class);
			

	private Environment environment
	private ApplicationContext ctx
	private static VotingSystemApplicationContex INSTANCE
	
	public Environment getEnvironment() {		
		if(environment == null) {
			Map grailsConfig = getBean("grailsApplication").config
			String environmentStr = grailsConfig.VotingSystemEnvironment?.toString().trim()
			if(environmentStr == null || "".equals(environmentStr.trim())) {
				return  Environment.valueOf(
					Environment.current.toString())
			} else environment = Environment.valueOf(environmentStr)	
		} 
		return environment;
	}

	
	@Override public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		ctx = applicationContext	
	}
	
	static ApplicationContext getApplicationContext() {
			getInstance().ctx
	}
	
	static Object getBean(String name) {
		ApplicationContext ctx = getApplicationContext()
		if(ctx != null) getApplicationContext().getBean(name)
		else return null
  	}
	
	static GrailsApplication getGrailsApplication() {
		getBean('grailsApplication')
	}
	
	static ConfigObject getConfig() {
		getGrailsApplication().config
	}
	
	static ServletContext getServletContext() {
		getBean('servletContext')
	}
	
	static GrailsPluginManager getPluginManager() {
		getBean('pluginManager')
	}
  
}