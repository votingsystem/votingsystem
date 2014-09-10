<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>

<div id="contentDiv" class="pageContentDiv" style="min-height: 1000px;">
    <div style="margin:0px 30px 0px 30px;">
        <ol class="breadcrumbVS">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'certificateVS', action: 'certs')}"><g:message code="certsPageTitle"/></a></li>
            <li class="active"><g:message code="newCACertLbl"/></li>
        </ol>
        <h3>
            <div class="pageHeader text-center">
                <g:message code="newCACertLbl"/>
            </div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li><g:message code="systemAdminReservedOperationMsg"/></li>
                <li><g:message code="signatureRequiredMsg"/></li>
            </ul>
        </div>

        <form onsubmit="return submitForm()">
            <div style="position:relative; width:100%;">
                <label><g:message code="interestInfoLbl"/></label>
                <votingsystem-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></votingsystem-texteditor>
            </div>

            <div layout vertical style="margin:15px 0px 0px 0px;">
                <label><g:message code="pemCertLbl"/></label>
                <textarea id="pemCert" rows="8" required=""></textarea>
            </div>

            <div style="margin:10px 10px 60px 0px;height:20px;">
                <div style="float:right;">
                    <votingsystem-button onclick="submitForm()">
                        <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="addCALbl"/>
                    </votingsystem-button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<asset:script>

    function submitForm(){
        if(!document.getElementById('pemCert').validity.valid) {
            showMessageVS('<g:message code="fillAllFieldsERRORLbl"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var textEditor = document.querySelector('#textEditor')
        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CERT_CA_NEW)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink( controller:'certificateVS', action:"addCertificateAuthority", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newCertificateAuthorityMsgSubject'/>"
        webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
                    operation:Operation.CERT_CA_NEW}
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = callbackListener
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        return false
    }

    var appMessageJSON

    var callbackListener = {setClientToolMessage: function(appMessage) {
        console.log("newCertifiateAuthorityCallback - message: " + appMessage);
        appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var caption = '<g:message code="newCACertERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newCACertOKCaption"/>'
                var msgTemplate = '<g:message code='accessLinkMsg'/>';
            }
            showMessageVS(msg, caption)
        }
        window.scrollTo(0,0);
    }}

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function() {
        if(appMessageJSON != null) {
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    });

</asset:script>