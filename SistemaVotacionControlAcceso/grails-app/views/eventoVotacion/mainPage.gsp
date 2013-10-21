<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
        <meta name="layout" content="main" />
		<link rel="stylesheet" href="${resource(dir:'css',file:'jqueryPaginate.css')}">
        <script src="${resource(dir:'js',file:'jquery.paginate.js')}"></script>

        <script type="text/javascript">
		 	$(function() {
		 		loadEvents("${createLink( controller:'eventoVotacion')}")	
		 		
		 		$('#eventsStateSelect').on('change', function (e) {
		 		    var optionSelected = $("option:selected", this);
		 		    var valueSelected = this.value;
		 		    console.log(" - valueSelected: " + valueSelected)
		 		    if(!isFirefox()) {
			 		    if($('#eventsStateSelect')[0].selectedIndex == 0) {
			 		    	$('#eventsStateSelect').css({'color': '#434343',
				 		    							 'border-color': '#cccccc'})
				 		} else {
			 		    	$('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
    							 'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
					 	}
			 		}
		 		    loadEvents(valueSelected)
		 		});
			
			 });

			function loadEvents(eventsURL) {
				console.log("- loadEvents - eventsURL: " + eventsURL);
				var $loadingPanel = $('#progressDiv')
				var $contentDiv = $('#contentDiv')
				$contentDiv.css("display", "none")
				$('#mainPageEventList ul').empty()
				$loadingPanel.fadeIn(100)

				
				$.ajax({
					url: eventsURL,
					//data: data,
				}).done(function(jsonResult) {
					console.log(" - ajax call done - ");					
					$.each(jsonResult.eventos.votaciones, function() {
						printEvent(this)
						//var dataStr = JSON.stringify(this);  
		  			    //console.log( " - ajax call done - dataStr: " + dataStr);
						//console.log("votacion " + this);
					});
					printPaginate(jsonResult.offset, jsonResult.numeroTotalEventosVotacionEnSistema)
					$contentDiv.fadeIn(500)
					$loadingPanel.fadeOut(500)
				}).error(function() {
					console.log("- ajax error - ");
					showResultDialog('<g:message code="errorLbl"/>',
						'<g:message code="connectionERRORMsg"/>') 
					$loadingPanel.fadeOut(100)
				});
			}

			function printEvent(eventJSON) {
				//var dataStr = JSON.stringify(eventJSON);  
  			    //console.log( " - ajax call done - dataStr: " + dataStr);
				//console.log("printEvent: " + dataStr);
		        var newEventTemplate = "${votingSystem.event(isTemplate:true)}"
		        var endTime = Date.parse(eventJSON.fechaFin)
		        
		        var newEventHTML = newEventTemplate.format(eventJSON.asunto, 
				        eventJSON.usuario, eventJSON.fechaInicio, getElapsedTime(endTime), 
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

			function printPaginate (offset, numEvents) {
				console.log(" - paginate - offset:" + offset + " - numEvents: " + numEvents)
				var numPages = ( (numEvents -numEvents%numMaxEventsForPage)/numMaxEventsForPage) + 1
				var offsetPage = ( (offset -offset%numMaxEventsForPage)/numMaxEventsForPage) + 1
				console.log(" - paginate - numPages:" + numPages + " - offsetPage: " + offsetPage)
				$("#paginationDiv").paginate({
					count 		: numPages,
					start 		: offsetPage,
					display     : 8,
					border					: true,
					border_color			: '#09287e',
					text_color  			: '#09287e',
					background_color    	: '',	
					background_hover_color  : '#09287e',
					border_hover_color		: '#ccc',
					text_hover_color  		: '#fff',
					images					: false,
					mouse					: 'press', 
					onChange				: paginate
				});
				
			}
			
			function paginate (pageNumber) {
				console.log(" - paginate: " + pageNumber)
			}
        </script>
</head>
<body>



	<div id="contentDiv" style="display:none;">
	
		<div style="display:inline-block;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
			<div style="display:inline;float:left;width:30%;">
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
						<option value="${createLink( controller:'eventoVotacion')}" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
					  	<option value="${createLink( controller:'eventoVotacion')}?estadoEvento=ACTIVO" style="color:#6bad74;"> - <g:message code="selectOpenPollsLbl"/> - </option>
					  	<option value="${createLink( controller:'eventoVotacion')}?estadoEvento=PENDIENTE_COMIENZO" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
					  	<option value="${createLink( controller:'eventoVotacion')}?estadoEvento=FINALIZADO" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
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

	<div id="mainPageEventList" class="mainPageEventList"><ul></ul></div>
		
	<div id="progressDiv" style="vertical-align: middle;height:100%;">
		<progress style="display:block;margin:0px auto 20px auto;"></progress>
	</div>

	<div style="width:100%;position:relative;display:block;">
		<div style="right:50%;">
			<div style="width:400px; margin:20px auto 20px auto;" id="paginationDiv" ></div>
		</div>
	</div>

</body>
</html>