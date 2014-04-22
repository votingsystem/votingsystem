<!DOCTYPE html>
<html>
<head>   
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="serverNameLabel"/></title>
    <r:external uri="/images/ControlCenter.png"/>
	<r:require module="application"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
<body>
<div>
    <div id="menu" style="visibility:hidden;">
        <nav>
            <h2><i class="fa fa-reorder"></i>
                <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
            </h2>
            <ul>
                <li>
                    <a href="${createLink(controller: 'eventVSElection', action: 'main')}">
                        <g:message code="homeLbl"/> <i class="a fa fa-home" style="color: #fdd302"></i>
                    </a>
                </li>
                <li>
                    <a href="${createLink(controller: 'subscriptionVS', action: 'feeds')}">
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
                    font-weight: bold; "><g:message code="serverNameLabel"/>
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
    <div id="pushobj" style="min-height: 600px; margin-top: 10px; max-width: 1000px; margin:0 auto;"><g:layoutBody/></div>

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

		$("#showAdvancedSearchButton").click(function () {
			$("#advancedSearchDialog").dialog("open");
		});

		 $('#searchForm').submit(function(event){
		 	console.log("searchForm")
		 	event.preventDefault();
		 	var searchQuery = {textQuery:$("#searchText").val()}
		 	getSearchResult(searchQuery)
		 });
	})
	
	
	function setMessageFromSignatureClient(appMessage) {
		var appMessageJSON = toJSON(appMessage)
		if(appMessageJSON != null) {
			if(ResponseVS.SC_PROCESSING == appMessageJSON.statusCode){
				signatureClientToolLoaded = true;
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			}
		}
	}
			 
</r:script>
<r:layoutResources/>