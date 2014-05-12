<div id='resultDialog' class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #870000;"></h4>
            </div>
            <div class="modal-body">
                <p id='resultMessage' style="text-align: center;"></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-accept-vs" data-dismiss="modal" onclick="closeResultDialog();">
                    <g:message code="acceptLbl"/></button>
            </div>
        </div>
    </div>
</div>

<div id='resultDialog' title='' style="display:none;">
    <p id='resultMessage' style="text-align: center;"></p>
</div>
<r:script>

    var clientCallback = null

    function closeResultDialog() {
        if(clientCallback != null) clientCallback()
        $('#resultDialog').modal('hide')
    }

    function showResultDialog(caption, message, callback) {
        console.log("showResultDialog - caption: " + caption + " - message: "+ message);
        $('#resultMessage').html(message);
        $("#resultCaption").html(caption);
        $('#resultDialog').modal('show')
        clientCallback = callback
    }
</r:script>