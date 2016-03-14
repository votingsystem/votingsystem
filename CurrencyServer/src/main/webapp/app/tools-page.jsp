<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.currency.messages" var="bundle"/>

<dom-module name="tools-page">
    <template>
        <div style="margin:30px auto;" class="vertical layout center center-justified">
            <div style="margin: 20px;">
                <fmt:message key="nativeClientURLMsg" bundle="${bundle}">
                    <fmt:param value="${contextURL}/tools/NativeClient.zip"/>
                </fmt:message>
            </div>
            <div>
                <fmt:message key="javaRequirementsMsg" bundle="${bundle}">
                    <fmt:param value="http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"/>
                </fmt:message>
            </div>
        </div>
    </template>
    <script>
        Polymer({is:'tools-page'});
    </script>
</dom-module>
