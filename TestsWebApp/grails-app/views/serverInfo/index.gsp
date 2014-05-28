<!DOCTYPE html>
<html>
	<head>
        <meta name="layout" content="main"/>
        <link type="text/css" href="${resource(dir: 'css', file: 'pageIndex.css')}"  type="text/css" rel="stylesheet" media="screen, projection" />
        <title><g:message code="simulationWebAppCaption"/></title>
	</head>
	<body>
		<div id="status" role="complementary">
			<h1>Application Status</h1>
			<ul>
				<li>App version: <g:meta name="app.version"/></li>
				<li>Grails version: <g:meta name="app.grails.version"/></li>
				<li>Groovy version: ${GroovySystem.getVersion()}</li>
				<li>JVM version: ${System.getProperty('java.version')}</li>
				<li>Reloading active: ${grails.util.Environment.reloadingAgentEnabled}</li>
				<li>Controllers: ${grailsApplication.controllerClasses.size()}</li>
				<li>Domains: ${grailsApplication.domainClasses.size()}</li>
				<li>Services: ${grailsApplication.serviceClasses.size()}</li>
				<li>Tag Libraries: ${grailsApplication.tagLibClasses.size()}</li>
			</ul>
			<h1>Installed Plugins</h1>
			<ul>
				<g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
					<li>${plugin.name} - ${plugin.version}</li>
				</g:each>
			</ul>
		</div>
		<div id="page-body" role="main">
			<h1>Aplicación de pruebas del Sistema de Votación</h1>
			<p>Mediante esta aplicación se pueden hacer pruebas de estado y de carga del sistema dde votación.</p>
			<p align="center" style="width:500px;font-size: 1.3em;"><g:link controller="simulation"><g:message code="goToApp"/></g:link></p>
			<div id="controller-list" role="navigation" style="margin:30px 0px 0px 0px;">
				<h2>Available Controllers:</h2>
				<ul>
					<g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName } }">
						<li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
					</g:each>
				</ul>
			</div>
		</div>
	</body>
</html>