<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class=""><a href="${createLink(controller: 'groupVS', action: 'index')}"><g:message code="groupvsLbl"/></a></li>
            <li class="active"><g:message code="groupvsAdminLbl"/></li>
        </ol>
    </div>
        <div class="row">
            <a href="${createLink(controller:'groupVS', action:'newGroup')}"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="newGroupVSLbl"/></a>
        </div>
        <div class="row">
            <a href="#" id="removeGroupVSButton"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="editGroupVSLbl"/></a>
        </div>
        <div class="row">
            <a href="#" id="editGroupVSButton"
               class="btn btn-default" role="button" style="margin:10px 20px 0px 0px; width:400px;">
                <g:message code="closeGroupVSLbl"/></a>
        </div>
</div>

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