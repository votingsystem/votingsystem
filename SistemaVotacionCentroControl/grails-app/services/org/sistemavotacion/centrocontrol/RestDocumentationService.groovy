package org.sistemavotacion.centrocontrol

import java.util.List;
import java.util.Map;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager
import org.codehaus.groovy.tools.groovydoc.FileOutputTool
import org.codehaus.groovy.tools.groovydoc.OutputTool
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;

import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.sistemavotacion.centrocontrol.modelo.Respuesta
import org.sistemavotacion.docs.CommentDoc
import org.sistemavotacion.docs.ControllerDoc
import org.sistemavotacion.util.*;
import groovy.text.Template;
import groovy.text.TemplateEngine;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class RestDocumentationService {
	
	public static final List classTokens = ["@infoController", "@descController", "@author"]
	public static final List methodTokens = ["@httpMethod", "@param", "@return"]

	private File controllerDocTemplateFile = new File("." + File.separator + "grails-app" +
		File.separator + "views" + File.separator + "templates" + File.separator + 
		"restControllerDoc.html")
	
	def grailsApplication
	File controllersBaseDir = new File("." + File.separator + "grails-app" + 
		File.separator + "controllers")
	File controllerDocViewBaseDir = new File("." + File.separator + "grails-app" +
		File.separator + "views")
	
	public Respuesta generateDocs() {
		try {
			grailsApplication.controllerClasses?.each { controller ->//DefaultGrailsControllerClass
				ControllerDoc controllerDoc = new ControllerDoc(controller,
					controllerDocViewBaseDir.absolutePath, grailsApplication.config.grails.serverURL);
				//if(!"app".equals(controllerDoc.getLogicalPropertyName())) return
				
				log.debug("controllerDoc: ${controllerDoc.controllerDocFilePath}")
				controller.uris?.each { actionURI ->
					if(actionURI.indexOf("/**") == -1) {
						String actionMethodName= controller.getMethodActionName(actionURI)
						controllerDoc.addControllerAction(actionMethodName, actionURI)
						log.debug("actionMethodName: ${actionMethodName} - actionURI: ${actionURI}")
					}
				}
				log.debug("controllerActions.size(): ${controllerDoc.getControllerActions()?.size()}")
				parseController(controllerDoc);
				if(!controllerDoc.isPluginController) controllerDoc.generateDoc(controllerDocTemplateFile);
			}
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION, mensaje:ex.getMessage())
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}

	private void parseController(ControllerDoc controllerDoc) {
		String controllerPath = controllersBaseDir.absolutePath  + File.separator + 
			controllerDoc.getReferenceInstance().class.getName().replace(".", "/") + ".groovy";
		log.debug(" --- parseController - controllerPath: ${controllerPath}")
		File controlleFile = new File(controllerPath)
		if(!controlleFile.exists()) {
			log.debug("Plugin controller")
			controllerDoc.isPluginController = true;
			return
		}
		def idx = 0
		int beginCommentIndex;
		int endCommentIndex;
		boolean isCommentLine = false
		String comment = ""
		controlleFile.eachLine{ line ->
			line = line.replace("/**/", "")
		    idx++
			beginCommentIndex = line.indexOf("/**")
			endCommentIndex = line.indexOf("*/")
			if(isCommentLine && endCommentIndex == -1) {
				comment = (comment + line.trim()).replace('*', '').replace("\n", "<br/>").trim();
				return;
			}
			if(isCommentLine && endCommentIndex >= 0) {
				comment = comment + line.substring(0, endCommentIndex).trim();
				line = line.substring(endCommentIndex)
				isCommentLine = false
			}
			if(!isCommentLine && beginCommentIndex >= 0) {
				if(endCommentIndex >= 0) {
					comment = line.substring(beginCommentIndex, endCommentIndex).trim();
					line.replace(comment, "");
					log.debug">>>>>> line ${line} - comment:${comment}"
				} else {
					isCommentLine = true;
					comment = line.substring(beginCommentIndex).replace("/*", "").trim();
					line = line.substring(0, beginCommentIndex)
				}
			}
			comment = comment.replace('*', '').replace("\n", "<br/>").trim()
			if(!line || line.length() == 0) return
			if(line.contains("class ${controllerDoc.getNaturalName()}")) {
				parseControllerComment(controllerDoc, comment)
				comment = "";
			}
			controllerDoc.controllerActions.each{controllerActionDoc ->
		        //TODO
				if(line.contains("def ${controllerActionDoc.method}")
					&& line.contains("{")){
					int methodDefIdx = line.indexOf("def ${controllerActionDoc.method}") 
					int nextCharIdx = methodDefIdx + "def ${controllerActionDoc.method}".length()
					Character nextChar = line.substring(nextCharIdx, nextCharIdx + 1)
					if(Character.isLetter(nextChar)) {
						log.debug("---- buscando 'def ${controllerActionDoc.method}' Saltando liena ${line}")
						return
					}
					
					log.debug("method: def ${controllerActionDoc.method} - lineNumber: ${idx} - comment:${comment}")
					controllerActionDoc.lineNumber = idx;
					controllerActionDoc.commentDoc = parseActionComment(comment)
					comment = "";
		        }
		    }
		}
	}
	
	private void parseControllerComment(
		ControllerDoc controllerDoc, String comment) {
		log.debug(" --- parseControllerComment: ${comment}")
		if(!comment || "".equals(comment)) return;
		String nextToken = getNextToken(comment, classTokens)
		while(nextToken) {
			int tokenIdx = comment.indexOf(nextToken)
			String lastToken = nextToken
			nextToken = getNextToken(comment.substring(tokenIdx + lastToken.length()), classTokens);
			//log.debug(" --- lastToken:${lastToken} - nextToken: ${nextToken} - comment:'${comment}'")
			int nexTokenIdx
			if("@infoController".equals(lastToken)) {
				if(!nextToken) {
					controllerDoc.infoController =
						comment.substring(tokenIdx + "@infoController".length())
				} else {
					nexTokenIdx = comment.indexOf(nextToken)
					controllerDoc.infoController =	comment.substring(
						tokenIdx + "@infoController".length(), nexTokenIdx)
					comment = comment.substring(nexTokenIdx)
				}
			} else if("@descController".equals(lastToken)) {
				if(!nextToken) {
					controllerDoc.descController =
						comment.substring(tokenIdx + "@descController".length())
				} else {
					nexTokenIdx = comment.indexOf(nextToken)
					controllerDoc.descController =	comment.substring(
						tokenIdx + "@descController".length(), nexTokenIdx)
					comment = comment.substring(nexTokenIdx)
				}
			} else {
				if(nextToken) {
					nexTokenIdx = comment.indexOf(nextToken)
					comment = comment.substring(nexTokenIdx)
				}
			}
		}
	}
	
	private CommentDoc parseActionComment(String comment) {
		log.debug(" --- parseActionComment: ${comment}")
		CommentDoc commentDoc = new CommentDoc()
		if(!comment || "".equals(comment)) return commentDoc;
		String nextToken = getNextToken(comment, methodTokens)
		int tokenIdx
		if(!nextToken) {
			commentDoc.description = comment;
			return commentDoc
		} else {
			tokenIdx = comment.indexOf(nextToken)
			commentDoc.description = comment.substring(0, tokenIdx)
			comment = comment.substring(tokenIdx + nextToken.length())
		}
		String tokenText = null;
		while(nextToken) {
			String lastToken = nextToken
			nextToken = getNextToken(comment, methodTokens)
			log.debug(" *** lastToken: '${lastToken}' - nextToken: ${nextToken} - comment: '${comment}'")
			if(!nextToken) {
				tokenText = comment.trim()
				comment = ""
			} else {
				tokenIdx = comment.indexOf(nextToken)
				tokenText = comment.substring(0, tokenIdx).trim()
				comment = comment.substring(tokenIdx + nextToken?.length())
			}
			if("@param".equals(lastToken)) {
				String[] tokenTextSplitted = tokenText.split("\\s+");//this groups all whitespaces as a delimiter.
				if(tokenTextSplitted.size() > 0) {
					commentDoc.addParam(tokenTextSplitted[0], tokenText.substring(tokenTextSplitted[0].length()) )
				} else commentDoc.addParam(tokenTextSplitted[0],"")
				
			} else if("@httpMethod".equals(lastToken)) {
				commentDoc.httpMethod = tokenText.trim()
			} else if("@return".equals(lastToken)) {
				commentDoc.result = tokenText.trim()
			}
		}
		return commentDoc;
	}
	
	private String getNextToken(String comment, List tokenList) {
		Integer lowestIdx
		String token = null
		tokenList.each {
			int idx = comment.indexOf(it)
			if(idx >= 0) {
				if(lowestIdx == null) {
					lowestIdx = idx
					token = it
				} else {
					if(idx < lowestIdx) {
						lowestIdx = idx;
						token = it
					}
				}
			}
		}
		return token
	}

}

