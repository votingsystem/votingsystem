<div id='anonymousReceiptDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code='operationOKCaption'/>
                </h4>
            </div>
            <div class="modal-body">
                <p id='anonymousReceiptDialogMessage' style="text-align: center; font-size: 1.2em;"></p>
                <p style="text-align: center; font-size: 1.2em;">
                    <g:message code="anonymousDelegationReceiptMsg"/>
                </p>
                <button type="button" class="btn btn-default" onclick="saveReceipt()">
                    <g:message code="saveReceiptLbl"/>
                </button>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal">
                    <g:message code="acceptLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<asset:script>

var hashReceiptAnonymousDelegation

function saveReceipt() {
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING,Operation.SAVE_RECEIPT_ANONYMOUS_DELEGATION)
        webAppMessage.message = document.getElementById("receipt").innerHTML.trim()
        webAppMessage.callerCallback = 'saveReceiptCallback'
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
}

function showAnonymousReceiptDialog(message, hashReceipt) {
	console.log("showResultDialog - hashReceipt: " + hashReceipt + " - message: "+ message);
    hashReceiptAnonymousDelegation = hashReceipt
	$('#anonymousReceiptDialogMessage').html(message);
	$('#anonymousReceiptDialog').modal('show')
}

function saveReceiptCallback() {
}

</asset:script>