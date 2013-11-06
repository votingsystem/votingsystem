<% def jarArchives = "AppletFirma.jar"
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
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
   	    <r:script>

		function setMessageFromSignatureClient(mensaje) {
			return parent.setMessageFromSignatureClient(mensaje)
		}

		function getMessageToSignatureClient() {
			return parent.getMessageToSignatureClient()
		}

		function loadCallerCallback() {
			var signatureClientCallback = parent.signatureClientCallback
			this[parent.getFnName(signatureClientCallback)] = signatureClientCallback
		}
		
   	    </r:script>
    </head>
    <body onload="loadCallerCallback()">
		<APPLET CODEBASE="${grailsApplication.config.grails.serverURL}/applet" 
     		CODE="org.sistemavotacion.AppletFirma" 
     		ARCHIVE="${jarArchives}" 
     		HEIGHT=200
     		WIDTH=600> 
		</APPLET>
    </body>
</html>