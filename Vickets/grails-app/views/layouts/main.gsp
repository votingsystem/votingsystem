<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
	<title><g:message code="appTitle"/></title>
    <r:external uri="/images/euro_16.png"/>
    <r:require module="multilevelmenu"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
	<body class="">
    <div>
        <div id="menu" style="visibility:hidden;">
            <nav>
                <h2><i class="fa fa-reorder"></i>
                    <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
                </h2>
                <ul>
                    <li>
                        <a href="${createLink(controller: 'app', action: 'index')}">
                            <g:message code="homeLbl"/> <i class="a fa fa-home" style="color: #fdd302"></i>
                        </a>
                    </li>
                    <li>
                        <a href="${createLink(controller: 'transaction', action: 'listener')}">
                            <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                        </a>
                    </li>
                    <li>
                        <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                            style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-envelope-o"></i>
                        </a>
                    </li>
                </ul>
            </nav>
        </div>
            <div  id="navbar" class="navbar navbar-vickets navbar-fixed-top" role="navigation" style="min-height:30px; margin-bottom: 6px; width: 100%;">
                <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>
                <div class="container">
                    <div class="container-fluid">
                        <div class="navbar-collapse collapse">
                            <span id="appTitle" class="navbar-text center-block" style="font-size: 2.5em; margin: 0 0px 0 30px;
                                    font-weight: bold; "><g:message code="appTitle"/>
                            </span>
                            <div class="navbar-form navbar-right input-group" style="width:15px;">
                                <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl"/>"
                                       style="width:120px; border-color: #f9f9f9;">
                                <div class="input-group-btn">
                                    <button id="searchButton" type="button" class="btn navBar-vicket-button" style="border-color: #f9f9f9;">
                                        <i class="fa fa-search navBar-vicket-icon" style="margin:0 0 0 0px;font-size: 1.2em; "></i></button>
                                    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" tabindex="-1"
                                            style="background-color: #ba0011; border-color: #f9f9f9; color:#f9f9f9;">
                                        <span class="caret"></span>
                                        <span class="sr-only">Toggle Dropdown</span>
                                    </button>
                                    <ul class="dropdown-menu" role="menu" style="">
                                        <li><a id="showAdvancedSearchButton" href="#"><g:message code="advancedSearchLbl"/></a></li>
                                    </ul>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
        <div id="pushobj" style="min-height: 600px; margin-top: 10px;"><g:layoutBody/></div>

    </div>

    <!-- Advanced search Modal dialog -->
    <div class="modal fade" id="advancedSearchDialog" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
        <div class="modal-dialog">
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
                        <div class="form-horizontal">
                            <div  class="form-inline" style="display:block; margin: 0 0 10px 0;">
                                <label><g:message code="fromLbl"/></label>
                                <votingSystem:datePicker id="advancedSearchFrom"></votingSystem:datePicker>
                                <votingSystem:timePicker id="advancedSearchFromTime"></votingSystem:timePicker>
                                <label style="margin: 0 0 0 20px;"><g:message code="toLbl"/></label>
                                <votingSystem:datePicker id="advancedSearchTo"></votingSystem:datePicker>
                                <votingSystem:timePicker id="advancedSearchToTime"></votingSystem:timePicker>
                            </div>
                            <div class="form-group" style="margin: 0 0 10px 0;">
                                <label class="control-label" ><g:message code="advancedSearchTextLbl"/></label>
                                <input type="text" id="advancedSearchText" class="form-control" style="">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-default btn-cancel-vs" data-dismiss="modal" style="">
                            <g:message code="closeLbl"/>
                        </button>
                        <button id="advancedSearchButton" type="submit" class="btn btn-accept-vs">
                            <g:message code="doAdvancedSearchLbl"/>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

	</body>
</html>
<r:script>
    var isMenuVisible = false
    var isSearchInputVisible = false
    var dateBeginFrom  = $("#advancedSearchFrom")
	var dateBeginTo    = $("#advancedSearchTo")
    $(function() {
        $( '#menu' ).multilevelpushmenu({
            menuWidth: 250,
            onItemClick: function() {
                var event = arguments[0], // First argument is original event object
                $menuLevelHolder = arguments[1], // Second argument is menu level object containing clicked item (<div> element)
                $item = arguments[2], // Third argument is clicked item (<li> element)
                options = arguments[3]; // Fourth argument is instance settings/options object
                var itemHref = $item.find( 'a:first' ).attr( 'href' );
                location.href = itemHref;
            },
            onCollapseMenuEnd: function() {
                isMenuVisible = false
            },
            onExpandMenuEnd: function() {
                isMenuVisible = true
            },
            collapsed: true,
            fullCollapse: true
        });

    })

    $("#expandMenuIcon").click(function () {
        $('#menu').css("visibility", "visible")
        if(isMenuVisible) $('#menu').multilevelpushmenu( 'collapse' );
        else $('#menu').multilevelpushmenu( 'expand' );

    })

    $("#searchButton").click(function () {
        if("" != $("#searchInput").val().trim()) {
            processUserSearch($("#searchInput").val())
        }
    })

    $("#showAdvancedSearchButton").click(function () {
        $('#advancedSearchDialog').modal()
    })

    $("#advancedSearchButton").click(function () {
        console.log("========= advancedSearchButton")
        if(dateBeginFrom.datepicker("getDate") === null) {
            dateBeginFrom.addClass( "formFieldError" );
            showErrorMsg('<g:message code="emptyFieldMsg"/>')
            return
        }

        if(dateBeginTo.datepicker("getDate") === null) {
            dateBeginTo.addClass( "formFieldError" );
            showErrorMsg('<g:message code="emptyFieldMsg"/>')
            return
        }

        if(dateBeginFrom.datepicker("getDate") >
            dateBeginTo.datepicker("getDate")) {
            showErrorMsg('<g:message code="dateRangeERRORMsg"/>')
            dateBeginFrom.addClass("formFieldError");
            dateBeginTo.addClass("formFieldError");
            return
        }

        if(dateFinishFrom.datepicker("getDate") === null) {
            dateFinishFrom.addClass( "formFieldError" );
            showErrorMsg('<g:message code="emptyFieldMsg"/>')
            return
        }

        if(dateFinishTo.datepicker("getDate") === null) {
            dateFinishTo.addClass( "formFieldError" );
            showErrorMsg('<g:message code="emptyFieldMsg"/>')
            return
        }

        if(dateFinishFrom.datepicker("getDate") >
            dateFinishTo.datepicker("getDate")) {
            showErrorMsg('<g:message code="dateRangeERRORMsg"/>')
            dateFinishFrom.addClass("formFieldError");
            dateFinishTo.addClass("formFieldError");
            return
        }
    })

    $("#searchInput").bind('keypress', function(e) {
        if (e.which == 13) {
            if("" != $("#searchInput").val().trim()) {
                processUserSearch($("#searchInput").val())
            }
        }
    });

    function isValidForm() {
 	    //allFields.removeClass("formFieldError");
 	}

    function showErrorMsg(errorMsg) {
        console.log("========= errorMsg: " + errorMsg)
        $("#searchErrorMsg").html('<p>' + errorMsg + '<p>')
        $("#searchErrorPanel").fadeIn(500)
    }

    $('#advancedSearchDialog').on('hidden.bs.modal', function (e) { //reset form
        $("#searchErrorPanel").hide()
    })

    function submitAdvancedSearchForm(form) {
        console.log("============= submitAdvancedSearchForm")
        return false
    }

</r:script>
<r:layoutResources/>