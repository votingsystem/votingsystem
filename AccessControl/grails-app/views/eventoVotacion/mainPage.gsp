<html>
<head>
    <meta name="layout" content="main" />
   	<r:require module="paginate"/>
</head>
<body>

	<div id="contentDiv" style="display:none;">
	
		<div style="display:inline-block;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
			<div style="display:inline;float:left;width:300px;">
				<votingSystem:simpleButton href="${createLink(controller:'representative', action:'mainPage')}" 
					imgSrc="${resource(dir:'images',file:'group_24x24.png')}" style="margin:0px 0px 0px 15px;">
						<g:message code="representativesPageLbl"/>
				</votingSystem:simpleButton>
				<div>
					<img src="${resource(dir:'images',file:'feed.png')}" style="margin:3px 0 0 15px;"></img>
					<g:link controller="subscripcion" action="votaciones" style="font-size: 0.8em;"><g:message code="subscribeToFeedsLbl"/></g:link>
				</div>
			</div>
			<div style="display:inline;float:left;margin:0px auto 0px auto;">
				<div style="margin:0px auto 0px auto;">		
					<select id="eventsStateSelect" style="margin:0px 0px 0px 40px;color:black;">
						<option value="" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
					  	<option value="ACTIVO" style="color:#6bad74;"> - <g:message code="selectOpenPollsLbl"/> - </option>
					  	<option value="PENDIENTE_COMIENZO" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
					  	<option value="FINALIZADO" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
					</select>
				</div>
			</div>

			<div style="display:inline;float:right;">
				<votingSystem:simpleButton href="${createLink(controller:'editor', action:'vote')}" 
					imgSrc="${resource(dir:'images',file:'poll_22x22.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="publishDocumentLbl"/>
				</votingSystem:simpleButton>
			</div>
		</div>	
		
	</div>

	<g:render template="/template/eventsSearchInfo"/>
	
	<div id="mainPageEventList" class="mainPageEventList"><ul></ul></div>
		
	<div id="progressDiv" style="vertical-align: middle;height:100%;">
		<progress style="display:block;margin:0px auto 20px auto;"></progress>
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
					var targetURL = "${createLink( controller:'eventoVotacion')}"
					if("" != eventState) targetURL = targetURL + "?estadoEvento=" + $(this).val()
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

			function printEvents(eventsJSON) {
				$.each(eventsJSON.eventos.votaciones, function() {
					printEvent(this)
				});
				printPaginate(eventsJSON.offset, eventsJSON.numeroTotalEventosVotacionEnSistema, numMaxEventsForPage)
				$('#contentDiv').fadeIn(500)
				$('#progressDiv').fadeOut(500)
			}
			
			function printEvent(eventJSON) {
				//var dataStr = JSON.stringify(eventJSON);  
  			    //console.log( " - ajax call done - dataStr: " + dataStr);
				//console.log("printEvent: " + dataStr);
		        var newEventTemplate = $('#eventTemplate').html()
		        
		        var newEventHTML = newEventTemplate.format(eventJSON.asunto, 
				        eventJSON.usuario, eventJSON.fechaInicio, eventJSON.fechaFin.getElapsedTime(), 
				        getEstadoEventoMsg(eventJSON.estado));
		        var $newEvent = $(newEventHTML)
		        
				if(EstadoEvento.ACTIVO == eventJSON.estado) {
					$newEvent.css('border-color', '#6bad74')
					$newEvent.find(".eventSubjectDiv").css('background-color', '#6bad74')
					$newEvent.find(".eventStateDiv").css('color', '#6bad74')
				}
				if(EstadoEvento.FINALIZADO == eventJSON.estado) {
					$newEvent.css('border-color', '#cc1606')
					$newEvent.find(".eventSubjectDiv").css('background-color', '#cc1606')
					$newEvent.find(".eventStateDiv").css('color', '#cc1606')
				}
				if(EstadoEvento.CANCELADO == eventJSON.estado) {
					$newEvent.css('border-color', '#cc1606')
					$newEvent.find(".eventSubjectDiv").css('background-color', '#cc1606')
					$newEvent.find(".eventStateDiv").css('color', '#cc1606')
					$newEvent.find(".cancelMessage").fadeIn(100)
					
				}
				if(EstadoEvento.PENDIENTE_COMIENZO == eventJSON.estado) {
					$newEvent.css('border-color', '#fba131')
					$newEvent.find(".eventSubjectDiv").css('background-color', '#fba131')
					$newEvent.find(".eventStateDiv").css('color', '#fba131')
				}

				$newEvent.click(function() {
					console.log("- eventURL: " + "${createLink( controller:'eventoVotacion')}/" + eventJSON.id);
					window.location.href = "${createLink(controller:'eventoVotacion')}/" + eventJSON.id;
					//document.write("${createLink(controller:'eventoVotacion', absolute:true)}/" + eventJSON.id);
				});
 				$("#mainPageEventList ul").append($newEvent)
			}


			function paginate (newOffsetPage) {
				console.log(" - paginate - offsetPage : " + offsetPage + " - newOffsetPage: " + newOffsetPage)
				if(newOffsetPage == offsetPage) return
				offsetPage = newOffsetPage
				var offsetItem
				if(newOffsetPage == 0) offsetItem = 0
				else offsetItem = (newOffsetPage -1) * numMaxEventsForPage
				var targetURL = "${createLink( controller:'eventoVotacion')}?max=" + numMaxEventsForPage + "&offset=" + offsetItem
				if(searchQuery != null) targetURL = "${createLink( controller:'buscador', action:'consultaJSON')}?max=" + 
						numMaxEventsForPage + "&offset=" + offsetItem
				loadEvents(targetURL, searchQuery)	
			}

			function getSearchResult(newSearchQuery) {
				newSearchQuery.eventState = eventState
				newSearchQuery.subsystem = "${selectedSubsystem}"
				searchQuery = newSearchQuery
				showEventsSearchInfoMsg(newSearchQuery)
				loadEvents("${createLink(controller:'buscador', action:'consultaJSON')}?max=" + 
						numMaxEventsForPage + "&offset=0", newSearchQuery)
			}
</r:script>