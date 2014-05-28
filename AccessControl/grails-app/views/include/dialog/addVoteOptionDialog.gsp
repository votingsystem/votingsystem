<div id='addVoteOptionDialog' class="modal fade">
    <div class="modal-dialog">
        <form id="addVoteOptionForm">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 id="resultCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                    <g:message code="addOptionLbl"/>
                </h4>
            </div>
            <div class="modal-body">
                <div id="addVoteOptionDialogMessageDiv" class='text-center'
                     style="color: #6c0404; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                <label><g:message code="pollOptionContentMsg"/></label>

                    <input type="text" id="newOptionText" style="width:350px; margin:10px auto 0px auto;"
                           oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                           onchange="this.setCustomValidity('')" class="form-control" required autofocus/>
                    <input id="submitOption" type="submit" style="display:none;">
            </div>
            <div class="modal-footer">
                <button id="" type="submit" class="btn btn-accept-vs">
                    <g:message code="acceptLbl"/>
                </button>
                <button id="" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                    <g:message code="cancelLbl"/>
                </button>
            </div>
        </div>
        </form>
    </div>
</div>
<asset:script>
    var callerCallback

    function showAddVoteOptionDialog(callback) {
        $("#newOptionText").val("")
        $("#addVoteOptionDialog").modal("show");
        callerCallback = callback
    }

    $('#addVoteOptionForm').submit(function(event){
        event.preventDefault();
        $('#addVoteOptionDialogMessageDiv').html("")
        if(!document.getElementById('addVoteOptionForm').checkValidity()) {
            $('#addVoteOptionDialogMessageDiv').html("<g:message code="formErrorMsg"/>");
            return false
        }
        callerCallback($("#newOptionText").val())
        $("#addVoteOptionDialog").modal("hide");
    })

</asset:script>