<div id="anonymousRepresentativeDateRangeDialog" title="<g:message code="anonymousRepresentativedateRangeCaption"/>"
     style="display:none;">
	<div class="errorMsgWrapper" style="display:none;"></div>
    <form id="representativeDateRangeForm" onsubmit="return submitRepresentativeDateRangeForm(this);">
        <p style="text-align: center;">
            <label style="display: block;"><g:message code="numWeeksAnonymousDelegationMsg"/></label>
            <input type="number" id="numWeeksAnonymousDelegation" min="1" value="" max="52" required
                   style="width:120px;margin:10px 20px 0px 7px;"
                   title="<g:message code="numWeeksAnonymousDelegationMsg"/>"
                   placeholder="<g:message code="numWeeksAnonymousDelegationMsg"/>"
                   oninvalid="this.setCustomValidity('<g:message code="numberFieldLbl"/>')"
                   onchange="this.setCustomValidity('')">
        </p>
        <button id="representativeDateRangeFormDummyButton" style="display: none;"></button>
    </form>
  	</div> 
<r:script>

var callerCallback

function showAnonymousRepresentativeDateRangeDialog(callback) {
	$("#anonymousRepresentativeDateRangeDialog").dialog("open");
	$("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").hide()
	if(callback != null) callerCallback = callback
}

 $("#anonymousRepresentativeDateRangeDialog").dialog({
    width: 'auto', autoOpen: false, modal: true,
      buttons: [{text:"<g:message code="acceptLbl"/>",
                        icons: { primary: "ui-icon-check"},
                        click:function() {$("#representativeDateRangeFormDummyButton").click();} },{
                text:"<g:message code="cancelLbl"/>",
                        icons: { primary: "ui-icon-closethick"},
                        click:function() { $(this).dialog("close"); }}],
      show: {effect: "fade",duration: 300},
      hide: {effect: "fade",duration: 300},
      open: function( event, ui ) { $("#numWeeksAnonymousDelegation").val("")}
});

function submitRepresentativeDateRangeForm(form) {
    weeksAnonymousDelegation = $('#numWeeksAnonymousDelegation').val()
    if (isNaN(Number(weeksAnonymousDelegation))) {
        showRepresentativeDateRangeErrorMsg('<g:message code="numberFieldLbl"/>')
        return false
    }
    if(weeksAnonymousDelegation > 52) {
        var msgTemplate = "<g:message code="maxValueErrorMsg"/>"
        showRepresentativeDateRangeErrorMsg(msgTemplate.format(52))
        return false
    }
    $("#anonymousRepresentativeDateRangeDialog").dialog("close");
    return false
}

function showRepresentativeDateRangeErrorMsg(errorMsg) {
    $("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
    $("#anonymousRepresentativeDateRangeDialog .errorMsgWrapper").fadeIn()
}
</r:script>