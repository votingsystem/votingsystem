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
            <votingSystem:datePicker id="testDate" required="true"></votingSystem:datePicker>
        </div>
        <button class="btn" type="submit">Test</button>
    </form>
</body>
</html>
<r:script>

    function submitForm(form) {
        var result = document.getElementById("testDate").getDate()
        console.log("=========== result: " + result)
        return false
    }

    $(document).ready(function(){
        $('#testForm').submit(function(event){event.preventDefault();});

        $("#submitButton").click(function(){});
    });

</r:script>
<r:layoutResources />