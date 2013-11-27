<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
   	<r:require modules="application"/>
    <style>
	  	textarea { }
	  	input[id="subject"] { }
  	</style>
  	<r:script>
        //This is number validation in for Firefox
        $("#resetForm").click()
        var allNumberFields = document.getElementsByClassName('userBaseDataInputNumber');
        for (var inputElement in allNumberFields) {
            console.log("========= inputElement: " + allNumberFields[inputElement])
            if(allNumberFields[inputElement] instanceof HTMLInputElement) {
                allNumberFields[inputElement].addEventListener('change', function(event) {
                    console.log("========= event.target.value: " + event.target.value)
                    if (isNaN(Number(event.target.value))) {
                        console.log("========= Not number")
                        event.target.message = "------ Debe introducir un número"
                        event.target.setCustomValidity("Invalid");
                    } else {
                        event.target.message = null
                        event.target.setCustomValidity("");
                    }
                }, false);
                allNumberFields[inputElement].addEventListener('invalid', setInvalidMsg, false);
            } else console.log("========= inputElement Instance of: " + allNumberFields[inputElement])
        }

        document.getElementById('numRepresentativesWithVote').addEventListener('change', checkRangeRepresentatives, false);
        document.getElementById('numRepresentatives').addEventListener('change', checkRangeRepresentatives, false);

        function checkRangeRepresentatives() {
            var representativeWithVote = Number(document.getElementById('numRepresentativesWithVote').value)
            var representatives = Number(document.getElementById('numRepresentatives').value)
            if (representativeWithVote > representatives) {
                document.getElementById('numRepresentativesWithVote').message = "El número de votos de representantes no puede ser superior al número de representantes"
                document.getElementById('numRepresentativesWithVote').setCustomValidity("DummyInvalid");
            }
        }

        function setInvalidMsg(event) {
            console.log(" --- setInvalidMsg --- ")
            if( event.target.message != null) {
                event.target.setCustomValidity(event.target.message);
            }
        }


	  	$(document).ready(function(){
	  		$('#testForm').submit(function(event){
                event.preventDefault();
                console.log("testForm")
            });
	
		  	$("#submitButton").click(function(){});

		  	
			var result = getUrlParam('param11', 'http://localhost:8080/AccessControl/prueba?param1=referer', true)


	  	});

	  	
  	</r:script>
	<r:layoutResources />
</head>
<body>
	<form id="testForm" style="display:block;margin:20px auto 30px auto; width:40%;">
		    <label for="urlControlCenter">URL: </label><input type="text" id="urlControlCenter">
        <input id="resetForm" type="reset" style="display:none;">
        <div style="display: block; margin: 0px 0px 5px 0px;">
            <label><g:message code="numRepresentativesMsg"/></label>
            <input type="number" id="numRepresentatives" min="1" value="1" class="userBaseDataInputNumber" required
                   style="width:120px;margin:10px 20px 0px 7px;"
                   title="<g:message code="numRepresentativesMsg"/>"
                   placeholder="<g:message code="numRepresentativesMsg"/>"
                   oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                   onchange="this.setCustomValidity('')">


            <label><g:message code="numRepresentativesWithVoteMsg"/></label>
            <input type="number" id="numRepresentativesWithVote" min="1" value="1" class="userBaseDataInputNumber" required
                   style="width:120px;margin:10px 20px 0px 7px;"
                   title="<g:message code="numRepresentativesWithVoteMsg"/>"
                   placeholder="<g:message code="numRepresentativesWithVoteMsg"/>"
                   oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
                   onchange="this.setCustomValidity('')">
        </div>
		    <button id="submitButton">Submit</button>
	</form>
	<div><input id="date" type="date" name="date" /></div>
</body>
</html>
<r:layoutResources />