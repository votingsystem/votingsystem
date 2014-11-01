<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
    <vs-innerpage-signal title="<g:message code="toolsPageTitle"/>"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width: 1300px; margin:0px auto 0px auto; padding: 0px 30px 0px 30px;">
        <div style="margin:30px 0px 0px 0px;">
            <g:if test="${request.getHeader("user-agent").toLowerCase().contains('android')}">
                <div class="userAdvert text-left">
                    <ul>
                        <li><g:message code="androidAppNeededMsg"/></li>
                        <li><g:message code="androidAppDownloadMsg"  args="${[grailsApplication.config.grails.serverURL + "/android/SistemaVotacion.apk"]}"/></li>
                        <li><g:message code="androidCertInstalledMsg"/></li>
                        <li><g:message code="androidSelectAppMsg"/></li>
                    </ul>
                </div>
            </g:if>
            <g:else>
                <div><g:message code="clientToolNeededMsg"
                                args="${["${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip"]}"/></div>
                <div><g:message code="javaRequirementsMsg"
                                args="${["http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"]}"/></div>
                <a href="${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip" class="downloadLink"
                   style="margin:40px 20px 0px 0px; width:400px;">
                    <g:message code="downloadClientToolAppLbl"/> <i class="fa fa-cogs"></i></a>
            </g:else>
        </div>
    </div>
</body>
</html>
<asset:script>
</asset:script>

