<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
	<div id="contentDiv" style="position:relative; height:700px;">
	
		<div style="width: 50%;height: 50%;overflow: auto;margin: auto;top: 0; left: 0; bottom: 0; right: 0;">
			<votingSystem:simpleButton href="${createLink(controller:'representative', action:'newRepresentative')}"
				imgSrc="${resource(dir:'images',file:'newRepresentative.png')}" style="margin:10px 20px 0px 0px; width:400px;">
					<g:message code="newRepresentativeLbl"/>
			</votingSystem:simpleButton>
			<votingSystem:simpleButton id="removeRepresentativeButton" style="margin:10px 20px 0px 0px; width:400px;"
				imgSrc="${resource(dir:'images',file:'removeRepresentative.png')}">
				<g:message code="removeRepresentativeLbl"/>
			</votingSystem:simpleButton>
			<votingSystem:simpleButton  id="editRepresentativeButton" style="margin:10px 20px 0px 0px; width:400px;"
					imgSrc="${resource(dir:'images',file:'editRepresentative.png')}">
					<g:message code="editRepresentativeLbl"/>
			</votingSystem:simpleButton>
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