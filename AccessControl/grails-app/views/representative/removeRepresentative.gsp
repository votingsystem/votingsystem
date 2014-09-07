<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
<div id="contentDiv" style="margin: 0px auto 0px auto; max-width: 1200px;">
    <div style="margin:0px 30px 0px 30px;">
        <div style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'representative', action:'main')}"><g:message code="representativesPageLbl"/></a></li>
                <li class="active"><g:message code="removeRepresentativeLbl"/></li>
            </ol>
        </div>
        <div id="contentDiv" style="position:relative; height:700px;">
            <div></div>
        </div>
    </div>
</div>

<g:include view="/include/dialog/removeRepresentativeDialog.gsp"/>

</body>
</html>
<asset:script>
	$(function() {
        $("#removeRepresentativeDialog").modal("show");
	});
</asset:script>