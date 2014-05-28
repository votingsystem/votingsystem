<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
    <style>
	  	textarea { }
	  	input[id="subject"] { }
  	</style>
  	<asset:script>
	  	$(document).ready(function(){
	  		$('#testForm').submit(function(event){event.preventDefault();});
	
		  	$("#submitButton").click(function(){});
	  	});
  	</asset:script>
</head>
<body>
	<form id="testForm" style="display:block;margin:20px auto 30px auto; width:40%;">
		    <label>URL: </label>
		    	<input type="text" id="urlControlCenter">
		    <button id="submitButton">Submit</button>
	</form>
</body>
</html>