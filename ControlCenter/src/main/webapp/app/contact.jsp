<!DOCTYPE html>
<html>
<head>

</head>
<body>
<vs-innerpage-signal caption="${msg.contactLbl}"></vs-innerpage-signal>
<div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto; text-align: center;">
    <a  href="mailto:${grailsApplication.config.vs.emailAdmin}"
        style="font-weight: bold;">${msg.emailLbl} <i class="fa fa-envelope-o"></i>
    </a>
</div>
</body>
</html>

