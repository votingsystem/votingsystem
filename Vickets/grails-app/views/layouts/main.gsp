<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-money.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="polymer.css"/>
    <asset:stylesheet src="vickets.css"/>

    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-navbar', file: 'votingsystem-navbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-icon-button', file: 'paper-icon-button.html')}">

    <g:javascript library="jquery" plugin="jquery"/>
    <asset:stylesheet src="jquery-ui-1.10.4.custom.min.css"/>
    <script type="text/javascript" src="${resource(dir: 'bower_components/jquery-ui', file: 'jquery-ui.min.js')}"></script>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/bootstrap/dist/css', file: 'bootstrap.min.css')}" type="text/css"/>

    <script type="text/javascript" src="${resource(dir: 'bower_components/bootstrap/dist/js', file: 'bootstrap.min.js')}"></script>

    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <g:layoutHead/>
    <style shim-shadowdom>
    paper-item {
        padding: 10px;
        border-bottom: 1px solid #f9f9f9;
    }
    </style>
    <g:layoutHead/>
</head>
<body style="margin:0px auto 0px auto;">
<polymer-element name="nav-bar">

    <template>
        <votingsystem-navbar id="_navbar" style="display: none;">
            <core-header-panel mode="seamed" id="core_header_panel" navigation flex class="navbar-vickets">
                <core-toolbar id="core_toolbar" class="dark-theme" style="background-color: #ba0011;">

                </core-toolbar>
                <core-menu valueattr="label" id="core_menu" theme="core-light-theme" style="font-size: 1.2em;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="data-href" on-core-select="{{drawerItemSelected}}">
                        <g:if test="${"admin".equals(params.menu)}">
                            <paper-item data-href="${createLink(controller: 'transaction', action: 'listener')}">
                                <i class="fa fa-money" style="margin:0px 10px 0px 0px;"></i> <g:message code="transactionsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'groupVS')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="selectGroupvsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'groupVS', action: 'newGroup')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newGroupVSLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action: 'contact')}">
                                <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                            </paper-item>
                            {{ "<g:message code="adminPageTitle"/>" | setTitle}}
                        </g:if>
                        <g:elseif test="${"superadmin".equals(params.menu)}">
                            <paper-item data-href="${createLink(controller: 'transaction', action: 'listener')}">
                                <i class="fa fa-money" style="margin:0px 10px 0px 0px;"></i> <g:message code="transactionsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'newVicketSource')}">
                                <i class="fa fa-university" style="margin:0px 10px 0px 0px;"></i> <g:message code="newVicketSourceLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'addCertificateAuthority')}">
                                <i class="fa fa-certificate" style="margin:0px 10px 0px 0px;"></i> <g:message code="newCAAuthorityLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'certificateVS', action: 'certs')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateCertLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'save')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="newUserCertLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'search')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateUserVSLbl"/>
                            </paper-item>
                            <paper-item id="changeToAdmin" data-href="${createLink(controller: 'app', action: 'contact')}" on-click="{{changeToAdminMenu}}">
                                <g:message code="changeToAdminMenuLbl"/>
                            </paper-item>
                            {{ "<g:message code="superAdminTitle"/>" | setTitle}}
                        </g:elseif>
                        <g:else>
                            <paper-item data-href="${createLink(controller: 'transaction', action: 'listener')}">
                                <i class="fa fa-money" style="margin:0px 10px 0px 0px;"></i> <g:message code="transactionsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'groupVS')}">
                                <i class="fa fa-list" style="margin:0px 10px 0px 0px;"></i> <g:message code="selectGroupvsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'userVS', action: 'search')}">
                                <i class="fa fa-users" style="margin:0px 10px 0px 0px;"></i> <g:message code="locateUserVSLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action: 'contact')}">
                                <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                            </paper-item>
                            {{ "<g:message code="usersPageTitle"/>" | setTitle}}
                        </g:else>

                    </core-selector>
                </core-menu>
            </core-header-panel>
            <div id="appTitle" style="width: 100%;" tool>{{appTitle}}</div>
            <content></content>
        </votingsystem-navbar>
    </template>
    <script>
        Polymer('nav-bar', {
            appTitle:"<g:message code="appTitle"/>",

            ready: function() {
                this.$._navbar.style.display = 'block';
                this.fire('nav-bar-ready');
            },

            drawerItemSelected: function() {
                if('changeToAdmin' == this.$.coreSelector.selectedItem.id) {
                    window.location.href = window.location.href.replace("menu=superadmin", "menu=admin");
                } else  window.location.href = this.coreSelectorValue
            },
            setTitle: function(appTitle) {
                this.appTitle = appTitle
            }
        });
    </script>
</polymer-element>

<div id="navBarDiv" style="display: none;">
    <nav-bar id="navBar" style="" class="">
        <g:layoutBody/>
    </nav-bar>
</div>


<g:include view="/include/dialog/windowAlertModal.gsp"/>
</body>
</html>
<asset:script>
    var navBarDiv = document.querySelector('#navBarDiv')
    navBarDiv.addEventListener('nav-bar-ready', function(e) {
        navBarDiv.style.display = 'block';
    });
    updateMenuLinks()
</asset:script>
<asset:deferredScripts/>