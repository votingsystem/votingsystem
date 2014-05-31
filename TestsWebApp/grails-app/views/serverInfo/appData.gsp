<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="padding: 0px 30px 0px 30px;">
        <div id="status" class="col-md-4">
            <h3>Application Status</h3>
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
            <h3>Installed Plugins</h3>
            <ul>
                <g:each var="plugin" in="${applicationContext.getBean('pluginManager').allPlugins}">
                    <li>${plugin.name} - ${plugin.version}</li>
                </g:each>
            </ul>
        </div>
        <div id="page-body"  class="col-md-6" style="">
            <h3>Aplicación de pruebas del Sistema de Votación</h3>
            <p>Mediante esta aplicación se pueden hacer pruebas de estado y de carga del sistema dde votación.</p>
            <p align="center" style="width:500px;font-size: 1.3em;"><g:link controller="simulation"><g:message code="goToApp"/></g:link></p>
            <div id="controller-list" role="navigation" style="margin:30px 0px 0px 0px;">
                <h3>Available Controllers:</h3>
                <ul>
                    <g:each var="c" in="${grailsApplication.controllerClasses.sort { it.fullName } }">
                        <li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
                    </g:each>
                </ul>
            </div>
        </div>
    </div>
</div>
</body>
</html>