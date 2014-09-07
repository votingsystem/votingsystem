<!DOCTYPE html>
<html>
<head>
    <title><g:message code="vicketUserBaseDataSimulationCaption"/></title>
    <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrapValidator/dist/js', file: 'bootstrapValidator.min.js')}"></script>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrapValidator/dist/css', file: 'bootstrapValidator.min.css')}" type="text/css"/>
    <meta name="layout" content="main" />
</head>
<body style="">
    <div class="pageContenDiv" style="padding: 0px 30px 0px 30px;">
        <div  style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li><a href="${createLink(controller: 'simulation', action:'vickets', absolute:true)}">
                    <g:message code="vicketsOperationsLbl"/></a></li>
                <li class="active"><g:message code="addUsersToGroupButton"/></li>
            </ol>
        </div>
        <div id="vicketUserBaseDataSimulationDataDialog" style="width: 800px; padding:0px 0px 0px 0px; margin:0px auto 0px auto; ">
            <div class="errorMsgWrapper" style="display:none;"></div>
            <div style="margin: 0px 0px 30px 0px; width: 100%;">
                <button id="changeViewButton" type="button" class="btn btn-default" style="margin:0px 0px 0px 30px; display:none; float:right;"
                    onclick="showListenerDiv(!$('#simulationListenerDiv').is(':visible'))">
                    <g:message code="goToResultViewMsg"/>
                </button>
            </div>
            <div id="formDataDiv" style="width: 800px;" class="container">
                <form id="addUsersToGroupForm" method="post" class="form-horizontal">
                    <h4 id="pageTitle" class="pageHeader text-center">
                        <g:message code="addUsersToGroupButton"/>
                    </h4>
                    <div >
                        <div class="form-group" style="display: inline-block;">
                            <label class="" style="width:190px;"><g:message code="groupIdMsg"/></label>
                            <input type="number" id="groupId" name="groupId" class="form-control"
                                   style="width:220px;margin:0px 0px 0px 0px; padding: 0px 0px 0px 10px; display: inline;"/>
                        </div>
                    </div>

                    <div >
                        <div class="form-group" style="display: inline-block;">
                            <label class="" style="width:190px;"><g:message code="numUsersLbl"/></label>
                            <input type="number" id="numUsers" name="numUsers" min="0" value="1" class="form-control"
                                   style="width:220px;margin:0px 0px 0px 0px; padding: 0px 0px 0px 10px; display: inline;"/>
                        </div>
                    </div>
                    <div >
                        <div class="form-group" style="display: inline-block;">
                            <label class="" style="width:190px;"><g:message code="userIndexLbl"/></label>
                            <input type="number" id="userIndex" name="userIndex" min="0" value="1" class="form-control"
                                   style="width:220px;margin:0px 0px 0px 0px; padding: 0px 0px 0px 10px; display: inline;"/>
                        </div>
                    </div>
                    <div >
                        <div class="form-group" style="display: inline-block;">
                            <label class="" style="width:190px;"><g:message code="vicketServerLbl"/></label>
                            <input type="url" id="vicketServerURL"  class="form-control" style="width:220px;margin:0px 0px 0px 0px;display: inline;"
                                   value="http://vickets/Vickets/" placeholder="<g:message code="vicketServerURLMsg"/>"/>
                        </div>
                    </div>
                    <div class="form-group" style="width:700px;">
                        <button id="addUsersToGroupButton" type="submit" class="btn btn-warning" style="margin:15px 20px 20px 0px; float:right; ">
                            <g:message code="addUsersToGroupButton"/>
                        </button>
                    </div>
                </form>

            </div>
        </div>

    </div>
</body>
</html>
<asset:script>

    $(document).ready(function(){

        $('#addUsersToGroupForm').bootstrapValidator({
            excluded: [':disabled'],
            feedbackIcons: {
                valid: 'glyphicon glyphicon-ok',
                invalid: 'glyphicon glyphicon-remove',
                validating: 'glyphicon glyphicon-refresh'
            },
            message: '<g:message code="fieldTypeErrorMsg"/>',
            submitHandler: function(validator, form, submitButton) {
                var vicketServerURL = $('#vicketServerURL').val()
                var suffix = "/"
                if((vicketServerURL.indexOf(suffix, vicketServerURL.length - suffix.length) == -1)) {
                    vicketServerURL = vicketServerURL + "/"
                }
                vicketServerURL = vicketServerURL + "serverInfo"
                if(vicketServerURL.indexOf("http://") != 0) {
                    vicketServerURL = "http://" + vicketServerURL
                }

                $(".errorMsgWrapper").fadeOut()
                 var userBaseData = {numUsers: $('#numUsers').val(), userIndex:$('#userIndex').val() }
                 var simulationData = {service:'vicketAddUsersToGroupSimulationService', status:"INIT_SIMULATION",
                         groupId:$('#groupId').val(), serverURL:$('#vicketServerURL').val(), userBaseData:userBaseData}

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
                },
                userIndex: {
                    validators: {
                        notEmpty: {message: '<g:message code="emptyFieldErrorMsg"/>'}
                    }
                }
            }
        });

    })


function showListenerDiv(isListening) {
    document.getElementById("changeViewButton").style.display= 'block'
    if(isListening) {
        $("#changeViewButton").text("<g:message code="goToFormViewMsg"/>")
        $('#formDataDiv').fadeOut()
        $('#simulationListenerDiv').fadeIn()
        document.getElementById('pageTitle').innerHTML = '<g:message code="listeningAddUsersToGroup"/>'
    } else {
        $("#changeViewButton").text("<g:message code="goToResultViewMsg"/>")
        $('#simulationListenerDiv').fadeOut()
        $('#formDataDiv').fadeIn()
        SimulationService.close()
        document.getElementById("addUsersToGroupButton").disabled = false;
        document.getElementById('pageTitle').innerHTML = '<g:message code="addUsersToGroupButton"/>'
    }
}

function showErrorMsg(errorMsg) {
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").html('<p>' + errorMsg + '<p>')
	$("#vicketUserBaseDataSimulationDataDialog .errorMsgWrapper").fadeIn()
}

</asset:script>