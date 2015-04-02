<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.accesscontrol.messages" var="bundle"/>
<html>
<head>
</head>
<body>

<vs-innerpage-signal caption="${msg.toolsPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto;">
    <c:choose>
        <c:when test="${request.getHeader('user-agent').toLowerCase().contains('android')}">
            <div class="userAdvert">
                <ul>
                    <li>${msg.androidAppNeededMsg}</li>
                    <li>
                        <fmt:message key="androidAppDownloadMsg" bundle="${bundle}">
                            <fmt:param value="${config.contextURL}/android/SistemaVotacion.apk"/>
                        </fmt:message>
                    </li>
                    <li>${msg.androidCertInstalledMsg}</li>
                    <li>${msg.androidSelectAppMsg}</li>
                </ul>
            </div>
        </c:when>
        <c:otherwise>
            <div style="margin: 10px;">
                <fmt:message key="clientToolNeededMsg" bundle="${bundle}">
                    <fmt:param value="${config.contextURL}/tools/ClientTool.zip"/>
                </fmt:message>
            </div>.
            <div style="margin: 10px;">
                <fmt:message key="javaRequirementsMsg" bundle="${bundle}">
                    <fmt:param value="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"/>
                </fmt:message>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
<script>
</script>

