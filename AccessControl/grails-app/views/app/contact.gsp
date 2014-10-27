<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal title="<g:message code="contactLbl"/>"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto; text-align: center;">
    <a  href="mailto:${grailsApplication.config.vs.emailAdmin}"
        style="font-weight: bold;"><g:message code="emailLbl"/> <i class="fa fa-envelope-o"></i>
    </a>
</div>
</body>
</html>
