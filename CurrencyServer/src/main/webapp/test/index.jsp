<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
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

<div>
    <i style="color:#6c0404; font-size: 4em;" class="fa fa-cog fa-spin"></i>
</div>
</body>
</html>
<script>

                getDateFormatted("2014/10/06 00:00:00","yyyy/MM/dd' 'HH:mm:ss", null,function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        notAfter = appMessageJSON.message
                        console.log("====== " + notAfter)
                    }
                })
</script>
