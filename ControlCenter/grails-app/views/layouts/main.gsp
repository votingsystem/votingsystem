<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-bar-chart-o.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="serverNameLbl"/></title>
    <g:javascript library="jquery" plugin="jquery"/>
    <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
    <asset:javascript src="jquery-ui-1.10.4.custom.min.js"/>
    <link rel="stylesheet" href="/ControlCenter/font-awesome/css/font-awesome.min.css" type="text/css"/>

    <asset:stylesheet src="bootstrap.min.css"/>
    <asset:javascript src="bootstrap.min.js"/>

    <asset:javascript src="jquery.multilevelpushmenu.min.js"/>
    <asset:stylesheet src="jquery.multilevelpushmenu.css"/>

    <asset:stylesheet src="votingSystem.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>

	<g:layoutHead/>
</head>
<body>
<div>
    <div id="menu" class="navBarMainMenu" style="">
        <nav>
            <h2><i class="fa fa-reorder"></i>
                <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
            </h2>
            <ul>
                <li>
                    <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                        <g:message code="electionSystemLbl"/><i class="fa fa-envelope"></i>
                    </a>
                </li>
                <li>
                    <a href="${createLink(controller: 'subscriptionVS', action: 'feeds')}">
                        <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                    </a>
                </li>
                <li>
                    <a  href="${createLink(controller: 'app', action: 'contact')}"
                        style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                    </a>
                </li>
            </ul>
        </nav>
    </div>
    <div class="navbar navbar-vickets" style="display:table; margin: 0px 0px 0px 0px; width:100%;">
        <div style="display:table-cell;width: 200px; margin:0px; padding:0px;">
            <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>
        </div>
        <div style="display:table-cell; width: 70%; vertical-align: middle;">
            <a id="selectedSubsystemLink" class=""
               style="font-size:2em; ;margin: 0 0px 0px 30px; color: #f9f9f9; font-weight: bold; white-space:nowrap;">
                <g:message code="serverNameLbl"/>
            </a>
        </div>
        <div style="display:table-cell;width: 200px; margin:0 20px 0 0; padding:0px;text-align: right;vertical-align: middle;">
            <div id="navBarSearchInput" class="navbar-right input-group" style="width:15px;visibility: hidden;">
                <div class="input-group-btn">
                    <button id="searchButton" type="button" class="btn navBar-vicket-button" style="border-color: #f9f9f9;">
                        <i class="fa fa-search navBar-vicket-icon" style="margin:0 0 0 0px;font-size: 1.2em; "></i></button>
                    <button id="advancedSearchButton" type="button" class="btn btn-default" data-container="body" data-toggle="popover"
                            data-placement="bottom" data-html="true" data-content="<div onclick='showAdvancedSearch()' style='cursor: pointer;'>
                                <g:message code="advancedSearchLbl"/>"
                            style="background-color: #ba0011; border-color: #f9f9f9; color:#f9f9f9; margin:0px 15px 0px 0px;">
                        <span class="caret"></span>
                        <span class="sr-only">Toggle Dropdown</span>
                    </button>
                </div>
            </div>
        </div>
    </div>
    <div id="searchPanel" class="" style="position: absolute;left: 40%; background:#ba0011; padding:10px 10px 10px 10px;display:none; z-index: 10;">
        <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl" />"
               style="width:160px; border-color: #f9f9f9;display:inline; vertical-align: middle;">
        <i id="searchPanelCloseIcon" onclick="toggleSearchPanel()" class="fa fa-times text-right navBar-vicket-icon"
           style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
    </div>
    <div id="" style="min-height: 600px; margin-top: 10px; max-width: 1300px; margin:0 auto;"><g:layoutBody/></div>

</div>

</body>

<g:include view="/include/dialog/resultDialog.gsp"/>
<g:include view="/include/dialog/advancedSearchDialog.gsp"/>
<g:include view="/include/dialog/windowAlertModal.gsp"/>

</html>
<asset:script>
    var isMenuVisible = false
    var isSearchInputVisible = false
	        
	$(function() {
        $('#advancedSearchButton').popover()
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



        $("#expandMenuIcon").click(function () {
            $('#menu').css("visibility", "visible")
            if(isMenuVisible) $('#menu').multilevelpushmenu( 'collapse' );
            else $('#menu').multilevelpushmenu( 'expand' );

        })

		 $('#searchForm').submit(function(event){
		 	console.log("searchForm")
		 	event.preventDefault();
		 	var searchQuery = {textQuery:$("#searchText").val()}
		 	getSearchResult(searchQuery)
		 });
	})

    $("#searchButton").click(function () {
        toggleSearchPanel()
    })

    var isSearchPanelVisible = false
    function toggleSearchPanel() {
        //$("#searchPanel").hide('slide',{direction:'right'},1000);
        if(!isSearchPanelVisible) {
            $("#searchPanel").slideDown("");
            $("#searchInput").val("")
        } else $("#searchPanel").slideUp("");
        isSearchPanelVisible = !isSearchPanelVisible
    }

    function showAdvancedSearch() {
        $('#advancedSearchDialog').modal()
        $('#advancedSearchButton').popover('hide')
    }
			 
</asset:script>
<asset:deferredScripts/>