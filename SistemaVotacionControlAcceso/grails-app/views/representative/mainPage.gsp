<html>
<head>
       	<meta name="layout" content="main" />
		<g:render template="/template/js/jqueryPaginate"/>
       	<script type="text/javascript">
       	var numMaxRepresentativesForPage = 20
       	
	 	$(function() {		
	 		paginate(0)	

	 		$('#checkRepresentativeButton').click(function() {
	 			$("#checkRepresentativeDialog").dialog("open");
			})
		 });

		function loadRepresentatives(representativesURL) {
			console.log("- loadRepresentatives - representativesURL: " + representativesURL);
			var $loadingPanel = $('#progressDiv')
			var $contentDiv = $('#contentDiv')
			$contentDiv.css("display", "none")
			$('#representativeList').empty()
			$loadingPanel.fadeIn(100)
			$.ajax({
				url: representativesURL,
				//data: data,
			}).done(function(jsonResult) {
				console.log(" - ajax call done - ");
				$.each(jsonResult.representatives, function() {
					printRepresentative(this)
					//var dataStr = JSON.stringify(this);  
	  			    //console.log( " - ajax call done - dataStr: " + dataStr);
					//console.log("votacion " + this);
				});
				printPaginate(jsonResult.offset, jsonResult.representativesTotalNumber, numMaxRepresentativesForPage)
				$contentDiv.fadeIn(500)
				$loadingPanel.fadeOut(500)
			}).error(function() {
				console.log("- ajax error - ");
				showResultDialog('<g:message code="errorLbl"/>',
					'<g:message code="connectionERRORMsg"/>') 
				$loadingPanel.fadeOut(100)
			});
		}

		function printRepresentative(representativeJSON) {
			//var dataStr = JSON.stringify(representativeJSON);  
			//console.log( " - ajax call done - dataStr: " + dataStr);
			//console.log("printEvent: " + dataStr);
	        var newRepresentativeTemplate = "${render(template:'/template/representative').replace("\n","")}"
	        var endTime = Date.parse(representativeJSON.fechaFin)
	        
	        var newRepresentativeHTML = newRepresentativeTemplate.format(representativeJSON.imageURL, 
			        representativeJSON.nombre + " " + representativeJSON.primerApellido,
			        representativeJSON.numRepresentations);
	        var $newRepresentative = $(newRepresentativeHTML)
	        //$newRepresentative.attr("representativeData", dataStr)			
			$newRepresentative.click(function() {
				var targetURL = "${createLink( controller:'representative')}/" + representativeJSON.id
				console.log("- representativeURL: " + targetURL);
				window.location.href = targetURL;
			});
			$("#representativeList").append($newRepresentative)
		}

		function paginate (pageNumber) {
			console.log(" - paginate: " + pageNumber)
		}

		
		function paginate (newOffsetPage) {
			console.log(" - paginate - offsetPage : " + offsetPage + " - newOffsetPage: " + newOffsetPage)
			if(newOffsetPage == offsetPage) return
			offsetPage = newOffsetPage
			var offsetItem
			if(newOffsetPage == 0) offsetItem = 0
			else offsetItem = (newOffsetPage -1) * numMaxRepresentativesForPage
			loadRepresentatives("${createLink( controller:'representative')}?max=" + numMaxRepresentativesForPage + "&offset=" + offsetItem)	
		}
		
       </script>
</head>
<body>
<div id="contentDiv" style="display:block;position:relative; margin: 0px 0px 30px 0px;min-height: 700px;">
		<div style="width: 80%;margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
			<div style="display:table-cell;">
				<votingSystem:simpleButton id="checkRepresentativeButton"
					imgSrc="${resource(dir:'images',file:'checkRepresentative.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="checkRepresentativeLbl"/>
				</votingSystem:simpleButton>
			</div>

			<div style="display:table-cell;">
				<votingSystem:simpleButton href="${createLink(controller:'representative', action:'representativeAdmin')}"
					imgSrc="${resource(dir:'images',file:'representativeAdmin.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="adminRepresentativeLbl"/>
				</votingSystem:simpleButton>
			</div>
		</div>

		<div style="width:90%;margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
			<div style="display:table-cell;"><ul id="representativeList"></ul></div>
			
		</div>
	
</div>

		
<div id="progressDiv" style="vertical-align: middle;height:100%;">
	<progress style="display:block;margin:0px auto 20px auto;"></progress>
</div>

<g:render template="/template/pagination"/> 	

<g:render template="/template/dialog/checkRepresentativeDialog"/>	
</body>
</html>