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
</head>
<body>
<div layout vertical style="width:1200px;height: 1200px;margin:0px auto; ">
    <div layout horizontal>
        <button onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS)" style="margin:10px;">Deposit dialog</button>
        <button onclick="document.querySelector('#tagDialog').show()"style="margin:10px;">Tag dialog</button>
        <button onclick="document.querySelector('#getReasonDialog').toggle()" style="margin:10px;">Get reason dialog</button>
        <button onclick="showMessageVS('msg msg msg ', 'caption', null, false)" style="margin:10px;">Message dialog</button>
    </div>
    <div layout vertical>

        <paper-input id="depositSubject" floatinglabel
                     label="<g:message code="subjectLbl"/>" required></paper-input>

        <g:include view="/polymer/dialog/vicket-deposit-dialog.gsp"/>
        <div layout horizontal center center-justified style="">
            <vicket-deposit-dialog id="depositDialog" caption="Realizar ingreso"></vicket-deposit-dialog>
        </div>


        <votingsystem-select-tag-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                                        serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></votingsystem-select-tag-dialog>

        <g:include view="/polymer/dialog/get-reason-dialog.gsp"/>
        <get-reason-dialog id="getReasonDialog" opened="false" caption="<g:message code="cancelCertFormCaption"/>"
                           isForAdmins="true"></get-reason-dialog>



    </div>

    <votingsystem-button>
        Testing <i class="fa fa-pencil-square-o"></i>
    </votingsystem-button>

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