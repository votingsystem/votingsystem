<?xml version="1.0" encoding="utf-8"?>
<!-- Custom Progress bar Demo Application -->
<jnlp spec="1.0+" codebase="${grailsApplication.config.grails.serverURL}/applet" >
  <information>
    <title>Herramienta de validación</title>
    <vendor>https://github.com/jgzornoza/HerramientaValidacionCopiasDeSeguridad</vendor>
    <description>Herramienta para validar firmas y copias de seguridad generadas por el Sistema de Votación</description>    
  </information>
  <resources>
    <j2se version="1.6+"/>
    <jar href="HerramientaValidacion.jar" main="true" />
    <g:if test="${params.gwt}">
    	<jar href="MonitorDescarga.jar" download="progress" />
	</g:if>
  </resources>
  <applet-desc
  		name="org.sistemavotacion.herramientavalidacion.AppletHerramienta"
  		<g:if test="${params.gwt}">
     	progress-class="org.sistemavotacion.MonitorDescarga"
		</g:if>
 		main-class="org.sistemavotacion.herramientavalidacion.AppletHerramienta"         
        width="600" height="170">
     </applet-desc>
</jnlp>