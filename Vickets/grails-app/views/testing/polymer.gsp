<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/vicketTagVS/tagvs-select-dialog']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/paper-dropdown-menu', file: 'paper-dropdown-menu.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <style shim-shadowdom>



    body /deep/ paper-dropdown-menu.narrow {
                    max-width: 100px;
                }
    </style>
</head>
<body>

    <button onclick="document.querySelector('#tagDialog').show()" style="margin:10px;">Tag dialog</button>

    <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                    serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>


    <div layout horizontal>
        <paper-dropdown-menu  valueattr="label" label="Favorite pastry" selected="Donut">
            <paper-item label="Croissant"></paper-item>
            <paper-item label="Donut"></paper-item>
            <paper-item label="Financier"></paper-item>
            <paper-item label="Madeleine"></paper-item>
        </paper-dropdown-menu>
    </div>


</body>
</html>
<asset:script>
    var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, this.operation)
    webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:" ", absolute:true)}"
    webAppMessage.signedMessageSubject = "<g:message code='transactionvsFromGroupMsgSubject'/>"
    webAppMessage.signedContent = {operation:"TEST"}

    webAppMessage.setCallback(function () {
            console.log("====#### callback")
    })
    window[webAppMessage.objectId]()
</asset:script>
<asset:deferredScripts/>