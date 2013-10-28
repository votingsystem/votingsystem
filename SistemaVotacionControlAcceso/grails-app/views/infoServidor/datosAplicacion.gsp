<div id="content" style="display:inline-block;top:0px;vertical-align: top;">
	<div id="controller-list" role="navigation">
		<h4>${message(code: 'controllersLabel', null)}:</h4>
		<ul>
			<g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName } }">
				<li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
			</g:each>
			</ul>
	</div>
</div>
<div id="status" role="complementary" style="display:inline-block;">
	<div style="float:right;">
		<div style="margin:25px 0px 0px 0px; font-size: 0.8em;">
		<h3 style="margin:15px 0px 0px 0px;">${message(code: 'appLabel', null)}</h3>
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
		</div>
		<div style="margin:45px 0px 0px 0px;font-size: 0.8em;">
			<h3>Plugins</h3>
			<ul>
				<g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
					<li>${plugin.name} - ${plugin.version}</li>
				</g:each>
			</ul>
		</div>
	</div>
</div>
		