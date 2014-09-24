<%@ page import="org.votingsystem.model.CertificateVS" %>
<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/certificateVS/votingsystem-cert.gsp']"/>">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/reason-dialog.gsp']"/>">

    <style type="text/css" media="screen">

    </style>
</head>
<body>
<div class="pageContentDiv">
    <ol class="breadcrumbVS">
        <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
        <li><a href="${createLink(controller: 'certificateVS', action: 'certs')}"><g:message code="certsPageTitle"/></a></li>
        <li class="active"><g:message code="trustedCertPageTitle"/></li>
    </ol>

    <div id="adminButtonsDiv" class=""  style="width: 600px; margin:20px auto 0px auto;">
        <g:if test="${"admin".equals(params.menu) || "superadmin".equals(params.menu)}">
            <votingsystem-button onclick="document.querySelector('#reasonDialog').toggle()">
                <g:message code="cancelCertLbl"/>
            </votingsystem-button>
        </g:if>
    </div>
    <div>
        <votingsystem-cert id="certData" certvs='${certMap as grails.converters.JSON}'></votingsystem-cert>
    </div>
</div>

<div style="position: absolute; width: 100%; top:0px;left:0px;visibility:hidden">
    <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
        <reason-dialog id="reasonDialog" caption="<g:message code="cancelCertFormCaption"/>" opened="false"
                           isForAdmins="true"></reason-dialog>
    </div>
</div>

</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector("#reasonDialog").addEventListener('on-submit', function (e) {
            var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CERT_EDIT)
            webAppMessage.serviceURL = "${createLink(controller:'certificateVS', action:'editCert',absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code="cancelCertMessageSubject"/>"
            webAppMessage.signedContent = {operation:Operation.CERT_EDIT, reason:e.detail,
                changeCertToState:"${CertificateVS.State.CANCELLED.toString()}", serialNumber:"${certMap.serialNumber}"}
            webAppMessage.contentType = 'application/x-pkcs7-signature'
            webAppMessage.setCallback(function(appMessage) {
                document.querySelector("#certData").url = "${createLink( controller:'certificateVS', action:'cert')}/" + certMap.serialNumber + "?menu=" + menuType
            })
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        })
    });

    <g:if test="${CertificateVS.State.CANCELLED.toString().equals(certMap.state)}">
        document.querySelector('#cancelCertButton').style.display = "none"
    </g:if>
</asset:script>