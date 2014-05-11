<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="pageContenDiv">
    <div class="row">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="vicketsToolPageTitle"/></li>
        </ol>
    </div>
    <div class="row" style="margin:30px 0px 0px 0px;">
        <a href="${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip" class=""
           style="margin:40px 20px 0px 0px; width:400px;">
            <g:message code="downloadClientToolAppLbl"/> <i class="fa fa-cogs"></i></a>
    </div>
</div>

</body>
</html>
<r:script>

    $(function() {

    })

</r:script>

