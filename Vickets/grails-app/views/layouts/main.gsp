<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-money.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="vickets.css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-navbar', file: 'votingsystem-navbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-button', file: 'votingsystem-button.html')}">

    <link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/votingsystem-message-dialog.gsp']"/>">

    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto;background-color: #f9f9f9;">
<polymer-element name="nav-bar" attributes="url loading">
    <template>
        <core-ajax id="ajax" auto url="" handleAs="document" on-core-response="{{ajaxResponse}}"></core-ajax>
        <!-- put core signals names in lower case !!!-->
        <core-signals on-core-signal-innerpage="{{innerPageSignal}}"></core-signals>
        <votingsystem-navbar id="_navbar" style="display: none;">
            <core-header-panel mode="seamed" id="core_header_panel" navigation flex class="navbar-vickets">
                <core-toolbar id="core_toolbar" style="background-color: #ba0011;">
                </core-toolbar>
                <core-menu valueattr="label" id="core_menu" theme="core-light-theme" style="font-size: 1.2em;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="data-href" on-core-select="{{drawerItemSelected}}">
                        <paper-item data-href="${createLink(controller: 'transaction', action: 'listener')}">
                            <i class="fa fa-money" style="margin:0px 10px 0px 0px;"></i> <g:message code="transactionsLbl"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'groupVS')}">
                            <i class="fa fa-list" style="margin:0px 10px 0px 0px;"></i> <g:message code="selectGroupvsLbl"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'userVS', action: 'search')}">
                            <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateUserVSLbl"/>
                        </paper-item>
                        <g:if test="${"admin".equals(params.menu)}">
                            <paper-item data-href="${createLink(controller: 'groupVS', action:'newGroup')}"">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newGroupVSLbl"/>
                            </paper-item>
                            {{ "<g:message code="adminPageTitle"/>" | setTitle}}
                        </g:if>
                        <g:elseif test="${"superadmin".equals(params.menu)}">
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'newVicketSource')}">
                                <i class="fa fa-university" style="margin:0px 10px 0px 0px;"></i> <g:message code="newVicketSourceLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'addCertificateAuthority')}"
                                        style="padding:30px 10px 30px 10px;">
                                <i class="fa fa-certificate" style="margin:0px 10px 0px 0px;"></i> <g:message code="newCAAuthorityLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'certs')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateCertLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'save')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newUserCertLbl"/>
                            </paper-item>
                            <paper-item id="changeToAdmin" data-href="${createLink(controller: 'app', action: 'contact')}" on-click="{{changeToAdminMenu}}"
                                        style="padding:30px 10px 30px 10px;">
                                <g:message code="changeToAdminMenuLbl"/>
                            </paper-item>
                            {{ "<g:message code="superAdminTitle"/>" | setTitle}}
                        </g:elseif>
                        <g:else>
                            {{ "<g:message code="usersPageTitle"/>" | setTitle}}
                        </g:else>
                        <paper-item data-href="${createLink(controller: 'reports', action:'index')}"">
                            <i class="fa fa-list-alt" style="margin:0px 10px 0px 0px;"></i> <g:message code="reportsPageTitle"/>
                        </paper-item>
                        <paper-item data-href="${createLink(controller: 'app', action: 'contact')}">
                            <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                        </paper-item>
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
            ready: function() {
                this.$._navbar.searchVisible(false)
                this.$._navbar.style.display = 'block';
                this.fire('nav-bar-ready');
                var navBar = this
                window.addEventListener('popstate', function(event) {
                    navBar.url = document.location.href
                });
            },
            innerPageSignal:function(e, detail, sender) {
                this.url = detail;
            },
            urlChanged: function() {
                if(this.url != null) {
                    this.loading= true;
                    history.pushState(null, null, this.url);
                    this.$.ajax.url =  updateMenuLink(this.url, "mode=innerPage")
                }
            },
            drawerItemSelected: function() {
                this.fire('item-selected', this.coreSelectorValue)
                if(this.$.coreSelector.selectedItem != null && 'changeToAdmin' == this.$.coreSelector.selectedItem.id) {
                    window.location.href = window.location.href.replace("menu=superadmin", "menu=admin");
                } else {
                    this.url = this.coreSelectorValue
                    this.coreSelectorValue = null
                }
            },
            searchVisible: function(isVisible) {
                this.$._navbar.searchVisible(isVisible)
            },
            setTitle: function(appTitle) {
                this.appTitle = appTitle
            },
            ajaxResponse: function(appTitle) {
                console.log(this.tagName + " - ajax-response - newURL: " + this.url)
                this.asyncFire('ajax-response', this.$.ajax.response)
                this.url = null
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
</body>
</html>
<asset:script>

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

    document.querySelector('#navBar').addEventListener('ajax-response', function(e) {
        var ajaxDocument = e.detail
        var links = ajaxDocument.querySelectorAll('link')
        for (var i = 0; i < links.length; i++) {
            console.log("links[i].innerHTML: " + links[i].href + " - rel: " + links[i].rel)
            if('import' == links[i].rel) {
                if(i == (links.length - 1)) {
                    links[i].onload = function() {
                      document.querySelector('#navBar').loading = false;
                    };
                }
                document.head.appendChild(links[i]);
            }
        }
        if(links.length == 0) document.querySelector('#navBar').loading = false;

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
                    document.querySelector('#navBar').url = e.target.href
                    e.preventDefault()
                });
            } else if("" != elementsArray[i].href.trim()) console.log("main.gsp - not system url: " + elementsArray[i].href)
        }
    }

</asset:script>
<asset:deferredScripts/>