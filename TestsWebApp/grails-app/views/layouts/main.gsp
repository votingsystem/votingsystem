<!DOCTYPE html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-bug.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <g:javascript library="jquery" plugin="jquery"/>
    <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/jquery-ui', file: 'jquery-ui.min.js')}"></script>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>

    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrap/dist/js', file: 'bootstrap.min.js')}"></script>

    <script type="text/javascript" src="${resource(dir: 'bower_components/multilevelpushmenu', file: 'jquery.multilevelpushmenu.min.js')}"></script>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/multilevelpushmenu', file: 'jquery.multilevelpushmenu.css')}" type="text/css"/>

    <asset:stylesheet src="testWebApp.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <g:layoutHead/>
</head>
<body>
<div id="navBarMainMenu">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;">
                <div style="margin: 60px 0px 0px 0px;"><g:message code="sectionsLbl"/></div></span>
        </h2>
        <ul>
            <li>
                <a href="${grailsApplication.config.grails.serverURL}">
                    <g:message code="homeLbl"/> <i class="a fa fa-home" style="color: #fdd302"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'simulation', action: 'votingSystem')}">
                    <g:message code="votingSystemOperationsLbl"/><i class="fa fa-envelope"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'simulation', action: 'vickets')}">
                    <g:message code="vicketsOperationsLbl"/><i class="fa fa-money"></i>
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
            </div>
        </div>
    </div>
</div>
<div id="" style=""><g:layoutBody/></div>
</body>

</html>
<asset:script>
    var isMenuVisible = false
    var isSearchInputVisible = false

    $(function() {
        $( '#navBarMainMenu' ).multilevelpushmenu({
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
            $('#navBarMainMenu').css("visibility", "visible")
            if(isMenuVisible) $('#navBarMainMenu').multilevelpushmenu( 'collapse' );
            else $('#navBarMainMenu').multilevelpushmenu( 'expand' );

        })

    })
</asset:script>
<asset:deferredScripts/>