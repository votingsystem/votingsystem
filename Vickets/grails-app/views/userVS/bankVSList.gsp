<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <style>
        .nifColumn {width:50px; margin: 10px auto;}
        .IBANColumn {width: 220px; margin: 10px auto;}
        .nameColumn {width: 170px; margin: 10px auto;}
        .stateColumn {width: 40px; margin: 10px auto;}
        .row {margin: 0px  auto;text-align: center; cursor: pointer;}
        .descriptionColumn {width: 350px; text-overflow: ellipsis;margin: 10px auto;}
    </style>
</head>
<body>
<div class="pageContentDiv">
    <template id="bankVSList" is="auto-binding" bind="{{bankVSMap}}">
        <votingsystem-innerpage-signal title="<g:message code="bankVSListLbl"/>"></votingsystem-innerpage-signal>
        <template repeat="{{bankVS in bankVSMap.bankVSList}}">
            <div horizontal layout class="row" onclick="bankVSSelected('{{bankVS.id}}')">
                <div class="nifColumn">{{bankVS.nif}}</div>
                <div class="IBANColumn">{{bankVS.IBAN}}</div>
                <div class="nameColumn">{{bankVS.name}}</div>
                <div class="stateColumn">{{bankVS.state}}</div>
                <div class="descriptionColumn">{{bankVS.description}}</div>
            </div>
        </template>
    </template>
</div>
</body>
</html>
<asset:script>
    <g:applyCodec encodeAs="none">
        var bankVSMap = ${bankVSMap as grails.converters.JSON}
    </g:applyCodec>

    function bankVSSelected(bankVSId) {
        loadURL_VS("${createLink( controller:'userVS', action:" ", absolute:true)}/" + bankVSId)
    }

    document.addEventListener('innerPageSignal', function() {
        document.querySelector('#bankVSList').bankVSMap = bankVSMap
    });
</asset:script>
<asset:deferredScripts/>