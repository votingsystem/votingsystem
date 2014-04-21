<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
    <r:require module="multilevelmenu"/>
    <style>
	  	textarea { }
	  	input[id="subject"] { }
  	</style>
    <r:layoutResources />
</head>
<body>
    <form onsubmit="return submitForm(this);">
        <div class="" style='display: inline-block; margin:100px 0 0 300px;; '>
            <votingSystem:timePicker id="testTime" style="margin: 0 0 0 20px;"></votingSystem:timePicker>
        </div>
        <button class="btn" type="submit">Test</button>
    </form>

<div class="form-group has-error">
    <input type="time" id='' value="11:25" class="form-control" style='width: 160px; display: inline;'>

</div>
</body>
</html>
<r:script>

    function submitForm(form) {
        var result = document.getElementById("testTime").getValidatedTime()
        console.log("=========== result: " + result)
        return false
    }

    $(document).ready(function(){
        $('#testForm').submit(function(event){event.preventDefault();});

        $("#submitButton").click(function(){});
    });

</r:script>
<r:layoutResources />