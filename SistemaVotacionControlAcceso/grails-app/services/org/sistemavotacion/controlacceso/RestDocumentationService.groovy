package org.sistemavotacion.controlacceso

import java.util.List;
import java.util.Map;

import java.util.HashMap;

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.tools.groovydoc.FileOutputTool
import org.codehaus.groovy.tools.groovydoc.GroovyDocTool
import org.codehaus.groovy.tools.groovydoc.OutputTool
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyMethodDoc
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyRootDoc

import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.sistemavotacion.controlacceso.modelo.Respuesta
import org.sistemavotacion.doc.*
import org.sistemavotacion.util.*;

import groovy.io.FileType
import grails.util.Environment
import org.codehaus.groovy.control.CompilePhase

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class RestDocumentationService {
	
	public static final List classTokens = ["@infoController", "@descController", "@author"]
	public static final List methodTokens = ["@httpMethod", "@param", "@return"]

	def grailsApplication
	grails.gsp.PageRenderer groovyPageRenderer
	
	File controllersBaseDir = new File("." + File.separator + "grails-app" + 
		File.separator + "controllers")
	File controllerDocViewBaseDir = new File("." + File.separator + "grails-app" +
		File.separator + "views")
	
	
	public Respuesta generateDocs() {
		log.debug(" --- generateDocs")
		Map<String, Set> controllerToActionsMap  = new HashMap<String, Set>()
		grailsApplication.controllerClasses?.each { controller ->//DefaultGrailsControllerClass
			Set<String> methodSet = new HashSet<String>()
			controller.uris?.each { actionURI ->
				if(actionURI.indexOf("/**") == -1) {
					String actionMethodName= controller.getMethodActionName(actionURI)
					methodSet.add(actionMethodName)
				}
			}
			log.debug("controller: ${controller.getLogicalPropertyName()} - Actions number: ${methodSet?.size()}")
			controllerToActionsMap.put(controller.getLogicalPropertyName(), methodSet)
		}
		List fileNames = new ArrayList()
		GroovyDocTool docTool = new GroovyDocTool("")
		SimpleGroovyRootDoc rootDoc = docTool.getRootDoc()
		controllersBaseDir.eachFileRecurse (FileType.FILES) { file ->
			println("--- Controller file.path: ${file.path}")
			fileNames << file.path
		}
		/*File file = new File("/home/jgzornoza/github/SistemaVotacion/SistemaVotacionControlAcceso/grails-app/controllers/org/sistemavotacion/controlacceso/InfoServidorController.groovy")
		if(!file.exists()) {
			log.debug("El archivo no existe")
			
		}
		fileNames << file.path*/
		docTool.add(fileNames)
		rootDoc.classes().each {//SimpleGroovyClassDoc
			ControllerDoc controllerDoc = new ControllerDoc(it.name());
			if(it.methods()?.length == 0) return; 
			Set<String> actionSet = controllerToActionsMap.get(controllerDoc.logicalPropertyName)
			String controllerBaseURI = "/${controllerDoc.logicalPropertyName}"
			parseControllerComment(controllerDoc, it.getRawCommentText())
			log.debug("----Controller: ${it.name()} - Number of methods: ${it.methods().length}" + 
				" - Number of actions: ${actionSet?.size()}")
			it.methods().each { method -> //SimpleGroovyMethodDoc
				if(!actionSet.contains(method.name())) {
					log.debug("--- method ${method.name()} is not a controller Action")
					return
				} else log.debug("--- Adding Action: ${method.name()}")
				ControllerActionDoc controllerActionDoc =
					new ControllerActionDoc(method.name(), "$controllerBaseURI/${method.name()}");
				controllerDoc.addControllerAction(method.name(), controllerActionDoc)
				controllerActionDoc.commentDoc = parseActionComment(method.getRawCommentText())
			}
			if(actionSet.size() != controllerDoc.getControllerActions()?.size()) {
				log.error("ERROR - Controller: ${it.name()} - actionSet.size(): ${actionSet.size()} - " +
					" - controllerDoc.getControllerActions().size(): ${controllerDoc.getControllerActions().size()}")
			}
			generateDoc(controllerDoc);
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
	
	private void generateDoc(ControllerDoc controllerDoc) {
		log.debug(" --- generateDoc")
		String renderedSrc = groovyPageRenderer.render (
			view:"/templates/restControllerDoc", model: [controllerDoc: controllerDoc]);
		FileOutputTool output = new FileOutputTool();
		String fs = File.separator
		output.writeToOutput("${controllerDocViewBaseDir.absolutePath}${fs}${controllerDoc.logicalPropertyName}" + 
			"${fs}index.gsp", renderedSrc);
	}
	
	private String cleanComment(String comment) {
		if(!comment || "".equals(comment)) return comment;
		String[] commentLines = comment.split(System.getProperty("line.separator"));
		String finalComment = ""
		commentLines.each {
			it = it.replace('*', '').replace("\n", "<br/>").trim()
			finalComment = finalComment.concat(it)
		}
		return finalComment
	}
	
	private Map<String, Integer> getMethodsLineNumberMap(File controlleFile) {
		log.debug("getMethodsLineNumberMap")
		String fileString = new String(FileUtils.getBytesFromFile(controlleFile))
		def ast = new AstBuilder().buildFromString(CompilePhase.INSTRUCTION_SELECTION, false, fileString)
		Map<String, Integer> result = new HashMap<String, Integer>();
		ast[1].methods.each {//MethodNode
			log.debug("--- methods: ${it.name} - lineNumber: ${it.lineNumber}")
			result.put(it.name, it.lineNumber)
			it.getAnnotations().each {annotation ->
				log.debug("annotation: ${annotation}")
			}
		}
		return result;
	}
	
	private void parseControllerComment(
		ControllerDoc controllerDoc, String comment) {
		log.debug(" --- parseControllerComment: ${comment}")
		if(!comment || "".equals(comment)) return;
		comment = comment.replace("*", "");
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
						comment.substring(tokenIdx + "@infoController".length()).trim()
				} else {
					nexTokenIdx = comment.indexOf(nextToken)
					controllerDoc.infoController =	comment.substring(
						tokenIdx + "@infoController".length(), nexTokenIdx).trim()
					comment = comment.substring(nexTokenIdx)
				}
			} else if("@descController".equals(lastToken)) {
				if(!nextToken) {
					controllerDoc.descController =
						comment.substring(tokenIdx + "@descController".length()).trim()
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
		log.debug(" --- parseActionComment --- ")
		CommentDoc commentDoc = new CommentDoc()
		if(!comment || "".equals(comment)) return commentDoc;
		comment = comment.replace("*", "");
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
			//log.debug(" *** lastToken: '${lastToken}' - nextToken: ${nextToken} - comment: '${comment}'")
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

