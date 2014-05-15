<html>
<head>
<meta name="layout" content="main" />
</head>
<body>
<div id="contentDiv" style="margin: 0px auto 0px auto; max-width: 1200px;">
    <div style="margin:0px 30px 0px 30px;">
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
    </div>
</div>

<g:include view="/include/dialog/removeRepresentativeDialog.gsp"/>

</body>
</html>
<r:script>
	$(function() {
        $("#removeRepresentativeDialog").modal("show");
	});
</r:script>