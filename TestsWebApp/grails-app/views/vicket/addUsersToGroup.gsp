<!DOCTYPE html>
<html>
<head>
    <title><g:message code="vicketUserBaseDataSimulationCaption"/></title>
    <asset:stylesheet src="bootstrapValidator.min.css"/>
    <asset:javascript src="bootstrapValidator.min.js"/>
    <meta name="layout" content="main" />
</head>
<body style="">
<div class="pageContenDiv">
    <div style="padding: 0px 30px 0px 30px;">
        <div class="row" style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'simulation', action:'vickets', absolute:true)}">
                    <g:message code="vicketsOperationsLbl"/></a></li>
                <li class="active"><g:message code="addUsersToGroupButton"/></li>
            </ol>
        </div>
        <div id="vicketUserBaseDataSimulationDataDialog"  class="row"
             style="padding:0px 20px 20px 20px; margin:0px 0px 0px 0px;overflow: hidden; position:relative;">
            <div class="errorMsgWrapper" style="display:none;"></div>
            <div style="margin: 15px 0px 30px 0px;display: table; width: 100%;">
                <div id="testButtonDiv" style="display:table-cell; text-align:center;vertical-align: middle;">
                    <button id="testButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px;">
                        <g:message code="goToResultViewMsg"/>
                    </button>
                </div>
            </div>
            <div id="formDataDiv" style="width: 800px; margin: 0px auto 0px auto;">
                <fieldset id="userBaseData">
                    <legend style="font-size: 1.2em; font-weight: bold;"><g:message code="addUsersToGroupButton"/></legend>
                    <form id="addUsersToGroupForm" method="post" class="form-horizontal">
                        <div class="form-group" style="">
                            <label class="col-sm-3"><g:message code="groupIdMsg"/></label>
                            <div class="col-sm-3" style="margin:0px 0px 0px 0px;">
                                <input type="number" id="groupId" name="groupId" class="form-control"
                                       style="margin:0px 0px 0px 0px;"/>
                            </div>
                        </div>

                        <div class="form-group" style="">
                            <label class="col-sm-3"><g:message code="numUsersLbl"/></label>
                            <div class="col-sm-3" style="margin:0px 0px 0px 0px;">
                                <input type="number" id="numUsers" name="numUsers" min="0" value="1"
                                       class="form-control" placeholder="<g:message code="numRepresentativesMsg"/>"/>
                            </div>

                        </div>
                        <div class="form-group" style="">
                            <label class="col-sm-3"><g:message code="vicketServerLbl"/></label>
                            <div class="col-sm-3" style="margin:0px 0px 0px 0px;">
                                <input type="url" id="vicketServerURL"  class="form-control" style="margin:0px 0px 0px 0px;"
                                       value="http://vickets/Vickets/" placeholder="<g:message code="vicketServerURLMsg"/>"/>
                            </div>
                        </div>
                        <div class="form-group" style="width:700px;">
                            <button type="submit" class="btn btn-warning" style="margin:15px 20px 20px 0px; float:right; ">
                                <g:message code="addUsersToGroupButton"/>
                            </button>
                        </div>
                    </form>

                </fieldset>

            </div>
        </div>

        <div id="simulationListenerDiv" style="display: none;">
            <g:include view="/include/listenSimulation.gsp"/>
        </div>
    </div>
</div>


</body>
</html>
<asset:script>


    $(document).ready(function(){


        $('#addUsersToGroupForm').bootstrapValidator({
            // Only disabled elements are excluded
            // The invisible elements belonging to inactive tabs must be validated
            excluded: [':disabled'],
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            message: '<g:message code="fieldTypeErrorMsg"/>',
            submitHandler: function(validator, form, submitButton) {
                console.log(" ===== submitHandler")
                event.preventDefault();
                $(".errorMsgWrapper").fadeOut()

                 var userBaseData = {userIndex:$('#groupId').val(),
                    numUsers: $('#numUsers').val() }

                 var simulationData = {service:'vicketUserBaseDataSimulationService', status:"INIT_SIMULATION",
                         serverURL:$('#vicketServerURL').val(), userBaseData:userBaseData}

                 showListenerDiv(true)
                 showSimulationProgress(simulationData)
            },
            fields: {
                groupId: {
                    validators: {
                        notEmpty: {message: '<g:message code="emptyFieldErrorMsg"/>'}
                    }
                },
                numUsers: {
                    validators: {
                        notEmpty: {message: '<g:message code="emptyFieldErrorMsg"/>'}
                    }
                }
            }
        });

    })


function isValidForm() {
	//numRepresentativesMsg"/></label>numRepresentativesWithVote numUsersWithRepresentativeMsg numUsersWithRepresentativeWithVote


	if(!document.getElementById('vicketServerURL').validity.valid) {
		$("#vicketServerURL").addClass( "formFieldError" );
		showResultDialog('<g:message code="dataFormERRORLbl"/>',
			'<g:message code="emptyFieldLbl"/>', function() {
			$("#addControlCenterDialog").dialog("open")
		})
		return false
	}
	var vicketServerURL = $('#vicketServerURL').val()
	var suffix = "/"
	if((vicketServerURL.indexOf(suffix, vicketServerURL.length - suffix.length) == -1)) {
		vicketServerURL = vicketServerURL + "/"
	}
	vicketServerURL = vicketServerURL + "serverInfo"
	if(vicketServerURL.indexOf("http://") != 0) {
		vicketServerURL = "http://" + vicketServerURL
	}
	return true
}

$("#testButtonDiv").hide()

function showListenerDiv(isListening) {
    $("#testButtonDiv").show()
    if(isListening) {
        $("#testButton").text("<g:message code="goToFormViewMsg"/>")
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        $('#pageTitle').text('<g:message code="listeningtVicketUserBaseDataSimulationMsg"/>')
    } else {
        $("#testButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        $('#pageTitle').text('<g:message code="initVicketUserBaseDataSimulationMsg"/>')
    }
}


$("#testButton").click(function () {
    showListenerDiv(!$("#simulationListenerDiv").is(":visible"))
});

function showErrorMsg(errorMsg) {
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</asset:script>