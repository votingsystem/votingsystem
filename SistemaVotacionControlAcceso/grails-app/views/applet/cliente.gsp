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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
   	    <script type="text/javascript">

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
		
   	    </script>
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