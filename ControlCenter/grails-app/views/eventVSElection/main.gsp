<html>
<head>
       <meta name="layout" content="main" />
      	<r:require module="paginate"/>
</head>
<body>

<div id="contentDiv" style="display:none;">
	<div style="position:relative; height: 30px;">
		<div style="position:absolute;width: 50%;  margin: auto; left: 0; right: 0;">
			<select id="eventsStateSelect" style="margin:0px 0px 0px 40px;color:black;">
				<option value="" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
			  	<option value="ACTIVE" style="color:#6bad74;"> - <g:message code="selectOpenPollsLbl"/> - </option>
			  	<option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
			  	<option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
			</select>
		</div>
	</div>
	
	<g:render template="/template/eventsSearchInfo"/>
		
	<div id="mainPageEventList" class="mainPageEventList"><ul></ul></div>
	
	<div id="progressDiv" style="vertical-align: middle;height:100%;">
		<progress style="display:block;margin:0px auto 20px auto;"></progress>
	</div>
</div>

<g:render template="/template/pagination"/>	

	<div id="eventTemplate" style="display:none;">
		<g:render template="/template/event" model="[isTemplate:'true']"/>
	</div>

</body>
</html>
<r:script>
    		var eventState = ''
            var searchQuery
		 	$(function() {
		 		paginate(0)
		 		$('#eventsStateSelect').on('change', function (e) {
		 			eventState = $(this).val()
		 		    var optionSelected = $("option:selected", this);
		 		    console.log(" - eventState: " + eventState)
		 		    if(!isFirefox()) {
			 		    if($('#eventsStateSelect')[0].selectedIndex == 0) {
			 		    	$('#eventsStateSelect').css({'color': '#434343',
				 		    							 'border-color': '#cccccc'})
				 		} else {
			 		    	$('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
    							 'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
					 	}
			 		}
					var targetURL = "${createLink( controller:'eventVSElection')}"
					if("" != eventState) targetURL = targetURL + "?eventVSState=" + $(this).val()
		 		    loadEvents(targetURL)
		 		});
		 		$("#searchFormDiv").fadeIn()
			 });

			function loadEvents(eventsURL, data) {
				console.log("--- loadEvents - eventsURL: " + eventsURL);
				var requestType = 'GET'
				if(data != null) requestType = 'POST'
				var $loadingPanel = $('#progressDiv')
				var $contentDiv = $('#contentDiv')
				$contentDiv.css("display", "none")
				$('#mainPageEventList ul').empty()
				$loadingPanel.fadeIn(100)
				$.ajax({url: eventsURL, type:requestType, contentType:'application/json',
				        data: JSON.stringify(data)}).done(function(jsonResult) {
					console.log(" - ajax call done - printEvents");
					printEvents(jsonResult)
				}).error(function() {
					console.log("- ajax error - ");
					showResultDialog('<g:message code="errorLbl"/>', '<g:message code="connectionERRORMsg"/>')
					$loadingPanel.fadeOut(100)
				});
			}

            var eventTemplate = $('#eventTemplate').html()

			function printEvents(eventsJSON) {
				$.each(eventsJSON.eventsVS.elections, function() {
                    var eventVS = new EventVS(this, eventTemplate, "VOTES")
				    $("#mainPageEventList ul").append(eventVS.getElement())
				});
				printPaginate(eventsJSON.offset, eventsJSON.numEventsVSElectionInSystem, numMaxEventsForPage)
				$('#contentDiv').fadeIn(500)
				$('#progressDiv').fadeOut(500)
			}


			function paginate (newOffsetPage) {
				console.log(" - paginate - offsetPage : " + offsetPage + " - newOffsetPage: " + newOffsetPage)
				if(newOffsetPage == offsetPage) return
				offsetPage = newOffsetPage
				var offsetItem
				if(newOffsetPage == 0) offsetItem = 0
				else offsetItem = (newOffsetPage -1) * numMaxEventsForPage
				var targetURL = "${createLink( controller:'eventVSElection')}?max=" + numMaxEventsForPage + "&offset=" + offsetItem
				if(searchQuery != null) targetURL = "${createLink( controller:'search', action:'find')}?max=" +
						numMaxEventsForPage + "&offset=" + offsetItem
				loadEvents(targetURL, searchQuery)	
			}

			function getSearchResult(newSearchQuery) {
				newSearchQuery.eventState = eventState
				newSearchQuery.subsystem = "${selectedSubsystem}"
				searchQuery = newSearchQuery
				showEventsSearchInfoMsg(newSearchQuery)
				loadEvents("${createLink(controller:'search', action:'find')}?max=" +
						numMaxEventsForPage + "&offset=0", newSearchQuery)
			}
</r:script>