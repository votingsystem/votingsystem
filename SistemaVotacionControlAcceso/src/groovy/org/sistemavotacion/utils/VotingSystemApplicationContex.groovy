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
  
	private static Logger logger = LoggerFactory.
		getLogger(VotingSystemApplicationContex.class);
			

	private ApplicationContext ctx
	
		
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