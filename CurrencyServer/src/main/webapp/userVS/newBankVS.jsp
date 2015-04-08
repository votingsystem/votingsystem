<html>
<head>
    <link href="${resourceURL}/paper-input/paper-input.html" rel="import"/>
</head>
<body>
<vs-innerpage-signal caption="${msg.newBankVSLbl}"></vs-innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px; margin:0px auto 0px auto;">
    <div style="margin:0px 30px 0px 30px;">
        <h3>
            <div class="pageHeader text-center">
                ${msg.newBankVSLbl}
            </div>
        </h3>

        <div class="text-left" style="margin:10px 0 10px 0;">
            <ul>
                <li>${msg.systemAdminReservedOperationMsg}</li>
                <li>${msg.signatureRequiredMsg}</li>
                <li>${msg.newBankVSAdviceMsg2}</li>
                <li>${msg.newBankVSAdviceMsg3}</li>
            </ul>
        </div>

        <form onsubmit="return submitForm()">
            <paper-input id="bankVSIBAN" floatinglabel style="width:400px; margin:0px 0px 0px 20px;" label="${msg.IBANLbl}"
                         validate="" error="${msg.requiredLbl}" style="" required>
            </paper-input>
            <div style="position:relative; width:100%;">
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div layout vertical style="margin:15px 0px 0px 0px;">
                <label>${msg.pemCertLbl}</label>
                <textarea id="pemCert" rows="8" required=""></textarea>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button class="btn btn-default" style="margin:10px 0px 0px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                        ${msg.newBankVSLbl} <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<script>
    var appMessageJSON = null

    function submitForm() {
        try {
            var textEditor = document.querySelector('#textEditor')
            if(document.querySelector('#bankVSIBAN').invalid) {
                showMessageVS('${msg.fillAllFieldsERRORLbl}', '${msg.dataFormERRORLbl}')
                return false
            }
            if(!document.querySelector('#pemCert').validity.valid) {
                showMessageVS('${msg.fillAllFieldsERRORLbl}', '${msg.dataFormERRORLbl}')
                return false
            }
            if(textEditor.getData() == 0) {
                textEditor.classList.add("formFieldError");
                showMessageVS('${msg.emptyDocumentERRORMsg}', '${msg.dataFormERRORLbl}')
                return false
            }
            var webAppMessage = new WebAppMessage(Operation.BANKVS_NEW)
            webAppMessage.serviceURL = "${restURL}/userVS/newBankVS"
            webAppMessage.signedMessageSubject = "${msg.newBankVSMsgSubject}"
            webAppMessage.signedContent = {info:textEditor.getData(),certChainPEM:document.querySelector("#pemCert").value,
                IBAN:document.querySelector("#bankVSIBAN").value, operation:Operation.BANKVS_NEW}
            webAppMessage.setCallback(function(appMessage) {
                console.log("newBankVSCallback - message: " + appMessage);
                appMessageJSON = toJSON(appMessage)
                var caption = '${msg.newBankVSERRORCaption}'
                var msg = appMessageJSON.message
                statusCode = appMessageJSON.statusCode
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = '${msg.newBankVSOKCaption}'
                    var msgTemplate = '${msg.accessLinkMsg}';
                }
                showMessageVS(msg, caption)
                window.scrollTo(0,0);
            })
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            appMessageJSON = null
            return false
        } catch(ex) {
            console.log(ex)
            return false
        }
    }

    document.querySelector("#coreSignals").addEventListener('core-signal-messagedialog-closed', function(e) {
        if(appMessageJSON != null) {
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                window.location.href = updateMenuLink(appMessageJSON.URL)
            }
        }
    });

</script>
