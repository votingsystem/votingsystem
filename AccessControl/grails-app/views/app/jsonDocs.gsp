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
                <paper-button raised onclick="publishControlCenter()" style="margin: 0px 0px 0px 5px;">
                    <g:message code="publishControlCenterLbl"/>
                </paper-button>
                <g:message code="operationForAdminSystemLbl"/>
            </div>
            <div id="publishControlCenter" style="width: 500px; height: 300px;"></div>
        </div>
        <div layout vertical>
            <div layout horizontal center center-justified>
                <paper-button raised onclick="validateCert()" style="margin: 0px 0px 0px 5px;">
                    <g:message code="validateCertLbl"/>
                </paper-button>
                <g:message code="operationForAdminSystemLbl"/>
            </div>
            <div id="validateCert" style="width: 500px; height: 300px;"></div>
        </div>
    </div>
</body>
</html>
<asset:script>
    var signedContent

    var publishControlCenterEditor = new JSONEditor(document.querySelector("#publishControlCenter"));
    var jsonPublishControlCenter = {operation:"CONTROL_CENTER_ASSOCIATION",
        receiverName:"${grailsApplication.config.vs.serverName}",
        signedMessageSubject:"<g:message code="addControlCenterMsgSubject"/>",
        signedContent:{operation:"CONTROL_CENTER_ASSOCIATION", serverURL: "http://www.sistemavotacion.org/ControlCenter"},
        serviceURL:"${createLink( controller:'subscriptionVS', absolute:true)}",
        serverURL:"${grailsApplication.config.grails.serverURL}",
        urlTimeStampServer:"${grailsApplication.config.vs.urlTimeStampServer}",
    }
    publishControlCenterEditor.set(jsonPublishControlCenter);

    function publishControlCenter() {
        console.log("publishControlCenter")
        var webAppMessage = publishControlCenterEditor.get();
        webAppMessage.statusCode = ResponseVS.SC_PROCESSING
        webAppMessage.objectId = Math.random().toString(36).substring(7)
        window[webAppMessage.objectId] = function(appMessage) {
            console.log("publishControlCenter - message: " + appMessage);
            var appMessageJSON = toJSON(appMessage)
            showMessageVS(appMessageJSON.message, "publishControlCenter - status: " + appMessageJSON.statusCode)
        }
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    var validateCertEditor = new JSONEditor(document.querySelector("#validateCert"));
    var jsonValidateCert = {operation:"CONTROL_CENTER_ASSOCIATION",
        receiverName:"${grailsApplication.config.vs.serverName}",
        signedMessageSubject:"<g:message code="validateCertLbl"/>",
        signedContent:{nif:"", deviceId: ""},
        serviceURL:"${createLink( controller:'csr', action:'validate', absolute:true)}",
        serverURL:"${grailsApplication.config.grails.serverURL}",
        urlTimeStampServer:"${grailsApplication.config.vs.urlTimeStampServer}",
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
</asset:script>
<asset:deferredScripts/>