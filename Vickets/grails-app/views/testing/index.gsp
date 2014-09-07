<!DOCTYPE html>
<html>
<head>
    <title>TestingTemplate</title>
    <style>
    textarea { }
    input[id="subject"] { }
    </style>
</head>
<body>

<div layout horizontal center center-justified style="">

    <g:datePicker name="myDate" value="${new Date()}"
                  precision="day" years="${1930..1970}"/>


    <g:createLink  controller="polymer" params="[element: '/groupVS/groupvs-list']"/>

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


</asset:script>
<asset:deferredScripts/>