<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main" />
    </head>
    <body>
   	    <div class="container">
   	    
	    <ul class="nav nav-tabs">
			<li><a href="informacion"><g:message code="infoLabel"/></a></li>
			<li><a href="listaServicios"><g:message code="serviceURLSMsg"/></a></li>
			<li class="active"><a href="datosAplicacion"><g:message code="appDataLabel"/></a></li>
		</ul>	
    
		<div id="status" role="complementary">
			<h1>${message(code: 'appLabel', null)}</h1>
			<ul>
				<li>${message(code: 'appVersionLabel', null)}: <g:meta name="app.version"/></li>
				<li>${message(code: 'grailsVersionLabel', null)}: <g:meta name="app.grails.version"/></li>
				<li>${message(code: 'groovyVersionLabel', null)}: ${GroovySystem.getVersion()}</li>
				<li>${message(code: 'jvmVersionLabel', null)}: ${System.getProperty('java.version')}</li>
				<li>${message(code: 'controllersLabel', null)}: ${grailsApplication.controllerClasses.size()}</li>
				<li>${message(code: 'domainsLabel', null)}: ${grailsApplication.domainClasses.size()}</li>
				<li>${message(code: 'servicesLabel', null)}: ${grailsApplication.serviceClasses.size()}</li>
				<li>${message(code: 'tagLibrariesLabel', null)}: ${grailsApplication.tagLibClasses.size()}</li>
			</ul>
			<h1>Plugins</h1>
			<ul>
				<g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
					<li>${plugin.name} - ${plugin.version}</li>
				</g:each>
			</ul>
		</div>
		<div id="content">
			<div id="controller-list" role="navigation">
				<h4>${message(code: 'controllersLabel', null)}:</h4>
				<ul>
					<g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName } }">
						<li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
					</g:each>
				</ul>
			</div>
		</div>
		
		</div>
		
	</body>
</html>
