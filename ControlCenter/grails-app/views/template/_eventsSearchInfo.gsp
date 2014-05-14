<div class="eventSearchInfo" style="">
	<div style="text-align:center;display:table-cell; width:100%;">
		<div class="eventSearchText"></div>
		<div class="eventSearchDateBegin"></div>
		<div class="eventSearchDateFinish"></div>
	</div>
</div>
<r:script>

var searchResultMsgTemplate = "<g:message code="searchResultMsg"/>"
var beginSearchMsgTemplate = "<g:message code="beginSearchInfoLbl"/>"
var endSearchMsgTemplate = "<g:message code="endSearchInfoLbl"/>"
 
function showEventsSearchInfoMsg(textQuery, dateBeginFrom, dateBeginTo, dateFinishFrom, dateFinishTo) {
	var searchResultMsg = searchResultMsgTemplate.format(textQuery)
	$(".eventSearchInfo .eventSearchText").html(searchResultMsg)
	if(FormUtils.checkIfEmpty(dateBeginFrom) && FormUtils.checkIfEmpty(dateBeginTo)) {
		$(".eventSearchInfo .eventSearchDateBegin").hide()
	} else {
		var beginSearchMsg = beginSearchMsgTemplate.format(dateBeginFrom, dateBeginTo)
		$(".eventSearchInfo .eventSearchDateBegin").html(beginSearchMsg)
	}
	if(FormUtils.checkIfEmpty(dateFinishFrom) && FormUtils.checkIfEmpty(dateFinishTo)) {
		$(".eventSearchInfo .eventSearchDateFinish").hide()
	} else {
		var endSearchMsg = endSearchMsgTemplate.format(dateFinishFrom, dateFinishTo)
		$(".eventSearchInfo .eventSearchDateFinish").html(endSearchMsg)
	}
	$(".eventSearchInfo").fadeIn()
}

function hideEventsSearchInfoMsg() {
    $(".eventSearchInfo").hide()
}
</r:script>