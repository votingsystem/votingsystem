<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
</head>
<body style="width:1200px; margin:0px auto 0px auto; height: 1200px; padding:10px 10px 50px 10px;">
<div layout vertical>
    <div layout horizontal>
        <button onclick="document.querySelector('#depositDialog').show(Operation.VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS)" style="margin:10px;">Deposit dialog</button>
        <button onclick="document.querySelector('#tagDialog').show()"style="margin:10px;">Tag dialog</button>
        <button onclick="document.querySelector('#getReasonDialog').toggle()" style="margin:10px;">Get reason dialog</button>
        <button onclick="showMessageVS('msg msg msg ', 'caption')" style="margin:10px;">Message dialog</button>
    </div>
    <div layout vertical>
        <g:include view="/include/vicket-deposit-dialog.gsp"/>
        <div layout horizontal center center-justified style="">
            <vicket-deposit-dialog id="depositDialog" caption="Realizar ingreso"></vicket-deposit-dialog>
        </div>


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