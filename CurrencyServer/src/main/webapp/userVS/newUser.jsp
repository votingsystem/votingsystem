<html>
<head>

</head>
<body>
<vs-innerpage-signal caption="${msg.newUserCertLbl}"></vs-innerpage-signal>
<div class="pageContentDiv" style="min-height: 1000px;">
    <div style="margin:0px 30px 0px 30px;">
        <h3>
            <div class="pageHeader text-center">
                ${msg.newUserCertLbl}
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
                <textarea id="userInfo" rows="8" required=""></textarea>
            </div>

            <div style="margin:15px 0px 0px 0px;">
                <label>${msg.pemCertLbl}</label>
                <textarea id="pemCert" rows="8" required=""></textarea>
            </div>

            <div style="position:relative; margin:10px 10px 60px 0px;height:20px;">
                <div style="position:absolute; right:0;">
                    <button type="submit" class="btn btn-default">
                        ${msg.doOperationLbl} <i class="fa fa fa-check"></i>
                    </button>
                </div>
            </div>

        </form>
    </div>
</div>
</body>
</html>
<script>

    var appMessageJSON
    function submitForm() {
        if(!document.getElementById('pemCert').validity.valid) {
            showMessageVS('${msg.fillAllFieldsERRORLbl}', '${msg.dataFormERRORLbl}')
            return false
        }

        if(!document.getElementById('userInfo').validity.valid) {
            showMessageVS('${msg.fillAllFieldsERRORLbl}', '${msg.dataFormERRORLbl}')
            return false
        }

        var webAppMessage = new WebAppMessage(Operation.CERT_CA_NEW)
        webAppMessage.serviceURL = "${config.restURL}/userVS/save"
        webAppMessage.signedMessageSubject = "${msg.newUserCertLbl}"
        webAppMessage.signedContent = {info:document.querySelector("#userInfo").value,
                certChainPEM:document.querySelector("#pemCert").value,
                operation:Operation.CERT_USER_NEW}
        webAppMessage.setCallback(function(appMessage) {
        console.log("newUserCertCallback - message: " + appMessage);
            appMessageJSON = toJSON(appMessage)
            var caption = '${msg.newUserCertERRORCaption}'
            var msg = appMessageJSON.message
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = '${msg.newUserCertOKCaption}'
                var msgTemplate = '${msg.accessLinkMsg}';
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

</script>
