<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="vs" uri="/WEB-INF/custom.tld"%>
<!DOCTYPE html>
<html>
<head></head>
<body>
    <vs-innerpage-signal caption="${msg.toolsPageTitle}"></vs-innerpage-signal>
    <div class="pageContentDiv" style="max-width: 1300px; margin:0px auto 0px auto; padding: 0px 30px 0px 30px;">
        <div style="margin:30px 0px 0px 0px;">
            <c:choose>
                <c:when test="${request.getHeader('user-agent').toLowerCase().contains('android')}" >
                    <div class="userAdvert text-left">
                        <ul>
                            <li>${msg.androidAppNeededMsg}</li>
                            <li>
                                <vs:msg value="${msg.androidAppDownloadMsg}" args="${conf.contextURL}/android/SistemaVotacion.apk"/>
                            <li>${msg.androidCertInstalledMsg}</li>
                            <li>${msg.androidSelectAppMsg}</li>
                        </ul>
                    </div>
                </c:when>
                <c:otherwise>
                    <div>
                        <vs:msg value="${msg.androidAppDownloadMsg}" args="${conf.contextURL}/tools/ClientTool.zip"/>
                    <div>
                        <vs:msg value="${msg.javaRequirementsMsg}"
                                args="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"/>
                    </div>
                    <a href="${config.webURL}/tools/ClientTool.zip" class="downloadLink"
                       style="margin:40px 20px 0px 0px; width:400px;">
                            ${msg.downloadClientToolAppLbl} <i class="fa fa-cogs"></i></a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>
<script>
</script>