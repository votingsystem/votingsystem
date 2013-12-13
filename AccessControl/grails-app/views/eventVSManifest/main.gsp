<html>
<head>
        <meta name="layout" content="main" />
       	<r:require modules="paginate"/>
</head>
<body>
<div class="mainPage">
    <div id="contentDiv" style="display:none;">
        <div style="display:inline-block;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
            <div style="display:inline;float:left;width:30%;">
                <div><votingSystem:feed href="${createLink(controller:'subscriptionVS', action:'manifests')}">
                    <g:message code="subscribeToFeedsLbl"/></votingSystem:feed></div>
            </div>
            <div style="display:inline;float:left;margin:0px auto 0px auto;">
                <div style="margin:0px auto 0px auto;">
                    <select id="eventsStateSelect" style="margin:0px 0px 0px 40px;color:black;">
                        <option value="" style="color:black;"> - <g:message code="selectManifestsLbl"/> - </option>
                        <option value="ACTIVE" style="color:#6bad74;"> - <g:message code="selectOpenManifestsLbl"/> - </option>
                        <option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingManifestsLbl"/> - </option>
                        <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedManifestsLbl"/> - </option>
                    </select>
                </div>
            </div>
            <div style="display:inline;float:right;">
                <votingSystem:simpleButton href="${createLink(controller:'editor', action:'manifest')}"
                        style="margin:0px 0px 0px 15px;"><g:message code="publishDocumentLbl"/>
                </votingSystem:simpleButton>
            </div>
        </div>

    </div>

    <g:render template="/template/eventsSearchInfo"/>

    <div id="progressDiv" style="position: absolute; left: 40%; right:40%; top: 300px;">
        <progress style="margin:0px auto 0px auto;"></progress>
    </div>

    <div id="mainPageEventList" class="mainPageEventList"><ul></ul></div>

    <div style="width:100%;position:absolute;display:block; margin:auto; bottom:20px;">
        <div style="width:500px; margin:20px auto 20px auto;" id="paginationDiv" ></div>
    </div>

    <g:render template="/template/pagination"/>

    <div id="eventTemplate" style="display:none;">
        <g:render template="/template/event" model="[isTemplate:'true']"/>
    </div>

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
		 		    if(!isFirefox()) {
			 		    if($('#eventsStateSelect')[0].selectedIndex == 0) {
			 		    	$('#eventsStateSelect').css({'color': '#434343',
				 		    							 'border-color': '#cccccc'})
				 		} else {
			 		    	$('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
    							 'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
					 	}
			 		}
					var targetURL = "${createLink( controller:'eventVSManifest')}?&max=" + numMaxEventsForPage;
					if("" != eventState) targetURL = targetURL + "&eventVSState=" + $(this).val()
                    $("#paginationDiv").hide()
		 		    loadEvents(targetURL)
		 		});
				$("#searchFormDiv").fadeIn()
			 });
	
			function loadEvents(eventsURL, data) {
				console.log("- loadEvents - eventsURL: " + eventsURL);
				var requestType = 'GET'
				if(data != null) requestType = 'POST'
				var $loadingPanel = $('#progressDiv')
				var $contentDiv = $('#contentDiv')
				$contentDiv.css("display", "none")
				$('#mainPageEventList ul').empty()
				$loadingPanel.fadeIn(100)
				$.ajax({
					url: eventsURL,
					type:requestType,
					contentType:'application/json',
					data: JSON.stringify(data),
				}).done(function(jsonResult) {
					console.log(" - ajax call done - printEvents");
					printEvents(jsonResult)
				}).error(function() {
					console.log("- ajax error - ");
					showResultDialog('<g:message code="errorLbl"/>',
						'<g:message code="connectionERRORMsg"/>') 
					$loadingPanel.fadeOut(100)
				});
			}

			var eventTemplate = $('#eventTemplate').html()

			function printEvents(eventsJSON) {
				$.each(eventsJSON.eventsVS.manifests, function() {
                    var eventVS = new EventVS(this, eventTemplate, "${selectedSubsystem}")
				    $("#mainPageEventList ul").append(eventVS.getElement())
				});
				printPaginate(eventsJSON.offset, eventsJSON.numEventsVSManifestInSystem, numMaxEventsForPage)
				$('#contentDiv').fadeIn(500)
				$('#progressDiv').fadeOut(500)
			}

			function paginate (newOffsetPage) {
				console.log(" - paginate - offsetPage : " + offsetPage + " - newOffsetPage: " + newOffsetPage +
				        " - eventState: " + eventState)
				if(newOffsetPage == offsetPage) return
				offsetPage = newOffsetPage
				var offsetItem
				if(newOffsetPage == 0) offsetItem = 0
				else offsetItem = (newOffsetPage -1) * numMaxEventsForPage
				var targetURL = "${createLink( controller:'eventVSManifest')}?max=" + numMaxEventsForPage +
				    "&offset=" + offsetItem + "&eventVSState=" + eventState
				if(searchQuery != null) targetURL = "${createLink( controller:'search', action:'find')}?max=" +
						numMaxEventsForPage + "&offset=" + offsetItem + "&eventVSState=" + eventState
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