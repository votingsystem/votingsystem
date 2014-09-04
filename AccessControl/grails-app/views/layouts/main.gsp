<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-money.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="votingSystem.css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-navbar', file: 'votingsystem-navbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-socket', file: 'votingsystem-socket.html')}">
    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votingsystem-message-dialog.gsp']"/>">

    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto;">
<polymer-element name="nav-bar" attributes="url loading">
    <template>
        <!--<core-ajax id="ajax" auto on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}" handleAs="document"></core-ajax>-->
        <core-xhr id="ajax" ></core-xhr>
        <!-- put core signals names in lower case !!!-->
        <core-signals on-core-signal-innerpage="{{innerPageSignal}}"></core-signals>
        <votingsystem-navbar id="_navbar" style="display: none;">
            <core-header-panel mode="seamed" id="core_header_panel" navigation flex class="navbar-vickets">
                <core-toolbar id="core_toolbar" style="background-color: #ba0011;">
                </core-toolbar>
                <core-menu valueattr="label" id="core_menu" theme="core-light-theme" style="font-size: 1.2em;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="data-href" on-core-select="{{drawerItemSelected}}">
                        <g:if test="${"admin".equals(params.menu)}">
                            {{ "<g:message code="adminPageTitle"/>" | setTitle}}
                            <!--<template if="{{isClientToolConnected}}">-->
                                <paper-item data-href="${createLink(controller: 'eventVSElection', action:'main')}">
                                    <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="electionSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'editor', action:'vote', absolute: true)}">
                                    <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishVoteLbl"/>
                                </paper-item>

                                <!--<paper-item data-href="${createLink(controller: 'eventVSManifest', action:'main')}">
                                    <i class="fa fa-file-text" style="margin:0px 10px 0px 0px;"></i> <g:message code="manifestSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'editor', action:'manifest')}">
                                    <i class="fa fa-file-text" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishManifestLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'eventVSClaim', action:'main')}">
                                    <i class="fa fa-exclamation-triangle" style="margin:0px 10px 0px 0px;"></i> <g:message code="claimSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'editor', action:'claim')}">
                                    <i class="fa fa-exclamation-triangle" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishClaimLbl"/>
                                </paper-item>-->

                                <paper-item data-href="${createLink(controller: 'representative', action:'main')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="representativesPageLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'representative', action:'newRepresentative')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="newRepresentativeLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'representative', action:'edit')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="editRepresentativeLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'representative', action:'remove')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="removeRepresentativeLbl"/>
                                </paper-item>

                            <!--</template>-->
                        </g:if>
                        <g:else>
                            {{ "<g:message code="votingsystemPageLbl"/>" | setTitle}}
                            <paper-item data-href="${createLink(controller: 'eventVSElection', action:'main')}">
                                <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="electionSystemLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'representative', action:'main')}">
                                <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="representativesPageLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action:'tools')}">
                                <i class="fa fa-cogs" style="margin:0px 10px 0px 0px;"></i> <g:message code="toolsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'subscriptionVS', action:'feeds')}">
                                <i class="fa fa-rss" style="margin:0px 10px 0px 0px;"></i> <g:message code="subscriptionLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action:'contact')}">
                                <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                            </paper-item>

                            <!--<paper-item data-href="${createLink(controller: 'eventVSManifest', action:'main')}">
                                <i class="fa fa-file-text" style="margin:0px 10px 0px 0px;"></i> <g:message code="manifestSystemLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'eventVSClaim', action:'main')}">
                                <i class="fa fa-exclamation-triangle" style="margin:0px 10px 0px 0px;"></i> <g:message code="claimSystemLbl"/>
                            </paper-item>-->

                        </g:else>
                    </core-selector>
                </core-menu>
            </core-header-panel>
            <div id="appTitle" style="width: 100%;" tool>{{appTitle}}</div>
            <content id="content"></content>
        </votingsystem-navbar>
        <div style="width: 30px;margin: 100px auto 0px auto;display:{{loading?'block':'none'}}">
            <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
        </div>
        <content id="content"></content>
    </template>
    <script>
        Polymer('nav-bar', {
            appTitle:"<g:message code="appTitle"/>",
            url:'',
            ajaxOptions:{method:'get', responseType:'document'},
            ready: function() {
                this.$._navbar.searchVisible(false)
                this.$._navbar.style.display = 'block';
                this.fire('nav-bar-ready');
                var navBar = this
                window.addEventListener('popstate', function(event) {
                    navBar.url = document.location.href
                });
                this.isClientToolConnected = window['isClientToolConnected']
                console.log(this.tagName + " - ready - isClientToolConnected: " + this.isClientToolConnected)
            },
            innerPageSignal:function(e, detail, sender) {
                this.url = detail;
            },
            urlChanged: function() {
                this.loadURL(this.url)
            },
            loadURL: function(urlToLoad) {
                this.loading= true;
                history.pushState(null, null, this.url);
                var newURL = updateMenuLink(urlToLoad, "mode=innerPage")
                this.ajaxOptions.url = newURL
                this.ajaxOptions.callback = this.ajaxResponse.bind(this)
                this.$.ajax.request(this.ajaxOptions)
                /*if(this.$.ajax.url == newURL)  this.$.ajax.go()
                else this.$.ajax.url = newURL*/
            },
            drawerItemSelected: function(e) {
                if(e.detail.isSelected) {
                    this.fire('item-selected', this.coreSelectorValue)
                    if(this.$.coreSelector.selectedItem != null && 'changeToAdmin' == this.$.coreSelector.selectedItem.id) {
                        window.location.href = window.location.href.replace("menu=superadmin", "menu=admin");
                    } else {
                        this.loadURL(this.coreSelectorValue)
                    }
                    this.coreSelectorValue = null
                }
            },
            searchVisible: function(isVisible) {
                this.$._navbar.searchVisible(isVisible)
            },
            setTitle: function(appTitle) {
                this.appTitle = appTitle
            },
            ajaxResponse: function(xhrResponse, xhr) {
                //console.log(this.tagName + " - ajax-response - newURL: " + this.$.ajax.url + " - status: " + e.detail.xhr.status)
                console.log(this.tagName + " - ajax-response - newURL: "  + this.ajaxOptions.url + " - status: " + xhr.status)
                //this.asyncFire('ajax-response', this.$.ajax.response)
                if(200 == xhr.status) this.asyncFire('ajax-response', xhrResponse)
                else {
                    this.loading = false
                    showMessageVS(xhrResponse.body.innerHTML, '<g:message code="errorLbl"/>')
                }
            },
            ajaxError: function(e) {
                console.log(this.tagName + " - ajaxError")
                if(ResponseVS.SC_PRECONDITION_FAILED == e.detail.xhr.status) {
                    this.loading = false
                    var response = e.detail.xhr.responseText
                    showMessageVS(response, '<g:message code="errorLbl"/>')
                }
            }
        });
    </script>
</polymer-element>

<nav-bar id="navBar" style="display:none;" class="">
    <g:layoutBody/>
</nav-bar>
<div id="loadingDiv" style="width: 30px;margin: 100px auto 0px auto;z-index: 10;">
    <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
</div>

<div layout horizontal center center-justified style="padding:100px 0px 0px 0px;">
    <votingsystem-message-dialog id="_votingsystemMessageDialog"></votingsystem-message-dialog>
</div>
<core-signals id="coreSignals"></core-signals>
<votingsystem-socket id="socketvs" url="${grailsApplication.config.webSocketURL}"></votingsystem-socket>
</body>
</html>
<asset:script>
    var selectedSubsystem = "${selectedSubsystem}"
    console.log("selectedSubsystem : " + selectedSubsystem)
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

    document.addEventListener('votingsystem-signal-innerPage', function(e) {
        console.log('main.gsp -votingsystem-signal-innerPage - newURL: ' + e.detail)
        document.querySelector('#navBar').url = e.detail
    });

    window.addEventListener('WebComponentsReady', function(e) {  });

    document.addEventListener('polymer-ready', function() {
        console.log("main.gsp - polymer-ready")
        update_a_elements(document.getElementsByTagName('a'))
    });

    document.querySelector('#navBar').addEventListener('nav-bar-ready', function(e) {
        document.querySelector('#navBar').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
    });

    document.querySelector('#socketvs').addEventListener('on-message', function(e) {
        console.log("main.gsp - socketvs - message: " + e.detail)
        var socketMessage = e.detail
        if(200 != socketMessage.status) {
            console.log("main.gsp - socketvs - error")
            showMessageVS(socketMessage.message, 'ERROR')
        }
    });

    function sendSocketVSMessage(dataJSON) {
        console.log ("sendSocketVSMessage")
        dataJSON.locale = navigator.language
        document.querySelector("#socketvs").sendMessage(JSON.stringify(dataJSON))
    }

    document.querySelector('#navBar').addEventListener('ajax-response', function(e) {
        var ajaxDocument = e.detail
        var links = ajaxDocument.querySelectorAll('link')
        var numImports = 0
        for (var i = 0; i < links.length; i++) {
            console.log("links[i].innerHTML: " + links[i].href + " - rel: " + links[i].rel)
            if('import' == links[i].rel) {
                ++numImports
                if(i == (links.length - 1)) {
                    links[i].onload = function() {
                      document.querySelector('#navBar').loading = false;
                    };
                }
                document.head.appendChild(links[i]);
            }
        }
        if(numImports == 0) document.querySelector('#navBar').loading = false;

        for (var i = 0; i < ajaxDocument.scripts.length; i++) {
            var script = document.createElement("script");
            script.innerHTML = ajaxDocument.scripts[i].innerHTML;
            console.log("script.src: " + script.src)
            document.head.appendChild(script);
        }
        document.querySelector("#navBar").innerHTML = ajaxDocument.body.innerHTML

        update_a_elements(document.querySelectorAll("#navBar a"))
    });

    function update_a_elements(elementsArray) {
         for (var i = 0; i < elementsArray.length; i++) {
            //console.log("elementsArray[i].href: " + elementsArray[i].href)
            if(elementsArray[i].href.indexOf("${grailsApplication.config.grails.serverURL}") > -1) {
                elementsArray[i].addEventListener('click', function(e) {
                    document.querySelector('#navBar').loadURL(e.target.href)
                    e.preventDefault()
                });
            } else if("" != elementsArray[i].href.trim()) console.log("main.gsp - not system url: " + elementsArray[i].href)
        }
    }
</asset:script>
<asset:deferredScripts/>