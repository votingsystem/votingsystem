<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<div class="" style="max-width:300px; margin:20px auto 0 auto;">
    <vs-innerpage-signal title="<g:message code="contactLbl"/>"></vs-innerpage-signal>
    <a  href="mailto:${grailsApplication.config.vs.emailAdmin}"
        style="font-weight: bold;"><g:message code="emailLbl"/> <i class="fa fa-envelope-o"></i>
    </a>
</div>
</body>
</html>
<asset:script>


</asset:script>

