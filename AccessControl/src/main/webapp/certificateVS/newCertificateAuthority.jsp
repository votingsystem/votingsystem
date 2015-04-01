<html>
<head>

    <link href="${config.resourceURL}/vs-texteditor/vs-texteditor.html" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.newCACertLbl}"></vs-innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px;">
    <h3>
        <div class="pageHeader text-center">
            ${msg.newCACertLbl}
        </div>
    </h3>

    <div class="text-left" style="margin:10px 0 10px 0;">
        <ul>
            <li>${msg.systemAdminReservedOperationMsg}</li>
            <li>${msg.signatureRequiredMsg}</li>
        </ul>
    </div>

    <form onsubmit="return submitForm()">
        <div style="position:relative; width:100%;">
            <label>${msg.interestInfoLbl}</label>
            <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
        </div>

        <div layout vertical style="margin:15px 0px 0px 0px;">
            <label>${msg.pemCertLbl}</label>
            <textarea id="pemCert" rows="8" required=""></textarea>
        </div>

        <div style="margin:10px 10px 60px 0px;height:20px;">
            <div style="float:right;">
                <paper-button raised onclick="submitForm()">
                    <i class="fa fa-check"></i> ${msg.addCALbl}
                </paper-button>
            </div>
        </div>

    </form>
</div>
</body>
</html>
<script>
    var appMessageJSON

    function submitForm(){
        if(!document.getElementById('pemCert').validity.valid) {
            showMessageVS('${msg.fillAllFieldsERRORLbl}', '${msg.dataFormERRORLbl}')
            return false
        }
        var textEditor = document.querySelector('#textEditor')
        if(textEditor.getData() == 0) {
            textEditor.classList.add("formFieldError");
            showMessageVS('${msg.emptyDocumentERRORMsg}', '${msg.dataFormERRORLbl}')
            return false
        }
        var webAppMessage = new WebAppMessage(Operation.CERT_CA_NEW)
        webAppMessage.serviceURL = "${config.webURL}/certificateVS/addCertificateAuthority"
        webAppMessage.signedMessageSubject = "${msg.newCertificateAuthorityMsgSubject}"
        webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
                    operation:Operation.CERT_CA_NEW}
        webAppMessage.setCallback(function(appMessage) {
            console.log("newCertifiateAuthorityCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var caption = '${msg.newCACertERRORCaption}'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '${msg.newCACertOKCaption}'
                var msgTemplate = '${msg.accessLinkMsg}';
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

</script>