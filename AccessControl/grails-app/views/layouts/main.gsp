<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="serverNameLabel"/></title>
    <r:external uri="/images/icon_16/fa-credit-card.png"/>
	<r:require module="application"/>
    <r:require module="multilevel_menu"/>
	<g:layoutHead/>
	<r:layoutResources />
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
                        <g:message code="homeLbl"/> <i class="fa fa-home" style="color: #fdd302"></i>
                    </a>
                </li>
                <li>
                    <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                        <g:message code="electionSystemLbl"/><i class="fa fa-envelope"></i>
                    </a>
                </li>
                <li>
                    <a href="${createLink(controller: 'eventVSManifest', action: 'main')}">
                        <g:message code="manifestSystemLbl"/><i class="fa fa-file-text"></i>
                    </a>
                </li>
                <li>
                    <a href="${createLink(controller: 'eventVSClaim', action: 'main')}">
                        <g:message code="claimSystemLbl"/><i class="fa fa-exclamation-triangle"></i>
                    </a>
                </li>
                <li>
                    <a href="#"><i class="fa fa-users"></i><g:message code="representativesPageLbl"/></a>
                    <h2><i class="fa fa-users"></i><g:message code="representativesPageLbl"/></h2>
                    <ul>
                        <li>
                            <a href="${createLink(controller: 'representative', action: 'main')}" style="">
                                <g:message code="selectRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i></a>
                        </li>
                        <li>
                            <a href="#"><g:message code="toolsLbl"/><i class="fa fa-cogs"></i></a>
                            <h2><g:message code="toolsLbl"/><i class="fa fa-cogs"></i></h2>
                            <ul>
                                <li>
                                    <a href="${createLink(controller:'representative', action:'newRepresentative')}" style="">
                                        <g:message code="newRepresentativeLbl"/> <i class="fa fa-plus"></i></a>
                                </li>
                                <li>
                                    <a href="${createLink(controller: 'representative', action: 'edit')}" style="">
                                        <g:message code="editRepresentativeLbl"/> <i class="fa fa-pencil"></i>
                                    </a>
                                </li>
                                <li>
                                    <a href="${createLink(controller:'representative', action:'remove')}" style="">
                                        <g:message code="removeRepresentativeLbl"/> <i class="fa fa-minus"></i></a>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </li>
                <li>
                    <a href="#"><i class="fa fa-pencil-square-o"></i><g:message code="publishDocumentLbl"/></a>
                    <h2><i class="fa fa-pencil-square-o"></i><g:message code="publishDocumentLbl"/></h2>
                    <ul>
                        <li>
                            <a href="${createLink(controller: 'editor', action: 'manifest')}" style="font-weight: normal;">
                                <g:message code="publishManifestLbl"/><i class="fa fa-certificate"></i></a>
                        </li>
                        <li>
                            <a href="${createLink(controller: 'editor', action: 'claim')}" style="font-weight: normal;">
                                <g:message code="publishClaimLbl"/> <i class="fa fa-exclamation-triangle"></i>
                            </a>
                        </li>
                        <li>
                            <a href="${createLink(controller: 'editor', action: 'vote')}" style="font-weight: normal;">
                                <g:message code="publishVoteLbl"/> <i class="fa fa-envelope"></i>
                            </a>
                        </li>
                    </ul>
                </li>
                <li>
                    <a href="${createLink(controller: 'subscriptionVS', action: 'feeds')}">
                        <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                    </a>
                </li>
                <li>
                    <a  href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
                        style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                    </a>
                </li>
            </ul>
        </nav>
    </div>
    <div  id="navbar" class="navbar navbar-vickets navbar-fixed-top" role="navigation" style="min-height:30px; margin-bottom: 6px; width: 100%;">
        <i id="expandMenuIcon" class="fa fa-bars navbar-text navBar-vicket-icon navbar-left" style="margin: 5px 10px 0 15px;"></i>
        <div class="container">
            <div class="container-fluid">
                <div class="navbar-default">
                    <span id="appTitle" class="navbar-text center-block" style="font-size: 2.5em; margin: 0 0px 0 30px;
                    font-weight: bold; "><a id="selectedSubsystemLink" style="color: #f9f9f9;"></a>
                    </span>
                    <div id="navBarSearchInput" class="navbar-form navbar-right input-group" style=" width:200px;top:0px; visibility: hidden;">
                        <input id="searchInput" type="text" class="form-control" placeholder="<g:message code="searchLbl"/>"
                               style="width:120px; border-color: #f9f9f9;">
                        <div class="input-group-btn" style="">
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
    <div id="pushobj" style="min-height: 600px; margin-top: 10px; max-width: 1300px; margin:0 auto;"><g:layoutBody/></div>
</div>

    <div id="appletsFrame"  style="width:0px; height:0px;">
        <iframe id="votingSystemAppletFrame" src="" style="visibility:hidden;width:0px; height:0px;"></iframe>
    </div>
</body>

<g:include view="/include/dialog/advancedSearchDialog.gsp"/>	
<g:include view="/include/dialog/loadingAppletDialog.gsp"/>
<g:include view="/include/dialog/workingWithAppletDialog.gsp"/>
<g:include view="/include/dialog/browserWithoutJavaDialog.gsp"/>
<g:include view="/include/dialog/resultDialog.gsp"/>

</html>
<r:script>
    var isMenuVisible = false
    var isSearchInputVisible = false

	$(function() {
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
        }

        $("#searchInput").bind('keypress', function(e) {
            if (e.which == 13) {
                if("" != $("#searchInput").val().trim()) {
                    processUserSearch($("#searchInput").val())
                }
            }
        });

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

		$("#showAdvancedSearchButton").click(function () {
			$("#advancedSearchDialog").dialog("open");
		});

		 $('#searchForm').submit(function(event){
		 	console.log("searchForm")
		 	event.preventDefault();
		 	processUserSearch($("#searchText").val())
		 });

	})
</r:script>
<r:layoutResources/>