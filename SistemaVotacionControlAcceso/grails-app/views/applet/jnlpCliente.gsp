<?xml version="1.0" encoding="utf-8"?>
<!-- Custom Progress bar Demo Application -->
<jnlp spec="1.0+" codebase="${grailsApplication.config.grails.serverURL}/applet" >
  <information>
    <title>Applet de firma del Sistema de Votación</title>
    <vendor>https://github.com/jgzornoza/SistemaVotacionControlAcceso</vendor>
    <description>Herramienta para firmas utilizada en el Sistema de Votación</description>    
  </information>
  <resources>
    <j2se version="1.6+"/>
    <g:each in="${appletJarDependencies}" var="it">
    	<jar href="lib/${it}" />  
    </g:each>
    <jar href="AppletFirma.jar" main="true" />
    <jar href="MonitorDescarga.jar" download="progress" />
  </resources>
  <applet-desc
  		name="org.sistemavotacion.AppletFirma"
  			main-class="org.sistemavotacion.AppletFirma"         
         progress-class="org.sistemavotacion.MonitorDescarga"
         width="600" height="170">
     </applet-desc>
</jnlp>