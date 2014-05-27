<!DOCTYPE html>
<html>
<head>
    <r:require module="application"/>
  	<title>pruebaTemplate</title>
    <style>
	  	textarea { }
	  	input[id="subject"] { }
  	</style>
    <r:layoutResources />
</head>
<body>



<div class="form-group has-error">
    <i style="color:#6c0404; font-size: 4em;" class="fa fa-cog fa-spin"></i>
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