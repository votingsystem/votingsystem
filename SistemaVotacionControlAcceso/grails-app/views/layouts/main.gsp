<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <title>${message(code: 'nombreServidorLabel', null)}</title>
        <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
        <meta name="viewport" content="width=device-width" />
        <meta name="HandheldFriendly" content="true" />
        <link rel="stylesheet" href="${resource(dir:'css',file:'votingSystem.css')}">  
        <script src="${resource(dir:'js',file:'jquery-1.10.2.min.js')}"></script>  
        
        <script src="${resource(dir:'js',file:'jquery-ui-1.10.3.custom.min.js')}"></script>
        <link rel="stylesheet" href="${resource(dir:'css',file:'jquery-ui-1.10.3.custom.min.css')}">    
        <script src="${resource(dir:'app',file:'jsMessages')}"></script>
        <script src="${resource(dir:'js',file:'utils.js')}"></script>
   		<script src="${resource(dir:'js',file:'deployJava.js')}"></script>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
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
				<a id="validationToolLink" class="appLink" href="#" style="float:left;margin: 3px 20px 0 0;">
					<g:message code="validationToolLinkText"/>
				</a>
				<a class="appLink" href="${createLink(controller: 'infoServidor', action: 'informacion')}"
					style="float:right;margin: 3px 0 0 20px;">
					<g:message code="dataInfoLinkText"/>
				</a>
			</div>
		
		
		<div id="advancedSearchDialog" title="<g:message code="advancedSearchLabel"/>" style="display:none;">
			<p style="text-align: center;"><g:message code="advancedSearchMsg"/>.</p>
    		<form id="advancedSearchForm">
				<div style="margin:0px auto 0px auto; width:50%">
    				<label for="searchText"><g:message code="advancedSearchFieldLbl"/></label>
    				<input type="text" id="searchText" style="" required
	    				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
	    				onchange="this.setCustomValidity('')" />
   				</div>
  
  				<div style="display:block;margin:20px 0px 0px 0px;">
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
	  					<label for="dateBeginFrom" style="margin:0px 0px 20px 3px"><g:message code="dateBeginFromLbl"/></label>
						<input type="text" id="dateBeginFrom" style="width:230px;" required readonly
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	  				</div>
	
	
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
		   				<label for="dateBeginTo" style="margin:0px 0px 20px 3px;"><g:message code="dateToLbl"/></label>
						<input type="text" id="dateBeginTo" required readonly
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	   				</div>
   				</div>
   				
  				<div style="display:block;margin:20px 0px 0px 0px;">
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
						<label for="dateFinishFrom" style="margin:0px 0px 20px 3px"><g:message code="dateFinishFromLbl"/></label>
						<input type="text" id="dateFinishFrom" style="width:230px;" required readonly
							oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	  				</div>
	
	  				<div style="display:inline-block;margin:0px 0px 0px 20px;">
		   				<label for="dateFinishTo" style="margin:0px 0px 20px 3px"><g:message code="dateToLbl"/></label>
						<input type="text" id="dateFinishTo" required readonly
		   					oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
		   					onchange="this.setCustomValidity('')"/>
	   				</div>
   				</div>   				
   				<input id="submitSearch" type="submit" style="display:none;">
    		</form>
    	</div> 
    	
	<div id="appletsFrame" style="display:none;">
		<iframe id="validationToolAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
		<iframe id="votingSystemAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
	</div>

    </body>
</html>