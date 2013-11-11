package org.votingsystem.groovy.util

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import grails.util.Metadata
@Singleton class VotingSystemApplicationContex implements ApplicationContextAware{
	
	public enum Environment {DEVELOPMENT, PRODUCTION, TEST}
  
	private static Logger log = LoggerFactory.
		getLogger(VotingSystemApplicationContex.class);

	private Environment environment
	private ApplicationContext ctx
	
	public Environment getEnvironment() {	
		if(environment == null) {
			Map grailsConfig = getBean("grailsApplication").config
			ConfigObject environment = grailsConfig.VotingSystemEnvironment
			if(environment == null || environment.isEmpty()) {
				return  Environment.valueOf(
					grails.util.Environment.current.name.toUpperCase())
			} else environment = Environment.valueOf(environment.toString().trim())	
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