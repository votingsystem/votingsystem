<div id="anonymousRepresentativeDateRangeDialog" class="modal fade">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title"><g:message code="anonymousRepresentativedateRangeCaption"/></h4>
            </div>
            <div class="errorMsgWrapper" style="display:none;"></div>
            <div class="modal-body">
                <form id="representativeDateRangeForm">
                    <p style="text-align: center;">
                        <label style="display: block;"><g:message code="numWeeksAnonymousDelegationMsg"/></label>
                        <input type="number" id="numWeeksAnonymousDelegation" min="1" value="" max="52" required
                               style="width:120px;margin:10px 20px 0px 7px;" class="form-control"
                               title="<g:message code="numWeeksAnonymousDelegationMsg"/>"
                               oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                               onchange="this.setCustomValidity('')">
                    </p>
                    <button id="representativeDateRangeFormDummyButton" style="display: none;"></button>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal"><g:message code="cancelLbl"/></button>
                <button type="button" class="btn btn-primary btn-accept-vs" onclick="$('#representativeDateRangeFormDummyButton').click();">
                    <g:message code="acceptLbl"/>
                </button>
            </div>
        </div>
    </div>
</div>
<r:script>

var callerCallback

function showAnonymousRepresentativeDateRangeDialog(callback) {
	$("#anonymousRepresentativeDateRangeDialog").modal("show");
	$("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").hide()
	if(callback != null) callerCallback = callback
}

$('#representativeDateRangeForm').submit(function(event){
    event.preventDefault();
    weeksAnonymousDelegation = $('#numWeeksAnonymousDelegation').val()
    if (Number(weeksAnonymousDelegation) <= 0) {
        showRepresentativeDateRangeErrorMsg('<g:message code="numberFieldLbl"/>')
        return false
    }
    if(weeksAnonymousDelegation > 52) {
        var msgTemplate = "<g:message code="maxValueErrorMsg"/>"
        showRepresentativeDateRangeErrorMsg(msgTemplate.format(52))
        return false
    }
    $("#anonymousRepresentativeDateRangeDialog").modal("hide");
    return false
})


function showRepresentativeDateRangeErrorMsg(errorMsg) {
    console.log("========= errorMsg: " + errorMsg)
    $("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
    $("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").fadeIn()
}
</r:script>