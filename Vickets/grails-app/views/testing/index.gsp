<!DOCTYPE html>
<html>
<head>
    <title>TestingTemplate</title>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <style>
    textarea { }
    input[id="subject"] { }
    </style>
</head>
<body>

<div layout horizontal center center-justified style="">

    <g:datePicker name="myDate" value="${new Date()}"
                  precision="day" years="${1930..1970}"/>


    <g:createLink  controller="element" params="[element: '/groupVS/groupvs-list']"/>

    <div style="font-weight: bold;">{{messages.fromDate}}: </div>
    <div>
        <input id='fromDate' type="date" value="{{fromDateValue}}" on-change="{{validateForm}}" style="width: auto;">
    </div>
    <div>
        <input id='fromTime' type="time" value="{{fromTimeValue}}"  on-change="{{validateForm}}" style="width: auto; margin:0px 0px 0px 10px;">
    </div>
</div>

<div>
    <i style="color:#6c0404; font-size: 4em;" class="fa fa-cog fa-spin"></i>
</div>
</body>
</html>
<asset:script>

                getDateFormatted("2014/10/06 00:00:00","yyyy/MM/dd' 'HH:mm:ss", null,function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        notAfter = appMessageJSON.message
                        console.log("====== " + notAfter)
                    }
                })
</asset:script>
<asset:deferredScripts/>