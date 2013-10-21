<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
       	<meta name="layout" content="main" />
   		<link rel="stylesheet" href="${resource(dir:'css',file:'jqueryPaginate.css')}">
        <script src="${resource(dir:'js',file:'jquery.paginate.js')}"></script>
       	<script type="text/javascript">
	 	$(function() {			
		 	
	 		loadRepresentatives("${createLink( controller:'representative')}")	

	 		$('#checkRepresentativeButton').click(function() {
	 			$("#userNifText").val("");
		 		$("#checkRepresentativeDialogFormDiv").show()
				$("#checkRepresentativeDialogProgressDiv").hide()
				$("#checkRepresentativeDialogResultDiv").hide()
				$("#acceptButton").show();
		 		$("#acceptButton").button("enable");
		 		$("#cancelButton").button("enable");
	 			$("#cancelButton").find(".ui-button-text").text("<g:message code="cancelLbl"/>")
	 			$("#checkRepresentativeDialog").dialog("open");
			})


   		   $("#checkRepresentativeDialog").dialog({
   			   	  width: 500, autoOpen: false, modal: true,
   			      buttons: [{id: "acceptButton",
   			        		text:"<g:message code="acceptLbl"/>",
   			               	icons: { primary: "ui-icon-check"},
   			             	click:function() {
   			             		$("#submitNifCheck").click() 
   			             		//$(this).dialog( "close" );   	   			   				
   	   			        	}}, {	
  	   			        	id: "cancelButton",
   			        		text:"<g:message code="cancelLbl"/>",
   			               	icons: { primary: "ui-icon-closethick"},
   			             	click:function() {
  	   			   					$(this).dialog( "close" );
  	   			       	 		}	
   			           }],
   			      show: {effect:"fade", duration: 300},
   			      hide: {effect: "fade",duration: 300}
   			    });

	 		document.getElementById('userNifText').addEventListener('change', nifValidation, false);
		    $('#checkRepresentativeForm').submit(function(event){	
		 		console.log("checkRepresentativeForm")
		 		event.preventDefault();
		 		$("#acceptButton").button("disable");
		 		$("#cancelButton").button("disable");
		 		checkRepresentative();
			 })
		 });

	 	function checkRepresentative () { 
	 		$("#checkRepresentativeDialogFormDiv").hide()
	 		$("#checkRepresentativeDialogProgressDiv").fadeIn(500)
	 		var urlRequest = "${createLink(controller:'user')}/" + $("#userNifText").val() + "/representative"
	 		console.log(" - checkRepresentative - urlRequest: " + urlRequest)
			$.ajax({///user/$nif/representative
				url: urlRequest
				//data: data,
			}).done(function(resultMsg) {
				//var resultMsgStr = JSON.stringify(resultMsg);  
				//console.log(" - ajax call done - resultMsgStr: " + resultMsgStr);
				showCheckRepresentativeResult (StatusCode.SC_OK, resultMsg.responseText)
			}).error(function(resultMsg) {
				showCheckRepresentativeResult (StatusCode.SC_ERROR, resultMsg.responseText)
			});
		}

	 	function showCheckRepresentativeResult (statusCode, msg) { 
	 		console.log("showCheckRepresentativeResult - statusCode: " + statusCode)
	 		$("#checkRepresentativeDialogProgressDiv").hide()
	 		$("#checkRepresentativeDialogResultDiv").fadeIn(500)
	 		$("#acceptButton").fadeOut();
	 		$("#cancelButton").find(".ui-button-text").text("<g:message code="acceptLbl"/>")
	 		$("#cancelButton").button("enable");
	 		if(StatusCode.SC_OK == statusCode) {
	 			$("#checkRepresentativeDialogResultImage").attr('src','../images/accept_48x48.png');
		 	} else {
		 		$("#checkRepresentativeDialogResultImage").attr('src','../images/advert_64x64.png');
			}
	 		$("#checkRepresentativeDialogResultMsg").text(msg)
		}
	 	
	 	var nifValidation = function () {
			var nifInput = document.getElementById('userNifText')
			var validationResult = validateNIF(nifInput.value)
			console.log("validateNIF result: " + validationResult)
			if (!validationResult) {
				document.getElementById('userNifText').setCustomValidity("<g:message code='nifERRORMsg'/>");
			}
		}

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
				printPaginate(jsonResult.offset, jsonResult.representativesTotalNumber, numMaxItemsForPage)
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
	        var newRepresentativeTemplate = "${votingSystem.representative(isTemplate:true)}"
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

	<div style="width:100%;position:relative;display:block;">
		<div style="right:50%;">
			<div style="width:400px; margin:20px auto 20px auto;" id="paginationDiv" ></div>
		</div>
	</div>   	

<div id="checkRepresentativeDialog" title="<g:message code="checkRepresentativeLbl"/>">	
	<div id="checkRepresentativeDialogFormDiv" >
		<p style="text-align: center;"><g:message code="checkRepresentativeMsg"/></p>
		<div style="width: 80%; margin: 0 auto;">
			<form id="checkRepresentativeForm">
		    	<label for="userNifText" style="margin:0px 0px 20px 0px"><g:message code="enterNIFMsg"/></label>
				<input type="text" id="userNifText" style="width:350px; margin:0px auto 0px auto;" required
					oninvalid="this.setCustomValidity('<g:message code="nifERRORMsg"/>')"
		   			onchange="this.setCustomValidity('')"/>
				<input id="submitNifCheck" type="submit" style="display:none;">
			</form>
		</div>
	</div>  	
	<div id="checkRepresentativeDialogProgressDiv" style="display:none;">
		<p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
		<progress style='display:block;margin:0px auto 10px auto;'></progress>
	</div>
	<div id="checkRepresentativeDialogResultDiv" style="display:none;">
		<div style='display:table; width:100%;'>
			<div style='display:table-cell; vertical-align:middle;'><img id="checkRepresentativeDialogResultImage" src='' style='margin:3px 0 0 10px;'></img></div>
			<div style='display:table-cell;width:15px;'></div>
			<div style='display:table-cell; vertical-align:middle;'>
				<p id="checkRepresentativeDialogResultMsg" style="margin: 0px 0px 0px 0px; text-align:center;"></p>
			</div>
		</div>
	</div>
</div> 

</body>
</html>