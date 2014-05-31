<!DOCTYPE html>
<html>
<head>
  	<title>pruebaTemplate</title>
    <g:javascript library="jquery" plugin="jquery"/>
    <link rel="stylesheet" href="${resource(dir: 'font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>

    <asset:stylesheet src="bootstrap.min.css"/>
    <asset:javascript src="bootstrap.min.js"/>

    <asset:stylesheet src="votingSystem.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
</head>
<body>

<form id="advancedSearchForm" class="" onsubmit="return submitAdvancedSearchForm(this);">
    <div class="modal-body">

        <div id="searchErrorPanel" class="alert alert-danger" style="display: none;">
            <a class="close" onclick="$('#searchErrorPanel').fadeOut()">×</a>
            <div id="searchErrorMsg"></div>
        </div>
        <div class="form-horizontal">
            <div  class="form-inline" style="display:block; margin: 0 0 10px 0;">
                <div style="display: block;">
                    <label style="width:45px;"><g:message code="fromLbl"/></label>
                    <votingSystem:datePicker id="advancedSearchFrom"></votingSystem:datePicker>
                    <votingSystem:timePicker id="advancedSearchFromTime" style="margin: 0 0 0 20px;"></votingSystem:timePicker>
                </div>
                <div style="display: block; margin: 7px 0 0 0;">
                    <label style="margin: 0 0 0 0;width:45px;"><g:message code="toLbl"/></label>
                    <votingSystem:datePicker id="advancedSearchTo"></votingSystem:datePicker>
                    <votingSystem:timePicker id="advancedSearchToTime" style="margin: 0 0 0 20px;"></votingSystem:timePicker>
                </div>
            </div>
            <div class="form-group" style="margin: 0 0 10px 0;">
                <label class="control-label" ><g:message code="advancedSearchTextLbl"/></label>
                <input type="text" id="advancedSearchText" class="form-control" style="">
            </div>
        </div>
    </div>
    <div class="modal-footer">
        <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
            <g:message code="closeLbl"/>
        </button>
        <button id="advancedSearchButton" type="submit" class="btn btn-accept-vs">
            <g:message code="doAdvancedSearchLbl"/>
        </button>
    </div>
</form>


<div id="dialog" title="Basic dialog">
    <!-- Advanced search Modal dialog -->fdsfsdfsdf

    <asset:script>

    $("#searchInput").bind('keypress', function(e) {
        if (e.which == 13) {
            if("" != $("#searchInput").val().trim()) {
                processUserSearch($("#searchInput").val())
            }
        }
    });

    $('#advancedSearchDialog').on('hidden.bs.modal', function (e) { //reset form
        $("#searchErrorPanel").hide()
        $("#advancedSearchDialogDiv").removeClass( "has-error" );
        document.getElementById("advancedSearchFrom").reset()
        document.getElementById("advancedSearchFromTime").reset()
        document.getElementById("advancedSearchTo").reset()
        document.getElementById("advancedSearchToTime").reset()
        $("#advancedSearchText").val()
    })


    function submitAdvancedSearchForm(form) {
        var advancedSearchFrom = document.getElementById("advancedSearchFrom")
        var advancedSearchFromValue = advancedSearchFrom.getDate()
        var advancedSearchFromTime = document.getElementById("advancedSearchFromTime")
        var advancedSearchFromTimeValue = advancedSearchFromTime.getTime()
        var advancedSearchTo = document.getElementById("advancedSearchTo")
        var advancedSearchToValue = advancedSearchTo.getDate()
        var advancedSearchToTime = document.getElementById("advancedSearchToTime")
        var advancedSearchToTimeValue = advancedSearchToTime.getTime()
        var advancedSearchTextValue = ("" == $("#advancedSearchText").val().trim())? null: $("#advancedSearchText").val();


        var errorMessage = null

        var userSearch
        if(advancedSearchFromValue == null && advancedSearchToValue == null && advancedSearchTextValue == null) {
            errorMessage = '<g:message code="allSearchFieldsEmptydMsg"/>'
            $("#advancedSearchDialogDiv").addClass( "has-error" );
        } else {
            var searchFrom
            if(advancedSearchFromValue != null) searchFrom = advancedSearchFromValue.format() + " " + advancedSearchFromTimeValue
            var searchTo
            if(advancedSearchToValue != null) searchTo = advancedSearchToValue.format() + " " + advancedSearchToTimeValue
            userSearch = {searchText:$("#advancedSearchText").val().trim(), searchFrom: searchFrom, searchTo:searchTo}
            console.log("advancedSearchDialog - userSearch: " + JSON.stringify(userSearch))
        }

        if(errorMessage != null) {
            $("#searchErrorMsg").html('<p>' + errorMessage + '<p>')
            $("#searchErrorPanel").fadeIn(500)
        } else {
            $('#advancedSearchDialog').modal('hide')
            processUserSearchJSON(userSearch)
        }
        return false
    }

    </asset:script>
</div>

</body>
</html>
<asset:script>
    $(function() {
        $( "#dialog" ).dialog();
    });
</asset:script>