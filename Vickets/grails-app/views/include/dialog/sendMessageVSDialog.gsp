<div id='sendMessageVSDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="sendMessageVSDialogCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <p id='sendMessageVSMsg' style="text-align: center;  font-size: 1.2em;">
                </p>
                <label class="control-label" ><g:message code="sendMessageVSMsg"/></label>
                <textarea id="messageVSContent" class="form-control" rows="4"></textarea>
            </div>
            <div class="modal-footer">
                <button id="" type="submit" class="btn btn-accept-vs" onclick="sendMessageVS()">
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

    var callback

    function showSendMessageVSDialog(message, callbackParam) {
        callback = callbackParam
        console.log("showSendMessageVSDialog - message: " + message);
        if(message == null || "" == message.trim()) document.getElementById("sendMessageVSMsg").style.display = 'none'
        else document.getElementById("sendMessageVSMsg").innerHTML = message
        $('#sendMessageVSDialog').modal('show')
    }

    function sendMessageVS() {
        console.log("sendMessageVS")
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.MESSAGEVS)
        webAppMessage.receiverName="${grailsApplication.config.VotingSystem.serverName}"
        webAppMessage.serverURL="${grailsApplication.config.grails.serverURL}"
        webAppMessage.serviceURL = "${createLink(controller:'messsageVS', absolute:true)}/"
        webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
        webAppMessage.signedContent = {operation:Operation.MESSAGEVS, messageContent:document.getElementById('messageVSContent').value}
        webAppMessage.certificateList =  toJSON(document.getElementById("certificateListDiv").innerHTML)

        //signed and encrypted
        webAppMessage.contentType = 'application/messagevs'
        webAppMessage.callerCallback = getFnName(callback)
        webAppMessage.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

</asset:script>