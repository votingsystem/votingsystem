<!DOCTYPE html>
<html>
<head>
  	<title>pruebaHistory</title>
   	<r:require modules="application"/>
    <style>
	  	textarea { }
	  	input[id="subject"] { }
  	</style>
  	<r:script>


	  	for(i=0;i<5;i++){
	  	  	var stateObject = {id: i};
	  	  	var title = "Wow Title "+i;
	  	  	var newUrl = "/my/awesome/url/"+i;
	  	  	history.pushState(stateObject,title,newUrl);
	  		console.log(" - pushingState: " + i);
	  	}
	  	 
	  	window.addEventListener('popstate', function(event) {
	  	  readState(event.state);
	  	});
	  	 
	  	function readState(data){
	  	  	console.log("readState: " + data)
	  	  	//console.log("readState: " + data.i)
	  	}
  	

	  	$(document).ready(function(){
	  		$('#testForm').submit(function(event){event.preventDefault();});

		  	$("#submitButton").click(function(){});
	  	});

  	</r:script>
</head>

<body>

	<form id="testForm" style="display:block;margin:20px auto 30px auto; width:40%;">
	    <label for="one">URL: </label>
	    	<input type="text" id="urlControlCenter">
	    <button id="submitButton">Submit</button>
	</form>

<progress style="display:block;margin:0px auto 0px auto;"></progress>

</body>

</html>