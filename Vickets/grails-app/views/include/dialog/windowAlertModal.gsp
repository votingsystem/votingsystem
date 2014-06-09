<div id='windowAlertModal' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="windowAlertModalCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="alertLbl"/>
                </h4>
            </div>
            <div class="modal-body">
                <p id="windowAlertModalMsg" style="text-align: center;  font-size: 1.2em;"></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal" onclick="closeResultDialog();">
                    <g:message code="acceptLbl"/></button>
            </div>
        </div>
    </div>
</div>
<asset:script>
    function showWindowAlertModalMsg(message, caption) {
        document.getElementById('windowAlertModalMsg').innerHTML = message
        document.getElementById('windowAlertModalCaption').innerHTML = caption
        $('#windowAlertModal').modal('show');
    }

    $('#windowAlertModal').on('hidden.bs.modal', function (e) {
        document.getElementById('windowAlertModalMsg').innerHTML = ''
        document.getElementById('windowAlertModalCaption').innerHTML = '<g:message code="alertLbl"/>'
    })
</asset:script>