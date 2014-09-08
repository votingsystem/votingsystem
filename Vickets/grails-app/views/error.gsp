<!DOCTYPE html>
<html>
<head>
    <title><g:if env="development">Grails Runtime Exception</g:if><g:else>Error</g:else></title>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <g:if env="development"></g:if>
</head>
<body>
    <div class="pageContentDiv" style="max-width: 1000px; padding:0px 30px 0px 30px;">
        <g:if env="development">
            <g:renderException exception="${exception}" />
        </g:if>
        <g:else>
            <ul class="errors">
                <li>An error has occurred</li>
            </ul>
        </g:else>
    </div>
</body>
</html>
