<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>   
   	<g:render template="/template/js/pcUtils"/>
	<g:layoutHead />
	<script type="text/javascript">
	        
	$(function() {		 	
		updateSubsystem("${selectedSubsystem}")
		$("#dateFinish").datepicker(pickerOpts);
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
			<img src="${resource(dir:'images',file:'feed.png')}" style="margin:3px 0 0 15px;"></img>
			<g:link controller="subscripcion" action="votaciones" style="font-size: 0.8em;"><g:message code="subscribeToFeedsLbl"/></g:link>
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
		
 	<g:render template="/template/dialog/advancedSearchDialog"/>			
</body>
</html>