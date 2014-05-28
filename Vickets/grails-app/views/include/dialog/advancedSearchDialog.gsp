<!-- Advanced search Modal dialog -->
<div class="modal fade" id="advancedSearchDialog" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div id="advancedSearchDialogDiv" class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="myModalLabel"><g:message code="advancedSearchLbl"/></h4>
            </div>
            <form id="advancedSearchForm" class="" onsubmit="return submitAdvancedSearchForm(this);">
                <div class="modal-body">

                    <div id="searchErrorPanel" class="alert alert-danger" style="display: none;">
                        <a class="close" onclick="$('#searchErrorPanel').fadeOut()">×</a>
                        <div id="searchErrorMsg"></div>
                    </div>
                    <div class="">
                        <div  class="" style="display:block; margin: 0 0 10px 0;">
                            <div class="" style="display: table;">
                                <div style="display:table-cell;vertical-align: middle;">
                                    <label style="width:45px;"><g:message code="fromLbl"/></label>
                                </div>
                                <div style="display:table-cell; vertical-align: middle;">
                                    <votingSystem:datePicker id="advancedSearchFrom"></votingSystem:datePicker>
                                </div>
                                <div style="display:table-cell; vertical-align: middle;">
                                    <votingSystem:timePicker id="advancedSearchFromTime" style="margin: 0 0 0 20px;"></votingSystem:timePicker>
                                </div>
                            </div>
                            <div class="" style="display: table; margin: 10px 0 0 0;">
                                <div style="display:table-cell;vertical-align: middle;">
                                    <label style="width:45px;"><g:message code="toLbl"/></label>
                                </div>
                                <div style="display:table-cell; vertical-align: middle;">
                                    <votingSystem:datePicker id="advancedSearchTo"></votingSystem:datePicker>
                                </div>
                                <div style="display:table-cell; vertical-align: middle;">
                                    <votingSystem:timePicker id="advancedSearchToTime" style="margin: 0 0 0 20px;"></votingSystem:timePicker>
                                </div>
                            </div>
                        </div>
                        <div class="form-group" style="margin: 0 0 10px 0;">
                            <label class="control-label" ><g:message code="advancedSearchTextLbl"/></label>
                            <input type="text" id="advancedSearchText" class="form-control" style="">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button id="advancedSearchButton" type="submit" class="btn btn-accept-vs">
                        <g:message code="doAdvancedSearchLbl"/>
                    </button>
                    <button id="advancedSearchCancelButton" type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                        <g:message code="closeLbl"/>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
<asset:script>

    $("#searchInput").bind('keypress', function(e) {
        if (e.which == 13) {
            if("" != $("#searchInput").val().trim()) {
                processUserSearch($("#searchInput").val())
            }
        }
    });

    $("#searchButton").click(function(e) {
        if("" != $("#searchInput").val().trim()) {
            processUserSearch($("#searchInput").val())
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