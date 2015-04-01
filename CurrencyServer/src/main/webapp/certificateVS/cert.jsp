<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
    <link href="${config.webURL}/certificateVS/votingsystem-cert.vsp" rel="import"/>
    <link href="${config.webURL}/element/reason-dialog.vsp" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.trustedCertPageTitle}"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div id="adminButtonsDiv" class=""  style="width: 600px; margin:20px auto 0px auto;">
        <c:if test="${"admin".equals(params.menu) || "superuser".equals(params.menu)}">
            <paper-button raised onclick="document.querySelector('#reasonDialog').toggle()">
                ${msg.cancelCertLbl}
            </paper-button>
        </c:if>
    </div>
    <div>
        <votingsystem-cert id="certData" certvs='${certMap}'></votingsystem-cert>
    </div>
</div>

<div style="position: absolute; width: 100%; top:0px;left:0px;visibility:hidden">
    <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
        <reason-dialog id="reasonDialog" caption="${msg.cancelCertFormCaption}" opened="false"
                           isForAdmins="true"></reason-dialog>
    </div>
</div>

</body>
</html>
<script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector("#reasonDialog").addEventListener('on-submit', function (e) {
            var webAppMessage = new WebAppMessage(Operation.CERT_EDIT)
            webAppMessage.serviceURL = "${config.restURL}/certificateVS/editCert"
            webAppMessage.signedMessageSubject = "${msg.cancelCertMessageSubject}"
            webAppMessage.signedContent = {operation:Operation.CERT_EDIT, reason:e.detail,
                changeCertToState:"${CertificateVS.State.CANCELED.toString()}", serialNumber:"${certMap.serialNumber}"}
            webAppMessage.contentType = 'application/pkcs7-signature'
            webAppMessage.setCallback(function(appMessage) {
                document.querySelector("#certData").url = "${config.restURL}/certificateVS/cert/" + certMap.serialNumber + "?menu=" + menuType
            })
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        })
    });

    <c:if test="${CertificateVS.State.CANCELED.toString().equals(certMap.state)}">
        document.querySelector('#cancelCertButton').style.display = "none"
    </c:if>
</script>