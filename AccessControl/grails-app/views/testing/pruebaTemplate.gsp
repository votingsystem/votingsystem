<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>

    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>


    <asset:stylesheet src="votingSystem.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
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
		    <label>URL: </label><input type="text" id="urlControlCenter">
		    <button id="submitButton">Submit</button>
	</form>
	<div><input id="date" type="date" name="date" /></div>
</body>
</html>