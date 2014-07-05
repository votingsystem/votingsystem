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
    <div style="font-weight: bold;">{{messages.fromDate}}: </div>
    <div>
        <input id='fromDate' type="date" value="{{fromDateValue}}" on-change="{{validateForm}}" style="width: auto;">
    </div>
    <div>
        <input id='fromTime' type="time" value="{{fromTimeValue}}"  on-change="{{validateForm}}" style="width: auto; margin:0px 0px 0px 10px;">
    </div>
</div>

<div class="form-group has-error">
    <i style="color:#6c0404; font-size: 4em;" class="fa fa-cog fa-spin"></i>
</div>
</body>
</html>
<asset:script>

    function submitForm(form) {
        var result = document.getElementById("testTime").getValidatedTime()
        console.log("result: " + result)
        return false
    }

    $(document).ready(function(){
        $('#testForm').submit(function(event){event.preventDefault();});
        $("#submitButton").click(function(){});
    });

</asset:script>