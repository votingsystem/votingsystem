<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
	<div id="contentDiv" style="position:relative; height:700px;">
	
		<div style="display: table;  margin: auto;height: 100%;margin: auto;">
            <a href="${createLink(controller:'representative', action:'newRepresentative')}"
               class="btn btn-default row" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="newRepresentativeLbl"/></a>
            <a href="#" id="removeRepresentativeButton"
               class="btn btn-default row" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="removeRepresentativeLbl"/></a>
            <a href="#" id="editRepresentativeButton"
               class="btn btn-default row" role="button" style="margin:10px 20px 0px 0px; width:400px;">
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