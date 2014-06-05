<div id='cancelGroupVSDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="cancelGroupVSSignedMessageSubject"/>
                </h4>
            </div>
            <div class="modal-body">
                <p id='cancelGroupVSMsg' style="text-align: center;  font-size: 1.2em;">
                </p>
                <p style="text-align: center;  font-size: 1.2em;"><g:message code="confirmOperationMsg"/></p>
            </div>
            <div class="modal-footer">
                <button id="" type="submit" class="btn btn-accept-vs" onclick="cancelGroupVS()">
                    <g:message code="acceptLbl"/></button>
                </button>
                <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                    <g:message code="closeLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<asset:script>

    var cancelGroupVSDialogMsgTemplate = "<g:message code="cancelGroupVSDialogMsg"/>"
    var groupVSName = null
    var groupVSId = null

    function showCancelGroupVSDialog(groupName, groupId) {
        console.log("showCancelGroupVSDialog - groupName: " + groupName);
        groupVSName = groupName
        groupVSId = groupId
        $('#cancelGroupVSMsg').html(cancelGroupVSDialogMsgTemplate.format(groupName));
        $('#cancelGroupVSDialog').modal('show')
    }

    function cancelGroupVS() {
        console.log("cancelGroupVS")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.VICKET_GROUP_CANCEL)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/" + groupVSId
        webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
        webAppMessage.signedContent = {operation:Operation.VICKET_GROUP_CANCEL, groupvsName:groupVSName, id:groupVSId}
        //signed and encrypted
        webAppMessage.contentType = 'application/x-pkcs7-signature, application/x-pkcs7-mime'
        webAppMessage.callerCallback = 'showCancelGroupVSDialogCallback'
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    var groupURL = null

    function cancelGroupResultOKCallback() {
        window.location.href = updateMenuLink(groupURL)
    }

    function showCancelGroupVSDialogCallback(appMessage) {
        console.log("showCancelGroupVSDialogCallback - message from native client: " + appMessage);
        var appMessageJSON = toJSON(appMessage)
        if(appMessageJSON != null) {
            var callBackResult = null
            var caption = '<g:message code="groupCancelERRORLbl"/>'
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                caption = "<g:message code='groupCancelOKLbl'/>"
                callBackResult = cancelGroupResultOKCallback
                groupURL = appMessageJSON.URL
            }
            var msg = appMessageJSON.message
            $('#cancelGroupVSDialog').modal('hide')
            showResultDialog(caption, msg, cancelGroupResultOKCallback)
        }
    }

</asset:script>