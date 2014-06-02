<div id='cancelCertFormDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="cancelCertFormCaption"/>
                </h4>
            </div>
            <div class="modal-body">
                <div class="dialogAdviceMsg text-center"><i class="fa fa-exclamation-triangle"></i>
                    <g:message code="systemAdminReservedOperationMsg"/></div>
                <label class="control-label" ><g:message code="cancelSubscriptionFormMsg"/></label>
                <textarea id="cancelCertReason" class="form-control" rows="4"></textarea>
            </div>
            <div class="modal-footer">
                <button id="" type="button" data-dismiss="modal" class="btn btn-accept-vs" onclick="submitCancelCertForm()">
                    <g:message code="acceptLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<asset:script>

    var clientCallback = null

    function submitCancelCertForm() {
        if(clientCallback != null) clientCallback()
        $('#cancelCertFormDialog').modal('hide')
    }

    function showCancelSubscriptionFormDialog(callback) {
        console.log("showCancelSubscriptionFormDialog");
        clientCallback = callback
        $("#cancelCertReason").val("")
        $('#cancelCertFormDialog').modal('show')
    }
</asset:script>