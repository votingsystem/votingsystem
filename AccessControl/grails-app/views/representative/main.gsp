<html>
<head>
       	<meta name="layout" content="main" />
       	<r:require modules="paginate"/>
</head>
<body>
<div style="position:relative;">
    <div id="progressDiv" style="position: absolute; left: 40%; right:40%; top: 300px;">
        <progress style="margin:0px auto 0px auto;"></progress>
    </div>

    <div id="contentDiv" style="display:block;position:relative; margin: 0px 0px 30px 0px;min-height: 700px;">
        <div style="display: table;  margin: auto;">
            <div style="display:table-cell;">
                <votingSystem:simpleButton id="checkRepresentativeButton" style="margin:0px 30px 0px 0px;">
                    <g:message code="checkRepresentativeLbl"/>
                </votingSystem:simpleButton>
            </div>

            <div style="display:table-cell;">
                <votingSystem:simpleButton href="${createLink(controller:'representative', action:'representativeAdmin')}"
                           style="margin:0px 20px 0px 30px;">
                    <g:message code="adminRepresentativeLbl"/>
                </votingSystem:simpleButton>
            </div>
        </div>

        <div style="width:90%;margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
            <div style="display:table-cell;"><ul id="representativeList"></ul></div>

        </div>
    </div>
</div>
<g:render template="/template/pagination"/> 	

<g:include view="/include/dialog/checkRepresentativeDialog.gsp"/>	

<template id="representativeTemplate" style="display:none;">
    <g:render template="/template/representative"/>
</template>
</body>
</html>
<r:script>
       	var numMaxRepresentativesForPage = 20
       	
	 	$(function() {		
	 		paginate(1)

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
				printPaginate(jsonResult.offset, jsonResult.numTotalRepresentatives, numMaxRepresentativesForPage)
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
	        var newRepresentativeTemplate = $('#representativeTemplate').html()
	        var endTime = Date.parse(representativeJSON.dateFinish)
	        
	        var newRepresentativeHTML = newRepresentativeTemplate.format(representativeJSON.imageURL, 
			        representativeJSON.name + " " + representativeJSON.firstName,
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
			if(newOffsetPage <= 1) offsetItem = 0
			else offsetItem = (newOffsetPage -1) * numMaxRepresentativesForPage
			loadRepresentatives("${createLink( controller:'representative')}?max=" + numMaxRepresentativesForPage + "&offset=" + offsetItem)	
		}
		
</r:script>