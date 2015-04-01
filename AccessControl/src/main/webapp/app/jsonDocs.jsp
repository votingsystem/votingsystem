<!DOCTYPE HTML>
<html>
<head>
    <link href="${config.resourceURL}/jsoneditor/jsoneditor.min.css" media="all" rel="stylesheet" />
    <script src="${config.resourceURL}/jsoneditor/jsoneditor.min.js" type="text/javascript"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>

<body>
    <div layout flex horizontal wrap around-justified>
        <div layout vertical>
            <div layout horizontal center center-justified>
                <paper-button raised onclick="validateCert()" style="margin: 0px 0px 0px 5px;">
                    ${msg.validateCertLbl}
                </paper-button>
                ${msg.operationForAdminSystemLbl}
            </div>
            <div id="validateCert" style="width: 500px; height: 300px;"></div>
        </div>
    </div>
</body>
</html>
<script>
    var signedContent

    var validateCertEditor = new JSONEditor(document.querySelector("#validateCert"));
    var jsonValidateCert = {
        receiverName:"${config.serverName}",
        signedMessageSubject:"${msg.validateCertLbl}",
        signedContent:{nif:"", deviceId: ""},
        serviceURL:"${config.webURL}/csr/validate",
        serverURL:"${config.webURL}",
        timeStampServerURL:"${config.timeStampServerURL}",
    }
    validateCertEditor.set(jsonValidateCert);

    function validateCert() {
        console.log("publishControlCenter")
        var webAppMessage = validateCertEditor.get();
        webAppMessage.statusCode = ResponseVS.SC_PROCESSING
        webAppMessage.objectId = Math.random().toString(36).substring(7)
        window[webAppMessage.objectId] = function(appMessage) {
            console.log("publishControlCenter - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            showMessageVS(appMessageJSON.message, "validateCert - status: " + appMessageJSON.statusCode)
        }
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }
</script>
