<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
    <div class="row" style="">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li><a href="${createLink(controller: 'representative', action:'main')}"><g:message code="representativesPageLbl"/></a></li>
            <li class="active"><g:message code="removeRepresentativeLbl"/></li>
        </ol>
    </div>
	<div id="contentDiv" style="position:relative; height:700px;">
        <div class="row"></div>
	</div>

<g:include view="/include/dialog/removeRepresentativeDialog.gsp"/>

</body>
</html>
<r:script>
	$(function() {
        $("#removeRepresentativeDialog").dialog("open");
	});
</r:script>