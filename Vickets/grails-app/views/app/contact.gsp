<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body>
<div class="" style="max-width:300px; margin:20px auto 0 auto;">
    <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
        style="font-weight: bold;"><g:message code="emailLbl"/> <i class="fa fa-envelope-o"></i>
    </a>
</div>
</body>
</html>
<asset:script>


</asset:script>

