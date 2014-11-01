<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal title="<g:message code="toolsPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto;">
    <g:if test="${request.getHeader("user-agent").toLowerCase().contains('android')}">
        <div class="userAdvert">
            <ul>
                <li><g:message code="androidAppNeededMsg"/></li>
                <li><g:message code="androidAppDownloadMsg"  args="${[grailsApplication.config.grails.serverURL + "/android/SistemaVotacion.apk"]}"/></li>
                <li><g:message code="androidCertInstalledMsg"/></li>
                <li><g:message code="androidSelectAppMsg"/></li>
            </ul>
        </div>
    </g:if>
    <g:else>
        <div style="margin: 10px;"><g:message code="clientToolNeededMsg"
              args="${["${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip"]}"/>
        </div>.
        <div style="margin: 10px;"><g:message code="javaRequirementsMsg"
                        args="${["http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"]}"/>
        </div>
    </g:else>
</div>
</body>
</html>
<asset:script>
</asset:script>

