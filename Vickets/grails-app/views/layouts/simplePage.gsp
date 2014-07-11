<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <g:layoutHead/>
</head>
<body id="vicketsPage" style="margin:0px auto 0px auto; max-width: 1200px;">
<g:layoutBody/>
<g:include view="/include/dialog/votingsystem-message-dialog.gsp"/>
<div layout horizontal center center-justified style="top:100px;">
    <votingsystem-message-dialog id="_votingsystemMessageDialog"></votingsystem-message-dialog>
</div>

</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        updateMenuLinks()
    });
</asset:script>
<asset:deferredScripts/>