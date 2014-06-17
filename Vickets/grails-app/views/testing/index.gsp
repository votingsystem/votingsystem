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