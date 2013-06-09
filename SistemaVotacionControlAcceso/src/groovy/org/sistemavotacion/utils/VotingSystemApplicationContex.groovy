package org.sistemavotacion.utils

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import javax.servlet.ServletContext

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@Singleton class VotingSystemApplicationContex 
		implements ApplicationContextAware {
  
	private ApplicationContext ctx

	@Override public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		ctx = applicationContext	
	}

	static ApplicationContext getApplicationContext() {
			getInstance().ctx
	}
	
	static Object getBean(String name) {
		getApplicationContext().getBean(name)
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