   <% def jarArchives = "AppletFirma.jar"
	appletJarDependencies?.each {
		jarArchives = jarArchives.concat(",lib/${it}")
	}
   %>
<!DOCTYPE html>
<html>
    <head>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
   	    <script type="text/javascript">

		function setMessageFromNativeClient(mensaje) {
			return parent.setMessageFromNativeClient(mensaje)
		}

		function getMessageToNativeClient() {
			return parent.getMessageToNativeClient()
		}
		
   	    </script>
    </head>
    <body>
		<APPLET CODEBASE="${grailsApplication.config.grails.serverURL}/applet" 
     		CODE="org.sistemavotacion.AppletFirma" 
     		ARCHIVE="${jarArchives}" 
     		HEIGHT=200
     		WIDTH=600> 
		</APPLET>
    </body>
</html>