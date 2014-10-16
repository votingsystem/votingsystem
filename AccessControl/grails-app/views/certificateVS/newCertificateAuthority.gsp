<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-texteditor', file: 'votingsystem-texteditor.html')}">
</head>
<body>
<votingsystem-innerpage-signal title="<g:message code="newCACertLbl"/>"></votingsystem-innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px;">
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
                    <i class="fa fa-check" style="margin:0 5px 0 2px;"></i> <g:message code="addCALbl"/>
                </votingsystem-button>
            </div>
        </div>

    </form>
</div>
</body>
</html>
<asset:script>
    var appMessageJSON

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
        webAppMessage.serviceURL = "${createLink( controller:'certificateVS', action:"addCertificateAuthority", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newCertificateAuthorityMsgSubject'/>"
        webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
                    operation:Operation.CERT_CA_NEW}
        webAppMessage.setCallback(function(appMessage) {
            console.log("newCertifiateAuthorityCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var caption = '<g:message code="newCACertERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newCACertOKCaption"/>'
                var msgTemplate = '<g:message code='accessLinkMsg'/>';
            }
            showMessageVS(msg, caption)
            window.scrollTo(0,0);
        })
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        return false
    }

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function() {
        if(appMessageJSON != null) {
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    });

</asset:script>