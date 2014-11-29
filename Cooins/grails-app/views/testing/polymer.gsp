<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/tagVS/tagvs-select-dialog']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <style shim-shadowdom>
    </style>
</head>
<body>

    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>

    <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                    serviceURL="<g:createLink controller="tagVS" action="index" />"></tagvs-select-dialog>

</body>
</html>
<asset:script>
    var webAppMessage = new WebAppMessage( this.operation)
    webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:" ", absolute:true)}"
    webAppMessage.signedMessageSubject = "<g:message code='transactionvsFromGroupMsgSubject'/>"
    webAppMessage.signedContent = {operation:"TEST"}

    webAppMessage.setCallback(function () {
            console.log("====#### callback")
    })
    window[webAppMessage.objectId]()
</asset:script>
<asset:deferredScripts/>