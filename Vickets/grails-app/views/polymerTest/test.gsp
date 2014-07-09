<!DOCTYPE html>
<html>
<head>
    <asset:stylesheet src="polymer.css"/>
    <asset:stylesheet src="vickets.css"/>
    <asset:javascript src="utilsVS.js"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=no">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
</head>
<body style="width:500px; margin:0px auto 0px auto;">
    <g:include view="/include/dialog/depositDialog1.gsp"/>
    <votingsystem-deposit-dialog id="depositDialog" caption="Realizar ingreso"></votingsystem-deposit-dialog>
    <button onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER)">Deposit dialog</button>
</body>
</html>
<asset:script>

</asset:script>
<asset:deferredScripts/>