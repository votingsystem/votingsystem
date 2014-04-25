<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
	<div id="contentDiv" style="position:relative; height:700px;">

        <div class="row">
            <a href="${createLink(controller:'representative', action:'newRepresentative')}"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="newRepresentativeLbl"/></a>
        </div>
        <div class="row">
            <a href="#" id="removeRepresentativeButton"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="removeRepresentativeLbl"/></a>
        </div>
        <div class="row">
            <a href="#" id="editRepresentativeButton"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="editRepresentativeLbl"/></a>
        </div>
	</div>

<g:include view="/include/dialog/removeRepresentativeDialog.gsp"/>
<g:include view="/include/dialog/editRepresentativeDialog.gsp"/>

</body>
</html>
<r:script>
	$(function() {
		$("#editRepresentativeButton").click(function() {		 	
			$("#editRepresentativeDialog").dialog("open");
		 })
		 		  
		$("#removeRepresentativeButton").click(function () { 
			$("#removeRepresentativeDialog").dialog("open");
		});
	});
</r:script>