<%@ page import="org.votingsystem.signature.util.CertUtil; java.security.cert.X509Certificate" %>
<!DOCTYPE HTML>
<html>
<head>
    <meta name="layout" content="simplePage" />
    <!--https://github.com/josdejong/jsoneditor-->
    <link rel="stylesheet" type="text/css" href="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.css')}">
    <script type="text/javascript" src="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.js')}"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>

<body>
    <div layout flex horizontal wrap around-justified>
        <div layout vertical>
            <div layout horizontal center center-justified>
                <votingsystem-button onclick="sendCertAuthority()" style="margin: 0px 0px 0px 5px;">
                    <g:message code="sendCertAuthority"/>
                </votingsystem-button>
                <div flex style="font-size: 0.7em; text-align: center;">
                    <g:message code="operationForAdminSystemLbl"/>
                </div>
            </div>
            <div id="sendCertAuthority" style="width: 500px; height: 300px;"></div>
        </div>
    </div>
<g:set var="signatureVSService" bean="signatureVSService"/>
<%
    X509Certificate serverCert  = signatureVSService.getServerCert()
    byte[] serverCertPEMEncoded = CertUtil.getPEMEncoded (serverCert)
    def serverCertPEM = new String(serverCertPEMEncoded, 'UTF-8')
%>
<textarea id="pemCert" style="display:none;">${serverCertPEM}</textarea>
</body>
</html>
<asset:script>
    var signedContent

    var serviceURL = "${createLink( controller:'certificateVS', action:"addCertificateAuthority", absolute:true)}"
    var sendCertAuthorityEditor = new JSONEditor(document.querySelector("#sendCertAuthority"));
    var jsonSendCertAuthorityEditor = {operation:"CERT_CA_NEW", receiverName:"Receiver server name",
        signedMessageSubject:"<g:message code="newCertificateAuthorityMsgSubject"/>",
        signedContent:{operation:"CERT_CA_NEW", info:"Cert from Tests Server",certChainPEM:document.querySelector("#pemCert").value},
        serviceURL:"http://sistemavotacion.org/AccessControl/certificateVS/addCertificateAuthority",
        serverURL:"http://sistemavotacion.org/AccessControl",
        urlTimeStampServer:"${grailsApplication.config.VotingSystem.urlTimeStampServer}",
    }

    sendCertAuthorityEditor.set(jsonSendCertAuthorityEditor);

    function sendCertAuthority() {
        console.log("sendCertAuthority")
        var webAppMessage = sendCertAuthorityEditor.get();
        webAppMessage.statusCode = ResponseVS.SC_PROCESSING
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = {setClientToolMessage: function(appMessage) {
            console.log("sendCertAuthority - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            showMessageVS(appMessageJSON.message, "sendCertAuthority - status: " + appMessageJSON.statusCode)
        }}
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }
</asset:script>