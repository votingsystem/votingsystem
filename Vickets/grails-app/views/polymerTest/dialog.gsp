<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-ripple', file: 'paper-ripple.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-advanced-search-dialog', file: 'votingsystem-advanced-search-dialog.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-deposit-dialog']"/>">




</head>
<body>
<div layout vertical style="width:1200px;height: 1200px;margin:0px auto; ">
    <div layout horizontal>
        <votingsystem-button onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS)" style="margin:10px;">Deposit dialog</votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#tagDialog').show()"style="margin:10px;">Tag dialog</votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#getReasonDialog').toggle()" style="margin:10px;">Get reason dialog</votingsystem-button>
        <votingsystem-button onclick="document.querySelector('#searchDialog').show()" style="margin:10px;">SEARCH dialog</votingsystem-button>
        <votingsystem-button onclick="showMessageVS('msg msg msg ', 'caption', null, false)" style="margin:10px;">
            Message dialog        <i class="fa fa-pencil-square-o"></i></votingsystem-button>
    </div>
    <div layout vertical>

        <paper-input id="depositSubject" floatinglabel
                     label="<g:message code="subjectLbl"/>" required></paper-input>

        <vicket-deposit-dialog id="depositDialog" caption="Realizar ingreso"></vicket-deposit-dialog>


        <votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                        serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>

        <votingsystem-advanced-search-dialog id="searchDialog"></votingsystem-advanced-search-dialog>
        <g:include view="/polymer/dialog/get-reason-dialog.gsp"/>
        <get-reason-dialog id="getReasonDialog" opened="false" caption="<g:message code="cancelCertFormCaption"/>"
                           isForAdmins="true"></get-reason-dialog>



    </div>

</div>
</body>
</html>
<asset:script>

    document.addEventListener('polymer-ready', function() {
    });

    function showEditor() {

    }
</asset:script>
<asset:deferredScripts/>