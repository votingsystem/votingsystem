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
<body style="width:1200px; margin:0px auto 0px auto; height: 1200px; padding:10px 10px 50px 10px;">
<div layout vertical>
    <div layout horizontal>
        <button onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS)" style="margin:10px;">Deposit dialog</button>
        <button onclick="document.querySelector('#tagDialog').show()"style="margin:10px;">Tag dialog</button>
        <button onclick="document.querySelector('#getReasonDialog').toggle()" style="margin:10px;">Get reason dialog</button>
    </div>
    <div layout vertical>
        <g:include view="/include/dialog/votingsystem-deposit-dialog.gsp"/>
        <votingsystem-deposit-dialog id="depositDialog" caption="Realizar ingreso"></votingsystem-deposit-dialog>


        <votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                        serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>

        <g:include view="/include/dialog/get-reason-dialog.gsp"/>
        <get-reason-dialog id="getReasonDialog" opened="false" caption="<g:message code="cancelCertFormCaption"/>"
                           isForAdmins="true"></get-reason-dialog>
    </div>
</div>
</body>
</html>
<asset:script>

</asset:script>
<asset:deferredScripts/>