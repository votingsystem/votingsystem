<%@ page import="org.votingsystem.model.CertificateVS" %>
<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webcomponent path="/certificateVS/votingsystem-cert"/>
    <vs:webcomponent path="/element/reason-dialog"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="trustedCertPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <div id="adminButtonsDiv" class=""  style="width: 600px; margin:20px auto 0px auto;">
        <g:if test="${"admin".equals(params.menu) || "superuser".equals(params.menu)}">
            <paper-button raised onclick="document.querySelector('#reasonDialog').toggle()">
                <g:message code="cancelCertLbl"/>
            </paper-button>
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
            var webAppMessage = new WebAppMessage(Operation.CERT_EDIT)
            webAppMessage.serviceURL = "${createLink(controller:'certificateVS', action:'editCert',absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code="cancelCertMessageSubject"/>"
            webAppMessage.signedContent = {operation:Operation.CERT_EDIT, reason:e.detail,
                changeCertToState:"${CertificateVS.State.CANCELLED.toString()}", serialNumber:"${certMap.serialNumber}"}
            webAppMessage.contentType = 'application/pkcs7-signature'
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