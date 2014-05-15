<div id='editRepresentativeDialog' class="modal fade">
    <div class="modal-dialog">
        <form id="editRepresentativeForm">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title" style="color: #870000; font-weight: bold;">
                        <g:message code="editRepresentativeLbl"/>
                    </h4>
                </div>
                <div class="modal-body">
                    <div id="editRepresentativeDialogMessageDiv" class='text-center'
                         style="color: #870000; font-size: 1.2em;font-weight: bold; margin-bottom: 15px;"></div>

                    <div id="editRepresentativeDialogFormDiv" style="margin:0px auto 0px 20px;">
                        <input id="resetEditRepresentativeForm" type="reset" style="display:none;">
                        <label style="margin:0px 0px 20px 0px"><g:message code="nifForEditRepresentativeLbl"/></label>
                        <input type="text" id="representativeNifText" style="width:350px; margin:0px auto 0px auto;" required
                               oninvalid="this.setCustomValidity('<g:message code="nifERRORMsg"/>')" class="form-control"
                               onchange="this.setCustomValidity('')" autofocus="autofocus"/>
                        <input id="submitNifCheck" type="submit" style="display:none;">
                    </div>
                    <div id="editRepresentativeDialogProgressDiv" style="display:none;">
                        <p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
                        <progress style='display:block;margin:0px auto 10px auto;'></progress>
                    </div>
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
<r:script>

var nifValidationResult

var nifValidation = function () {
    var nifInput = document.getElementById('representativeNifText')
    nifValidationResult = validateNIF(nifInput.value)
    console.log("validateNIF result: " + nifValidationResult)
    if (!nifValidationResult) {
            document.getElementById('representativeNifText').setCustomValidity("<g:message code='nifERRORMsg'/>");
    }
}

   $('#editRepresentativeForm').submit(function(event){	
		console.log("editRepresentativeForm")
		event.preventDefault();
        if(!document.getElementById('editRepresentativeForm').checkValidity()) {
            $('#editRepresentativeDialogMessageDiv').html("<g:message code="nifERRORMsg"/>");
            return false
        }
		$("#acceptButton").button("disable");
		$("#cancelButton").button("disable");
		$("#editRepresentativeDialogFormDiv").hide()
		$("#editRepresentativeDialogProgressDiv").fadeIn(500)
		var urlRequest = "${createLink(controller:'representative')}/edit/" + nifValidationResult
		console.log(" - editRepresentative - urlRequest: " + urlRequest)
		$.ajax({///user/$nif/representative
			contentType:'application/json',
			url: urlRequest
		}).done(function(resultMsg) {
		    var representativeDataJSON = toJSON(resultMsg)
            showRepresentativeData(representativeDataJSON)
		}).error(function(resultMsg) {
			showResultDialog('<g:message code="errorLbl"/>',resultMsg.responseText, errorCallback)
		}).always(function(resultMsg) {
			$("#editRepresentativeDialog").modal("hide");
		});

	 })

    function errorCallback(appMessage) {
        $("#editRepresentativeDialog").dialog("open");
    }

document.getElementById('representativeNifText').addEventListener('change', nifValidation, false);
</r:script>