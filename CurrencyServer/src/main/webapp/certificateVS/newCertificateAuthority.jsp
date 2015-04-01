<html>
<head>

    <link href="${config.resourceURL}/vs-texteditor/vs-texteditor.html" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.newCACertLbl}"></vs-innerpage-signal>
<div id="contentDiv" class="pageContentDiv" style="min-height: 1000px;">
    <div style="margin:0px 30px 0px 30px;">
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

            <div style="margin:15px 0px 0px 0px;">
                <label>${msg.pemCertLbl}</label>
                <textarea id="pemCert" rows="8" required=""></textarea>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <paper-button raised onclick="submitForm()" style="margin: 0px 0px 0px 5px;">
                        ${msg.addCALbl} <i class="fa fa-check"></i>
                    </paper-button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<script>

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
        webAppMessage.serviceURL = "${config.restURL}/certificateVS/addCertificateAuthority"
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

        var objectId = Math.random().toString(36).substring(7)
        window[objectId] = callbackListener
        webAppMessage.callerCallback = objectId
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        return false
    }

    var appMessageJSON

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function() {
        if(appMessageJSON != null) {
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    });

</script>