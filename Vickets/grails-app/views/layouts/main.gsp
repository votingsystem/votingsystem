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
                   style="font-size:2.2em; ;margin: 0 0px 0px 30px; color: #f9f9f9; font-weight: bold; ">
                    <g:message code="appTitle"/>
                </a>
            </div>
            <div style="display:table-cell;width: 200px; margin:0px; padding:0px;text-align: right;vertical-align: middle;">
                <div id="navBarSearchInput" class="navbar-right input-group" style="width:15px;visibility: hidden;">
                    <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl" />"
                           style="width:90px; border-color: #f9f9f9;">
                    <div class="input-group-btn">
                        <button id="searchButton" type="button" class="btn navBar-vicket-button" style="border-color: #f9f9f9;">
                            <i class="fa fa-search navBar-vicket-icon" style="margin:0 0 0 0px;font-size: 1.2em; "></i></button>
                        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" tabindex="-1"
                                style="background-color: #ba0011; border-color: #f9f9f9; color:#f9f9f9; margin:0px 15px 0px 0px;">
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

        <div id="" style=""><g:layoutBody/></div>

        <g:if test="${"admin".equals(params.menu)}">
            <g:render template="/template/adminMenu"/>
            <r:script>
                document.getElementById('appTitle').innerHTML = "<g:message code="adminPageTitle"/>"
            </r:script>
        </g:if>
        <g:else><g:render template="/template/mainMenu"/></g:else>

    </div>

	</body>

    <g:include view="/include/dialog/advancedSearchDialog.gsp"/>

</html>
<r:script>
    var isMenuVisible = false
    var isSearchInputVisible = false

    $(function() {
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

    window.onload = function() {
        var result = getParameterByName('menu')
        if("" != result.trim()) {
            updateMenuLinks(result)
        }
    }

    function isValidForm() {
 	    //allFields.removeClass("formFieldError");
 	}

</r:script>
<r:layoutResources/>