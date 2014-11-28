<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal caption="<g:message code="newUserCertLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px;">
    <div style="margin:0px 30px 0px 30px;">
        <h3>
            <div class="pageHeader text-center">
                <g:message code="newUserCertLbl"/>
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
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div style="margin:15px 0px 0px 0px;">
                <label><g:message code="pemCertLbl"/></label>
                <textarea id="pemCert" rows="8" required=""></textarea>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button type="submit" class="btn btn-default">
                        <g:message code="doOperationLbl"/> <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<asset:script>
    var textEditor = document.querySelector('#textEditor')

    var appMessageJSON
    function submitForm() {
        if(!document.getElementById('pemCert').validity.valid) {
            showMessageVS('<g:message code="fillAllFieldsERRORLbl"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }

        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('<g:message code="emptyDocumentERRORMsg"/>', '<g:message code="dataFormERRORLbl"/>')
            return false
        }
        var webAppMessage = new WebAppMessage(Operation.CERT_CA_NEW)
        webAppMessage.serviceURL = "${createLink( controller:'userVS', action:"save", absolute:true)}"
        webAppMessage.signedMessageSubject = "<g:message code='newUserCertLbl'/>"
        webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
                    operation:Operation.CERT_USER_NEW}
        webAppMessage.setCallback(function(appMessage) {
        console.log("newUserCertCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var caption = '<g:message code="newUserCertERRORCaption"/>'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '<g:message code="newUserCertOKCaption"/>'
                var msgTemplate = '<g:message code='accessLinkMsg'/>';
            }
            newCertURL = updateMenuLink(appMessageJSON.URL)
            showMessageVS(msg, caption)
            window.scrollTo(0,0);
        })
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        return false
    }

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function(e) {
        if(appMessageJSON.URL != null) window.location.href = updateMenuLink(appMessageJSON.URL)
    });

</asset:script>
<asset:deferredScripts/>