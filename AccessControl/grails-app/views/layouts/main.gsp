<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="serverNameLabel"/></title>
    <r:external uri="/images/AccessControl.ico"/>
	<r:require module="application"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
    <body>
		<div class="header"  style="display:none;">
		   	<div class="col-subsystem">
		   		<a id="subsystem_0_0_Link"></a>
		   		<a id="subsystem_0_1_Link"></a>
		   		<a id="subsystem_0_2_Link"></a>
		   	</div>
		   	<div id="selectedSubsystemDiv" class="col-selectedSystem">
		        <a id="selectedSubsystemLink"></a>
		   	</div>
		   	<div class="col-advancedSearch">
		   		<div id="searchFormDiv" style="display:none;">
				<form id="searchForm">
				  	<input id="searchText" name="q"  required 
				  		placeholder="<g:message code="searchLbl"/>" 
				  		title="<g:message code="searchLbl"/>" style="width:120px;">
					<div id="advancedSearchLink" class="appLink" style="display:inline;font-weight:bold;font-size: 0.8em">
						<g:message code="advancedSearchLabel"/>
					</div>
				</form>
				</div>
		   	</div>
		</div>

        <div style="min-height: 600px;"><g:layoutBody/></div>
	
		<div class="footer" style="display:none;">
			<a class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
				 style="">${message(code: 'emailLabel', null)}</a>
			<a class="appLink" href="${createLink(controller: 'serverInfo', action: 'info')}"
				style="margin: 3px 0 0 20px; float:right;">
				<g:message code="dataInfoLinkText"/>
			</a>
		</div>
		
	

	<div id="appletsFrame"  style="width:0px; height:0px;">
		<iframe id="votingSystemAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
	</div>	
</body>

<g:include view="/include/dialog/advancedSearchDialog.gsp"/>	
<g:include view="/include/dialog/loadingAppletDialog.gsp"/>
<g:include view="/include/dialog/workingWithAppletDialog.gsp"/>
<g:include view="/include/dialog/browserWithoutJavaDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>

</html>
<r:script>
	$(function() {
		updateSubsystem("${selectedSubsystem}")
		$(".header").fadeIn(3000)
		$(".footer").fadeIn(3000)
		
		$("#advancedSearchLink").click(function () { 
			$("#advancedSearchDialog").dialog("open");
		});

		 $('#searchForm').submit(function(event){
		 	console.log("searchForm")
		 	event.preventDefault();
		 	var searchQuery = {textQuery:$("#searchText").val()}
		 	getSearchResult(searchQuery)
		 });

	})
	
	
	function setMessageFromSignatureClient(appMessage) {
		var appMessageJSON = toJSON(appMessage)
		if(appMessageJSON != null) {
			if(ResponseVS.SC_PROCESSING == appMessageJSON.statusCode){
				signatureClientToolLoaded = true;
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			}
		}
	}
	
</r:script>
<r:layoutResources/>