<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="appTitle"/></title>
    <r:external uri="/images/euro_16.png"/>
    <r:require module="multilevelmenu"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
	<body>
    <div id="menu">
        <nav>
            <h2><i class="fa fa-reorder"></i><g:message code="sectionsLbl"/></h2>
            <ul>
                <li>
                    <a href="${createLink(controller: 'transaction', action: 'listener')}">
                        <g:message code="transactionsLbl"/> <i class="fa fa-money"></i></a>
                </li>
            </ul>
        </nav>
    </div>
    <div class="navbar navbar-tickets navbar-fixed-top" role="navigation" style="min-height:30px; margin-bottom: 6px; height: 50px;">
        <div class="container">
            <i id="expandMenuIcon" class="fa fa-bars fa-2x navbar-text nav-var-ticket-icon" style=""></i>

            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav">
                    <li class="" style="font-size: 2.5em; margin: 0 50px 0 50px; font-weight: bold;"><g:message code="appTitle"/></li>

                    <li>
                        <form class="navbar-form" role="search">
                            <div class="input-group"  style="width:270px;">
                                <input type="text" class="form-control" placeholder="<g:message code="searchLbl"/>" style="border-color:#f9f9f9 ;">
                                <span class="input-group-btn">
                                    <button type="submit" class="btn btn-default"
                                            style="color: #f9f9f9; background-color: #ba0011; border-color: #f9f9f9;">
                                        <i class="fa fa-search" style="margin:0 0 0 0px;font-size: 1.3em; "></i></button>
                                </span>
                            </div>
                        </form>
                    </li>
                    <li class="navbar-text">
                        <i id="rssMenuIcon" class="fa fa-rss-square  nav-var-ticket-icon" style="font-size: 2em;"></i>
                    </li>
                    <li class=""><a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                                     style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/></a></li>
                </ul>
            </div>
        </div>
    </div>

    <div id="pushobj" style="min-height: 600px;"><g:layoutBody/></div>

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
        if(isMenuVisible) $('#menu').multilevelpushmenu( 'collapse' );
        else $('#menu').multilevelpushmenu( 'expand' );

    })

</r:script>
<r:layoutResources/>