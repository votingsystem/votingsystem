<div id='cancelSubscriptionFormDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="cancelSubscriptionFormCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <label class="control-label" ><g:message code="cancelSubscriptionFormMsg"/></label>
                <textarea id="cancelUserSubscriptionReason" class="form-control" rows="3"></textarea>
            </div>
            <div class="modal-footer">
                <button id="" type="button" data-dismiss="modal" class="btn btn-accept-vs" onclick="submitCancelSubscriptionForm()">
                    <g:message code="acceptLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<asset:script>

    var clientCallback = null

    function submitCancelSubscriptionForm() {
        if(clientCallback != null) clientCallback()
        $('#cancelSubscriptionFormDialog').modal('hide')
    }

    function showCancelSubscriptionFormDialog(callback) {
        console.log("showCancelSubscriptionFormDialog");
        clientCallback = callback
        $("#cancelUserSubscriptionReason").val("")
        $('#cancelSubscriptionFormDialog').modal('show')
    }
</asset:script>