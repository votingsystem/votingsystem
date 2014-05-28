<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="serverNameLbl"/></title>
    <g:javascript library="jquery" plugin="jquery"/>
    <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
    <asset:javascript src="jquery-ui-1.10.4.custom.min.js"/>
    <link rel="stylesheet" href="/AccessControl/font-awesome/css/font-awesome.min.css" type="text/css"/>

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
    <div class="navbar navbar-vickets" style="display:table; margin: 0px 0px 0px 0px; width:100%;">
        <div style="display:table-cell;width: 200px; margin:0px; padding:0px;">
            <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>
        </div>
        <div style="display:table-cell; width: 70%; vertical-align: middle;">
            <a id="selectedSubsystemLink" class=""
               style="font-size:2em; ;margin: 0 0px 0px 30px; color: #f9f9f9; font-weight: bold; white-space:nowrap;">
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
    <div id="searchPanel" class="" style="position: absolute;left: 40%; background:#ba0011; padding:10px 10px 10px 10px;display:none;">
        <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl" />"
               style="width:160px; border-color: #f9f9f9;display:inline; vertical-align: middle;">
        <i id="searchPanelCloseIcon" onclick="toggleSearchPanel()" class="fa fa-times text-right navBar-vicket-icon"
           style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
    </div>
    <div><g:layoutBody/></div>
</div>

<g:if test="${"admin".equals(params.menu)}"><g:render template="/template/adminMenu"/></g:if>
<g:else><g:render template="/template/mainMenu"/></g:else>

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
	    var selectedSubsystem = "${selectedSubsystem}"
        if(SubSystem.VOTES == selectedSubsystem) {
            selectedSubsystemLink = "${createLink(controller: 'eventVSElection', action: 'main')}"
            selectedSubsystemText = "<g:message code="electionSystemLbl"/>"

        } else if(SubSystem.CLAIMS == selectedSubsystem) {
            selectedSubsystemLink = "${createLink(controller: 'eventVSClaim', action: 'main')}"
            selectedSubsystemText = "<g:message code="claimSystemLbl"/>"
        } else if(SubSystem.MANIFESTS == selectedSubsystem) {
            selectedSubsystemLink = "${createLink(controller: 'eventVSManifest', action: 'main')}"
            selectedSubsystemText = "<g:message code="manifestSystemLbl"/>"
        } else if(SubSystem.REPRESENTATIVES == selectedSubsystem) {
            selectedSubsystemLink = "${createLink(controller: 'representative', action: 'main')}"
            selectedSubsystemText = "<g:message code="representativesPageLbl"/>"
        } else if(SubSystem.FEEDS == selectedSubsystem) {
            selectedSubsystemLink = "${createLink(controller: 'subscriptionVS', action: 'feeds')}"
            selectedSubsystemText = "<g:message code="subscriptionsPageLbl"/>"
        } else {
            console.log("### unknown subsytem -> " + selectedSubsystem)
            selectedSubsystemText = "<g:message code="votingsystemPageLbl"/>"
        }

        <g:if test="${"admin".equals(params.menu)}">
            selectedSubsystemText = "<g:message code="adminPageTitle"/>"
        </g:if>
	    $('#selectedSubsystemLink').attr('href',selectedSubsystemLink);
	    $('#selectedSubsystemLink').text(selectedSubsystemText)

		
        $( '#menu' ).multilevelpushmenu({
                menuWidth: 330,
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
                backText:'<g:message code="backLbl"/>' ,
                collapsed: true,
                fullCollapse: true
            });


        $("#expandMenuIcon").click(function () {
            $('#menu').css("visibility", "visible")
            if(isMenuVisible) $('#menu').multilevelpushmenu( 'collapse' );
            else $('#menu').multilevelpushmenu( 'expand' );
        })


        $("#searchButton").click(function () {
            toggleSearchPanel()
        })

        <g:if test="${"user".equals(params.menu)}">
            $(".breadcrumbVS").css("display", "none");
            setMainMenuIconVisible(false)
        </g:if>
        updateMenuLinks()
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

    function setMainMenuIconVisible(isVisible) {
        if(isVisible) {
            $('#expandMenuIcon').css("display", "visible")
        } else $('#expandMenuIcon').css("display", "none")
    }

</asset:script>
<asset:deferredScripts/>