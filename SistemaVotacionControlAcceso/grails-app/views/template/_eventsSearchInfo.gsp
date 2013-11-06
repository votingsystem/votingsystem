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
 
function showEventsSearchInfoMsg(searchQuery) {
	var searchResultMsg = searchResultMsgTemplate.format(searchQuery.textQuery)
	$(".eventSearchInfo .eventSearchText").html(searchResultMsg)
	var dateBeginFrom = searchQuery.dateBeginFrom?searchQuery.dateBeginFrom:''
	var dateBeginTo = searchQuery.dateBeginTo?searchQuery.dateBeginTo:''
	if('' == dateBeginFrom && '' == dateBeginTo) {
		$(".eventSearchInfo .eventSearchDateBegin").hide()
	} else {
		var beginSearchMsg = beginSearchMsgTemplate.format(dateBeginFrom, dateBeginTo)
		$(".eventSearchInfo .eventSearchDateBegin").html(beginSearchMsg)
	}
	var dateFinishFrom = searchQuery.dateFinishFrom?searchQuery.dateFinishFrom:''
	var dateFinishTo = searchQuery.dateFinishTo?searchQuery.dateFinishTo:''
	if('' == dateFinishFrom && '' == dateFinishTo) {
		$(".eventSearchInfo .eventSearchDateFinish").hide()
	} else {
		var endSearchMsg = endSearchMsgTemplate.format(dateFinishTo, dateFinishTo)
		$(".eventSearchInfo .eventSearchDateFinish").html(endSearchMsg)
	}
	$(".eventSearchInfo").fadeIn()
}
</r:script>