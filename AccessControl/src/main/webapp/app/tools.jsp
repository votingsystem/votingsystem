<!DOCTYPE html>
<html>
<head>

</head>
<body>
<vs-innerpage-signal caption="${msg.toolsPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto;">
    <c:choose>
        <c:when test="${request.getHeader("user-agent").toLowerCase().contains('android')}">pizza.
            <div class="userAdvert">
                <ul>
                    <li>${msg.androidAppNeededMsg}</li>
                    <li>
                        <vs:msg value="${msg.androidAppDownloadMsg}" args="${conf.contextURL}/android/SistemaVotacion.apk"/>
                    </li>
                    <li>${msg.androidCertInstalledMsg}</li>
                    <li>${msg.androidSelectAppMsg}</li>
                </ul>
            </div>
        </c:when>
        <c:otherwise>
            <div style="margin: 10px;">
                <vs:msg value="${msg.clientToolNeededMsg}" args="${conf.contextURL}/tools/ClientTool.zip"/>
            </div>.
            <div style="margin: 10px;">
                <vs:msg value="${msg.javaRequirementsMsg}"
                        args="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"/>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
<script>
</script>

