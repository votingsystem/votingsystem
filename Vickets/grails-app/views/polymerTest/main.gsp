<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-money.png')}" type="image/x-icon">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="polymer.css"/>
    <asset:stylesheet src="vickets.css"/>

    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-navbar', file: 'votingsystem-navbar.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-item', file: 'paper-item.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-icon-button', file: 'paper-icon-button.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
    <style shim-shadowdom>
        paper-item {
            padding: 10px;
            border-bottom: 1px solid #f9f9f9;
        }
    </style>
</head>
<body style="margin:0px auto 0px auto;">
<polymer-element name="nav-bar">

    <template>
        <votingsystem-navbar>
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
            <div>Content goes here...

                <div class="card">

                <div id="">
                    <div class="content">

                        <div layout horizontal style="z-index: 20;">
                            <div>
                                <label center center-justified>{{messages.fromDate}}: </label>
                                <paper-input id="dateFrom" value={{dateFromValue}} on-input="{{validateForm}}" floatinglabel label="Fecha (dd/mm/aaaa)"
                                             validate="^(?:(?:31(\/|-|\.)(?:0?[13578]|1[02]))\1|(?:(?:29|30)(\/|-|\.)(?:0?[1,3-9]|1[0-2])\2))(?:(?:1[6-9]|[2-9]\d)?\d{2})$|^(?:29(\/|-|\.)0?2\3(?:(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\d|2[0-8])(\/|-|\.)(?:(?:0?[1-9])|(?:1[0-2]))\4(?:(?:1[6-9]|[2-9]\d)?\d{2})$"
                                             error="No es una fecha válida!" style="width:170px; margin:0px 10px 0px 10px;">
                                </paper-input>
                            </div>
                            <div>
                                <paper-input id="hourFrom" value={{dateFromHour}} on-input="{{validateForm}}" floatinglabel label="Hora (HH:mm)"
                                             validate="^([01]?[0-9]|2[0-3]):[0-5][0-9]$"
                                             error="No es una hora válida!" style="width:120px;margin:0px 0px 0px 0px;">
                                </paper-input>
                            </div>
                        </div>

                    </div>


                    <div layout horizontal center center-justified style="margin:10px 0px 0px 0px;">
                        <paper-input id="searchInputMessage" value="{{searchInputMessageValue}}" label="{{messages.searchInputMessage}}" on-input="{{validateForm}}"></paper-input>
                    </div>
                </div>
                    <paper-ripple class="recenteringTouch" fit></paper-ripple>
                </div>

            </div>
        </votingsystem-navbar>
    </template>
    <script>
        Polymer('nav-bar', {
            appTitle:"<g:message code="appTitle"/>",

            ready: function() { },

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

<nav-bar id="navBar" style="display:none;" class=""></nav-bar>

</body>
</html>
<asset:script>
    document.querySelector('#navBar').style.display = 'block';
</asset:script>
<asset:deferredScripts/>