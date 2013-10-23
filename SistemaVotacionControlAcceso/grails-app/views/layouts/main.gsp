<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<title>${message(code: 'nombreServidorLabel', null)}</title>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
	<meta name="viewport" content="width=device-width" />
	<meta name="HandheldFriendly" content="true" />
	<g:include controller="app" action="jsUtils" />
	<g:layoutHead />
	<script type="text/javascript">
		$(function() {
			updateSubsystem("${selectedSubsystem}")
			$(".footer").fadeIn(3000)
			
			$("#advancedSearchLink").click(function () { 
				$("#advancedSearchDialog").dialog("open");
			});
			
			$('#advancedSearchForm').submit(function(event){
				console.log("advancedSearchForm")
				event.preventDefault();
			});
		})
	</script>
</head>
    <body>
		<div class="header">
		   	<div class="col-subsystem">
		   		<a id="subsystem_0_0_Link"></a>
		   		<a id="subsystem_0_1_Link"></a>
		   		<a id="subsystem_0_2_Link"></a>
		   	</div>
		   	<div id="selectedSubsystemDiv" class="col-selectedSystem">
		        <a id="selectedSubsystemLink"></a>
		   	</div>
		   	<div class="col-advancedSearch">
				<form>
				  	<input name="q" placeholder="<g:message code="searchLbl"/>" style="width:120px;">
					<div id="advancedSearchLink" class="appLink" style="display:inline;font-weight:bold;">
						<g:message code="advancedSearchLabel"/>
					</div>
				</form>
		   	</div>
		</div>

		<g:layoutBody/>

		<div class="footer" style="display:none;width:100%;">
			<a class="appLink" href="mailto:${grailsApplication.config.SistemaVotacion.emailAdmin}"
				 style="">${message(code: 'emailLabel', null)}</a>
			<a class="appLink" href="${createLink(controller: 'infoServidor', action: 'informacion')}"
				style="margin: 3px 0 0 20px; float:right;">
				<g:message code="dataInfoLinkText"/>
			</a>
		</div>
		
	<g:include controller="gsp" action="index" params="[pageName:'advancedSearchDialog']"/> 
</body>
</html>