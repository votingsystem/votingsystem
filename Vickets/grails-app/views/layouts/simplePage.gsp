<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votingsystem-message-dialog.gsp']"/>">
    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body style="margin:0px auto 0px auto; max-width: 1200px;">
<g:layoutBody/>

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