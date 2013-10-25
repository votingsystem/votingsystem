<li class='eventDiv'>
	<div class='eventSubjectDiv'>
		<p style='margin:0px 0px 0px 0px;text-align:center;'>${event?.isTemplate?'{0}':event.subject}</p></div>
	<div class='eventBodyDiv'>
		<div style='vertical-align: middle;'>
			<div class='eventAuthorDiv'>
				<div class='eventAuthorLblDiv'><g:message code='publishedByLbl'/>:</div>
				<div class='eventAuthorValueDiv'>${event?.isTemplate?'{1}':event.user}</div>
			</div>
			<div class='eventDateBeginDiv'>
				<div class='eventDateBeginLblDiv'><g:message code='dateBeginLbl'/>:</div>
				<div class='eventDateBeginValueDiv'>${event?.isTemplate?'{2}':event.dateInit}</div>
			</div>
			<div class='cancelMessage'><g:message code='eventCancelledLbl'/></div>
		</div>
	</div>
	<div class='eventDivFooter'>
		<div class='eventRemainingDiv'>${event?.isTemplate?'{3}':event.elapsedTime}</div>
		<div class='eventStateDiv'>${event?.isTemplate?'{4}':event.state}</div>
	</div>
</li>