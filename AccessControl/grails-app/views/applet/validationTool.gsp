<% def jarArchives = "ValidationToolApplet.jar"
	String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
	def appletJarDependencies = []
	new File(depsPath).eachFile() { file->
		if(file.path.endsWith(".jar"))
			appletJarDependencies.add(file.getName())
	}
	appletJarDependencies?.each {
		jarArchives = jarArchives.concat(",lib/${it}")
	}
	
%>
<!DOCTYPE html>
<html>
    <head>
        <r:layoutResources />
    </head>
    <body>    	
		<APPLET CODEBASE="${grailsApplication.config.grails.serverURL}/applet"
     		CODE="org.votingsystem.applet.validationtool.ValidationApplet"
     		ARCHIVE="${jarArchives}" 
     		HEIGHT=200
     		WIDTH=600> 
		</APPLET>
    </body>
</html>
<r:script>

	function setMessageFromValidationTool(message) {
		return parent.votingSystemClient.setMessageFromValidationTool(message)
	}
	
	function getMessageToValidationTool() {
		return parent.votingSystemClient.getMessageToValidationTool()
	}

</r:script>
<r:layoutResources />