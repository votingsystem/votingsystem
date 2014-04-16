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
        <div id="menu" style="visibility: hidden;">
            <nav>
                <h2><i class="fa fa-reorder"></i>
                    <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
                </h2>
                <ul>
                    <li>
                        <a href="${createLink(controller: 'transaction', action: 'listener')}">
                            <g:message code="transactionsLbl"/> <i class="fa fa-money"></i></a>
                    </li>
                    <li>
                        <a href="#">
                            <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i></a>
                    </li>
                    <li>
                        <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                            style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-envelope-o"></i></a>
                    </li>
                </ul>
            </nav>
        </div>
            <div  id="navbar" class="navbar navbar-tickets navbar-fixed-top" role="navigation" style="min-height:30px; margin-bottom: 6px;">
                <div class="container">
                    <div class="container-fluid">
                        <div class="navbar-collapse collapse">
                            <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-ticket-icon" style="margin: 5px 10px 0 15px;"></i>
                            <a class= "" href="${createLink(controller: 'app', action: 'index')}" style="">
                                <i id="homeIcon" class="fa fa fa-home navbar-text navBar-ticket-icon"
                                   style="color: #fdd302;margin: 5px 10px 0 35px;"></i>
                            </a>
                            <span id="appTitle" class="navbar-text center-block" style="font-size: 2.5em; margin: 0 30px 0 30px; font-weight: bold;">
                                <g:message code="appTitle"/>
                            </span>
                            <div class="input-group navbar-right"  style="width:160px; margin-top: 10px;">
                                <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl"/>"
                                       style="border-color:#f9f9f9; height: 30px;">
                                <span class="input-group-btn">
                                    <button type="submit" class="btn btn-default"
                                            style="color: #f9f9f9; background-color: #ba0011; border-color: #f9f9f9; height: 30px;">
                                        <i class="fa fa-search" style="margin:0 0 0 0px;font-size: 1.2em; "></i></button>
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        <div id="pushobj" style="min-height: 600px; margin-top: 10px;"><g:layoutBody/></div>

    </div>


	</body>
</html>
<r:script>
    var isMenuVisible = false
    var isSearchInputVisible = false

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


    $("#searchInput").bind('keypress', function(e) {
                if (e.which == 13) {
                    if("" != $("#searchInput").val().trim()) {
                        processUserSearch($("#searchInput").val())
                    }
                }
            });
</r:script>
<r:layoutResources/>