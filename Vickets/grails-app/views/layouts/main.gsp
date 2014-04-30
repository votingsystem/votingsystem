<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
	<title><g:message code="appTitle"/></title>
    <r:external uri="/images/icon_16/fa-credit-card.png"/>
    <r:require module="application"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
	<body class="">
    <div>
        <div id="menu" class="navBarMainMenu" style="">
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
                        <a href="#"><i class="fa fa-users"></i><g:message code="groupvsLbl"/></a>
                        <h2><i class="fa fa-users"></i><g:message code="groupvsLbl"/></h2>
                        <ul>
                            <li>
                                <a href="${createLink(controller: 'groupVS', action: 'index')}" style="">
                                    <g:message code="groupvsLbl"/> <i class="fa fa-users"></i></i></a>
                            </li>
                            <li>
                                <a href="${createLink(controller: 'groupVS', action: 'admin')}" style="">
                                    <g:message code="groupvsAdminLbl"/> <i class="fa fa-cogs"></i>
                                </a>
                            </li>
                        </ul>
                    </li>
                    <li>
                        <a href="${createLink(controller: 'app', action: 'tools')}">
                            <g:message code="toolsSectionLbl"/> <i class="fa fa-cogs"></i>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                        </a>
                    </li>
                    <li>
                        <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                            style="color:#f9f9f9;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
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
                            <a href="${grailsApplication.config.grails.serverURL}" id="appTitle" class=""
                               style="font-size: 2.5em; margin: 0 0px 0 30px; color: #f9f9f9; font-weight: bold; ">
                                <g:message code="appTitle"/>
                            </a>
                            <div id="navBarSearchInput" class="navbar-form navbar-right input-group" style="width:15px;visibility: hidden;">
                                <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl" />"
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

    function isValidForm() {
 	    //allFields.removeClass("formFieldError");
 	}

</r:script>
<r:layoutResources/>