<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="org.votingsystem.web.accesscontrol.messages" var="bundle"/>

<dom-module name="tools-page">
    <style>
        .buttonvs {
            box-shadow: 1px 2px 1px 0px rgba(0, 0, 0, 0.24);
            border: 1px solid #efefef;
        }
    </style>
    <template>
        <div class="pagevs vertical layout center center-justified">
            <div class="buttonvs" style="width: 280px;font-size: 1.1em;" data-route="publish_election" href="/publish_election">
                <i class="fa fa-envelope"></i> ${msg.publishVoteLbl}
            </div>
            <div class="buttonvs" style="width: 280px; margin: 10px 0 0 0; font-size: 1.1em;" data-route="edit_representative" href="/representative/edit">
                <i class="fa fa-hand-o-right"></i> ${msg.newRepresentativeLbl}
            </div>
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
            <div class="flex"></div>
        </div>
    </template>
    <script>
        Polymer({is:'tools-page'});
    </script>
</dom-module>
