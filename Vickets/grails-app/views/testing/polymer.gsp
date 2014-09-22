<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/tagvs-select-dialog']"/>">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/uservs-search-dialog']"/>">
</head>
<body>

    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>
    <button onclick="document.querySelector('#searchDialog').show()" style="margin:10px;">User search dialog</button>

    <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                    serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>

    <uservs-search-dialog id="searchDialog"></uservs-search-dialog>

</body>
</html>
<asset:script>
    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, this.operation)
    webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:"deposit", absolute:true)}"
    webAppMessage.signedMessageSubject = "<g:message code='depositFromGroupMsgSubject'/>"
    webAppMessage.signedContent = {operation:"TEST"}

    webAppMessage.setCallback(function () {
            console.log("====#### callback")
    })
    window[webAppMessage.objectId]()
</asset:script>
<asset:deferredScripts/>