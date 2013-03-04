<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <meta name="layout" content="main"/>
    </head>
    <body>
     	<div class="container">
    
	    <div class="navbar">   
			<div class="navbar-inner">
			    <div class="container">
			 		<ul class="nav">
						<li><a href="informacion">${message(code: 'infoLabel', null)}</a></li>
						<li><a href="listaServicios">${message(code: 'servicesListLabel', null)}</a></li>
						<li class="active"><a href="datosAplicacion">${message(code: 'appDataLabel', null)}</a></li>
					</ul>
				</div>
			</div>
		</div>
    
		<div id="status" role="complementary">
			<h1>${message(code: 'appLabel', null)}</h1>
			<ul>
				<li>${message(code: 'appVersionLabel', null)}: <g:meta name="app.version"/></li>
				<li>${message(code: 'grailsVersionLabel', null)}: <g:meta name="app.grails.version"/></li>
				<li>${message(code: 'groovyVersionLabel', null)}: ${org.codehaus.groovy.runtime.InvokerHelper.getVersion()}</li>
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
				<h2>${message(code: 'controllersLabel', null)}:</h2>
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
