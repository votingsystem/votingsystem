<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
	<title><g:message code="appTitle"/></title>
    <r:external uri="/images/icon_16/fa-money.png"/>

    <meta name="viewport" content="width=device-width, initial-scale=1">
    <g:if test="${request.getHeader("User-Agent").contains("Android")}">
        <r:require module="applicationMobile"/>
    </g:if>
    <g:else>
        <r:require module="application"/>
    </g:else>
	<g:layoutHead/>
	<r:layoutResources />
</head>
	<body class="">
    <div>

        <div class="navbar navbar-vickets" style="display:table; margin: 0px 0px 0px 0px; width:100%;">
            <div style="display:table-cell;width: 200px; margin:0px; padding:0px;">
                <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>
            </div>
            <div style="display:table-cell; width: 70%; vertical-align: middle;">
                <a href="${grailsApplication.config.grails.serverURL}" id="appTitle" class=""
                   style="font-size:2em; ;margin: 0 0px 0px 30px; color: #f9f9f9; font-weight: bold; white-space:nowrap;">
                    <g:message code="appTitle"/>
                </a>
            </div>
            <div style="display:table-cell;width: 200px; margin:0px; padding:0px;text-align: right;vertical-align: middle;">
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

        <div id="" style=""><g:layoutBody/></div>

        <g:if test="${"admin".equals(params.menu)}">
            <g:render template="/template/adminMenu"/>
            <r:script>
                document.getElementById('appTitle').innerHTML = "<g:message code="adminPageTitle"/>"
            </r:script>
        </g:if>
        <g:elseif test="${"user".equals(params.menu)}">
            <g:render template="/template/userMenu"/>
            <r:script>
                document.getElementById('appTitle').innerHTML = "<g:message code="usersPageTitle"/>"
            </r:script>
        </g:elseif>
        <g:else><g:render template="/template/mainMenu"/></g:else>

    </div>

	</body>

    <g:include view="/include/dialog/advancedSearchDialog.gsp"/>
    <g:include view="/include/dialog/windowAlertModal.gsp"/>

</html>
<r:script>
    var isMenuVisible = false
    var isSearchInputVisible = false

    $(function() {
        $('#advancedSearchButton').popover()

        $('body').on('click', function (e) {
            //did not click a popover toggle or popover
            if ($(e.target).data('toggle') !== 'popover'
                && $(e.target).parents('.popover.in').length === 0) {
                $('[data-toggle="popover"]').popover('hide');
            }
        });

        $( '#menu' ).multilevelpushmenu({
            menuWidth: 300,
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
        updateMenuLinks()
    })

    $("#expandMenuIcon").click(function () {
        $('#menu').css("visibility", "visible")
        if(isMenuVisible) $('#menu').multilevelpushmenu( 'collapse' );
        else $('#menu').multilevelpushmenu( 'expand' );

    })


    $("#searchButton").click(function () {
        toggleSearchPanel()
    })

    function showAdvancedSearch() {
        $('#advancedSearchDialog').modal()
        $('#advancedSearchButton').popover('hide')
    }

    var isSerachPanelVisible = false
    function toggleSearchPanel() {
        //$("#searchPanel").hide('slide',{direction:'right'},1000);
        if(!isSerachPanelVisible) {
            $("#searchPanel").slideDown("");
            $("#searchInput").val("")
        } else $("#searchPanel").slideUp("");
        isSerachPanelVisible = !isSerachPanelVisible
    }

    function isValidForm() {
 	    //allFields.removeClass("formFieldError");
 	}

</r:script>
<r:layoutResources/>