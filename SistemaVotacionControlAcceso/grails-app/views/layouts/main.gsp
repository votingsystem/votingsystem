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
	 	   $("#dateFinish").datepicker(pickerOpts);
		   $(".footer").fadeIn(3000)
 		   $("#advancedSearchDialog").dialog({
	  			width: 'auto', autoOpen: false, modal: true,
   			      buttons: [{text:"<g:message code="acceptLbl"/>",
   	   			               	icons: { primary: "ui-icon-check"},
   	   			             	click:function() {
   	  	   	   			   				$("#submitSearch").click() 	   	   			   				
   	  	   	   			        	} },{text:"<g:message code="cancelLbl"/>",
   	   			               	icons: { primary: "ui-icon-closethick"},
   	   			             	click:function() {
   	  	   			   				$(this).dialog( "close" );
   	  	   			       	 	}}],
			      show: {effect: "fade",duration: 1000},
			      hide: {effect: "fade",duration: 1000}
			    });

		    $("#dateBeginFrom").datepicker(pickerOpts);
		    $("#dateBeginTo").datepicker(pickerOpts);
		    $("#dateFinishFrom").datepicker(pickerOpts);
		    $("#dateFinishTo").datepicker(pickerOpts);
		    
 		   
	   		$("#advancedSearchLink").click(function () { 
				$("#advancedSearchDialog").dialog("open");
			});

		    $('#advancedSearchForm').submit(function(event){
			    console.log("advancedSearchForm")
		        event.preventDefault();
		    });

    		$("#validationToolLink").click(function () { 
	    		if(!validationToolAppletLoaded) {
	    			console.log("Loading validationToolApplet");
	    			$("#validationToolAppletFrame").attr("src", "${createLink(controller: 'applet', action: 'herramientaValidacion')}");
	    			validationToolAppletLoaded = true;
		    	} else console.log("validationToolApplet already loaded");
    		      
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

			<div class="footer" style="display:none;">
				<a class="appLink" href="${createLink(controller: 'infoServidor', action: 'informacion')}"
					style="float:right;margin: 3px 0 0 20px;">
					<g:message code="dataInfoLinkText"/>
				</a>
			</div>
		
		
		<div id="advancedSearchDialog" title="<g:message code="advancedSearchLabel"/>" style="display:none;">
			<p style="text-align: center;"><g:message code="advancedSearchMsg"/>.</p>
    		<form id="advancedSearchForm">
				<div style="margin:0px auto 0px auto; width:50%">
    				<input type="text" id="searchText" style="" required
    					title="<g:message code="advancedSearchFieldLbl"/>"
    					placeholder="<g:message code="advancedSearchFieldLbl"/>"
	    				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	    				onchange="this.setCustomValidity('')" />
   				</div>
  
  				<div style="display:block;margin:20px 0px 0px 0px;">
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
						<input type="text" id="dateBeginFrom" style="width:230px;" required readonly
							title="<g:message code="dateBeginFromLbl"/>"
							placeholder="<g:message code="dateBeginFromLbl"/>"
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	  				</div>
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
						<input type="text" id="dateBeginTo" required readonly
							title="<g:message code="dateToLbl"/>"
							placeholder="<g:message code="dateToLbl"/>"
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	   				</div>
   				</div>
   				
  				<div style="display:block;margin:20px 0px 0px 0px;">
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
						<input type="text" id="dateFinishFrom" style="width:230px;" required readonly
							title="<g:message code="dateFinishFromLbl"/>"
							placeholder="<g:message code="dateFinishFromLbl"/>"
							oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	  				</div>
	
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
						<input type="text" id="dateFinishTo" required readonly
							title="<g:message code="dateToLbl"/>"
							placeholder="<g:message code="dateToLbl"/>"
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	   				</div>
   				</div>   				
   				<input id="submitSearch" type="submit" style="display:none;">
    		</form>
    	</div> 
    	
	<div id="appletsFrame">
		<iframe id="votingSystemAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
	</div>

    </body>
</html>