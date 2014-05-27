<div id='addClaimFieldDialog' class="modal fade">
    <div class="modal-dialog">
        <form id="newFieldClaimForm">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 id="resultCaption" class="modal-title" style="color: #6c0404; font-weight: bold;">
                        <g:message code="addClaimFieldLbl"/>
                    </h4>
                </div>
                <div class="modal-body">
                    <div id="addClaimFieldDialogMessageDiv" class='text-center'
                         style="color: #6c0404; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                    <p style="text-align: center;">
                        <g:message code="claimFieldDescriptionMsg"/>
                    </p>
                    <label><g:message code="addClaimFieldMsg"/></label>
                    <input type="text" id="claimFieldText" style=""
                           oninvalid="this.setCustomValidity('')"
                           onchange="this.setCustomValidity('')" class="form-control"
                           class="text ui-widget-content ui-corner-all" required/>
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


<div id="" title=""
     style="display:none;padding:30px 20px 30px 20px; margin:auto;">


	<form id="">

			<input id="submitClaimFieldText" type="submit" style="display:none;">
	</form>
</div> 
<r:script>
    var callerCallback

    function showAddClaimFieldDialog(callback) {
        $("#addClaimFieldDialog").modal("show");
        callerCallback = callback
    }

    $('#newFieldClaimForm').submit(function(event){
        event.preventDefault();
        $('#addClaimFieldDialogMessageDiv').html("")
        if(!document.getElementById('newFieldClaimForm').checkValidity()) {
            $('#addClaimFieldDialogMessageDiv').html("<g:message code="emptyFieldLbl"/>");
            return false
        }
        callerCallback($("#claimFieldText").val())
        $("#addClaimFieldDialog").modal("hide");
    })

</r:script>