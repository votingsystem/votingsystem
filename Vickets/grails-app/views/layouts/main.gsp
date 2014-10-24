<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-credit-card.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="vickets.css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/vs-navbar', file: 'vs-navbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/vs-socket', file: 'vs-socket.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/alert-dialog.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/vs-texteditor', file: 'vs-texteditor.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/vs-innerpage-signal', file: 'vs-innerpage-signal.html')}">

    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto;">
<polymer-element name="nav-bar" attributes="url loading">
    <template>
        <g:include view="/include/styles.gsp"/>
        <!--<core-ajax id="ajax" auto on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}" handleAs="document"></core-ajax>-->
        <core-xhr id="ajax" ></core-xhr>
        <!-- put core signals names in lower case !!!-->
        <core-signals on-core-signal-votingsystem-innerpage="{{innerPageSignal}}"></core-signals>
        <vs-navbar id="_navbar" style="display: none;">
            <core-header-panel mode="seamed" id="core_header_panel" navigation flex class="navbar-vickets">
                <core-toolbar id="core_toolbar" style="background-color: #ba0011;">
                </core-toolbar>
                <core-menu valueattr="label" id="core_menu" theme="core-light-theme" style="font-size: 1.2em;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="data-href" on-core-select="{{drawerItemSelected}}">
                        <paper-item data-href="${createLink(controller: 'groupVS', absolute: true)}">
                            <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="selectGroupvsLbl"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'userVS', action: 'search', absolute: true)}">
                            <i class="fa fa-user" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateUserVSLbl"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'vicket', action:'request', absolute: true)}">
                            <i class="fa fa-money" style="margin:0px 10px 0px 0px;"></i> <g:message code="doVicketRequestLbl"/>
                        </paper-item>
                        <g:if test="${"admin".equals(params.menu)}">
                            <template if="{{isClientToolConnected}}">
                                <paper-item data-href="${createLink(controller: 'groupVS', action:'newGroup', absolute: true)}"">
                                    <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newGroupVSLbl"/>
                                </paper-item>
                            </template>
                            {{ "<g:message code="adminPageTitle"/>" | setTitle}}
                        </g:if>
                        <g:elseif test="${"superuser".equals(params.menu)}">
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'newBankVS', absolute: true)}">
                                <i class="fa fa-university" style="margin:0px 10px 0px 0px;"></i> <g:message code="newBankVSLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'addCertificateAuthority', absolute: true)}"
                                        style="margin:0px 10px 0px 0px;">
                                <i class="fa fa-certificate" style="margin:0px 10px 0px 0px;"></i> <g:message code="newCAAuthorityLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'certs', absolute: true)}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateCertLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'save', absolute: true)}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newUserCertLbl"/>
                            </paper-item>
                            <paper-item id="changeToAdmin" data-href="${createLink(controller: 'app', action: 'contact', absolute: true)}" on-click="{{changeToAdminMenu}}">
                                <i class="fa fa-exchange" style="margin:0px 10px 0px 0px;"></i><g:message code="changeToAdminMenuLbl"/>
                            </paper-item>
                            {{ "<g:message code="superUserTitle"/>" | setTitle}}
                        </g:elseif>
                        <g:else>
                            {{ "<g:message code="usersPageTitle"/>" | setTitle}}
                        </g:else>
                        <paper-item data-href="${createLink(controller: 'transactionVS', absolute: true)}">
                            <i class="fa fa-line-chart" style="margin:0px 10px 0px 0px;"></i> <g:message code="transactionsLbl"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'reports', action:'index', absolute: true)}">
                            <i class="fa fa-list-alt" style="margin:0px 10px 0px 0px;"></i> <g:message code="reportsPageTitle"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'app', action: 'contact', absolute: true)}">
                            <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                        </paper-item>
                    </core-selector>
                </core-menu>
            </core-header-panel>
            <div id="appTitle" style="font-size:1.5em;width: 100%; text-align: center;" tool>{{appTitle}}</div>
            <content id="content"></content>
        </vs-navbar>
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
                //window.addEventListener("popstate", function(e) {  });
            },
            urlChanged: function() { //for history navigation
                this.loadURL(this.url)
            },
            innerPageSignal:function(e, detail, sender) {
                console.log("innerPageSignal - title:" + detail.title + " - url: " + detail.url)
                var sufix = ""
                if('admin' === menuType) sufix = ' - <g:message code="adminLbl"/>'
                if('superuser' === menuType) sufix = ' - <g:message code="superUserLbl"/>'
                if(detail.title) this.appTitle = detail.title + sufix
                if(detail.searchVisible) this.$._navbar.searchVisible(detail.searchVisible)
                if(detail.url) this.url = detail.url
                document.dispatchEvent( new Event('innerPageSignal'));
            },
            loadURL: function(urlToLoad) {
                this.loading= true;
                //history.pushState(null, null, this.url);
                history.pushState(null, null, urlToLoad);
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
                        window.location.href = window.location.href.replace("menu=superuser", "menu=admin");
                    } else {
                        this.loadURL(this.coreSelectorValue)
                    }
                    this.coreSelectorValue = null
                }
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
    <alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
</div>
<core-signals id="coreSignals"></core-signals>
<!--<vs-socket id="socketvs" socketservice="${grailsApplication.config.webSocketURL}"></vs-socket>-->
</body>
</html>
<asset:script>
    window.addEventListener('WebComponentsReady', function(e) {  });
    document.querySelector('#coreSignals').addEventListener('core-signal-votingsystem-innerpage', function(e) {
        console.log("simplePage.gsp - innerPageSignal: " + e.detail)
        var message = e.detail

    });
    document.addEventListener('polymer-ready', function() {
        console.log("main.gsp - polymer-ready")
        updateLinksVS(document.getElementsByTagName('a'))
    });

    document.querySelector('#navBar').addEventListener('nav-bar-ready', function(e) {
        document.querySelector('#navBar').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
    });


    if(document.querySelector('#socketvs')) document.querySelector('#socketvs').addEventListener('on-message', function(e) {
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

        updateLinksVS(document.querySelectorAll("#navBar a"))
    });
</asset:script>
<asset:deferredScripts/>