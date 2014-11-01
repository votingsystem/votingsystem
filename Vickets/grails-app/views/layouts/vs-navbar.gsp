<link rel="import" href="${resource(dir: '/bower_components/core-toolbar', file: 'core-toolbar.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-drawer-panel', file: 'core-drawer-panel.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-header-panel', file: 'core-header-panel.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-radio-group', file: 'paper-radio-group.html')}">

<polymer-element name="vs-navbar" attributes="responsiveWidth mode sessionData userVS">
<template>
    <g:include view="/include/styles.gsp"/>
  <style>
    [drawer] { background-color: #ba0011; color: #f9f9f9; box-shadow: 1px 0 1px rgba(0, 0, 0, 0.1); }
    [main] { height: 100%; background-color: #fefefe;  }
    core-toolbar { background-color: #ba0011; color: #fff; }
    core-header-panel #mainContainer { height: 1000px; }
    #drawerPanel:not([narrow]) #menuButton { display: none; }
    icon-button.white  { fill: #f9f9f9; }
    #drawer { width: 300px; }
    .userInfoPanel { border: 1px solid #6c0404; padding: 10px; background: #f9f9f9; width: 300px; color: #888;
        font-size: 1em; padding:30px 20px 20px 20px; margin:20px 0 0 0;
    }
    ::shadow #tooltip { padding: 0px; background: #f9f9f9; font-size: 1.2em; top:200px;}
    ::shadow #control {color:#f9f9f9;}
    paper-dialog::shadow #main {padding:10px 24px 0px 24px;}
    paper-dialog::shadow h1 {color:#6c0404;}
  </style>
  <core-drawer-panel id="coreDrawerPanel" narrow="{{narrow}}" responsiveWidth="{{responsiveWidth}}" >
    <div id="drawerItems" vertical layout drawer style="">
        <content select="[navigation], nav"></content>
    </div>
    <core-header-panel id="mainHeaderPanel" main mode="{{mode}}" style="">
      <core-toolbar id="coreToolbar">
        <core-icon-button id="menuButton" icon="menu" on-tap="{{togglePanel}}"></core-icon-button>
        <content select="[tool]"></content>
        <core-icon-button id="searchButton" icon="search" on-tap="{{toogleSearchPanel}}" style="fill: #f9f9f9;"></core-icon-button>
        <div horizontal layout style="font-size: 0.8em;">
            <div flex></div>
            <div horizontal layout center center-justified style=" margin:0 30px 0 0; width: 250px;">
                <core-icon-button id="connectButton" icon="{{connecButtonIcon}}" on-click="{{connectButtonClicked}}">
                    <span id="connectButtonDiv">{{connectButtonLbl}}</span>
                </core-icon-button>
                <core-tooltip id="userInfoPanel" style="padding: 0px; font-size: 0.9em;" position="left">
                    <div vertical layout class="userInfoPanel" tip>
                        <div style="height: 30px;"><g:message code="connectedWithLbl"/>: {{userVS.nif}}</div>
                        <paper-button raised label="<g:message code="disConnectLbl"/>" affirmative autofocus
                                      on-click="{{disConnect}}">
                        </paper-button>
                    </div>
                </core-tooltip>
            </div>
        </div>
      </core-toolbar>
      <content select="*"></content>
    </core-header-panel>
  </core-drawer-panel>
    <div id="searchPanel" class="" style="position: absolute;top:70px; left: 40%; background:#ba0011;
            padding:10px 10px 10px 10px;display:none; z-index: 10;">
        <input id="searchInput" type="text" placeholder="Search"
               style="width:160px; border-color: #f9f9f9;display:inline; vertical-align: middle;">
        <i id="searchPanelCloseIcon" on-click="{{toogleSearchPanel}}" class="fa fa-times text-right navBar-vicket-icon"
           style="margin:0px 0px 0px 15px; display:inline;vertical-align: middle;"></i>
    </div>
    <paper-dialog id="userSelectorDialog" heading="<g:message code="selectUserVSLbl"/>" style="max-width: 400px;
            padding: 0px 0 0 0;">
            <div horizontal layout center center-justified>
                <template if="{{userVSList.length >0}}">
                    <paper-radio-group id="userSelector" selected="{{lastSessionNif}}">
                        <template repeat="{{user in userVSList}}">
                            <paper-radio-button name="{{user.nif}}" label="{{user.nif}}" style="margin:0 0 10px 0;"></paper-radio-button><br/>
                        </template>
                    </paper-radio-group>
                </template>
                <template if="{{userVSList == null || userVSList.length === 0}}">
                    <div style="margin: 20px"><g:message code="certNeededMsg"/></div>
                </template>
            </div>
        <div horizontal layout style="font-size: 0.9em; padding: 3px">
            <paper-button raised label="<g:message code="addCertUserVSLbl"/>" affirmative autofocus
                          on-click="{{selectCertificate}}" style="color: #008000; margin:0 20px 0 0;">
                <i class="fa fa-certificate"></i>
            </paper-button>
            <paper-button raised label="<g:message code="requestCertLbl"/>" affirmative autofocus
                          on-click="{{requestCert}}" style="color: #008000; margin:0 20px 0 0;">
                <i class="fa fa-download"></i>
            </paper-button>
            <div flex></div>
            <template if="{{userVSList.length > 0}}">
                <paper-button raised label="<g:message code="acceptLbl"/>" affirmative autofocus
                              on-click="{{connect}}" style="color: #008000; margin:0 0 0 30px;">
                    <i class="fa fa-check" style="margin:0px 10px 0px 0px;"></i>
                </paper-button>
            </template>
        </div>
    </paper-dialog>
</template>
<script>
  Polymer('vs-navbar', {
    sessionData:null,
    responsiveWidth: '100000px',// 100000px -> Always closed
    mode: 'seamed', //Used to control the header and scrolling behaviour of `core-header-panel`
    isConnected:false,
    lastSessionNif:null,
    connecButtonIcon: "settings-remote",
    connectButtonLbl:"<g:message code="connectLbl"/>",
    ready: function() {
        window.onclick = function(e){
            this.$.userInfoPanel.show = false
        }.bind(this)
        this.$.userInfoPanel.onclick = function(e){
            e.stopPropagation();
        }
        this.$.connectButton.onclick = function(e){
            e.stopPropagation();
        }
    },
    requestCert:function() {
        window.open(window['accessControlURL'] + "/certificateVS/certRequest", "_blank");
    },
    disConnect: function(e) {
        this.$.userInfoPanel.show = false
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.DISCONNECT)
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    },
    selectCertificate: function(e) {
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.KEYSTORE_SELECT)
        webAppMessage.setCallback(function(appMessage) {
            var appMessageJSON = JSON.parse(appMessage)
            if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                if(this.userVSList != null && this.userVSList.length > 1) this.userVSList.push(appMessageJSON)
                else this.userVSList = [appMessageJSON]
            } else showMessageVS(appMessageJSON.message, '<g:message code="transactionvsERRORLbl"/>')
        }.bind(this))
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    },
    connect: function(e) {
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CONNECT)
        var selectedUser = null
        if(this.userVSList != null && this.userVSList.length > 1) {
            Array.prototype.forEach.call(this.userVSList, function(userVS) {
                if(userVS.nif === this.$.userSelector.selected) {
                    selectedUser = userVS
                }
            }.bind(this));
        } else if(this.userVSList != null && this.userVSList.length === 1) selectedUser = this.userVSList[0]
        if(selectedUser != null) {
            webAppMessage.document = selectedUser
            console.log(this.tagName + " - connect - userVS: " + selectedUser.nif)
        }
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    },
    connectButtonClicked: function(e) {
        if(this.userVSList != null) console.log(this.tagName + " - connectButtonClicked: " + this.userVSList.length +
            JSON.stringify(this.userVSList))
        else  console.log(this.tagName + " - connectButtonClicked")
        if(this.isConnected) {
            this.$.userInfoPanel.show = true
        } else {
            if(this.userVSList != null && this.userVSList.length === 1) {
                this.connect()
            } else this.$.userSelectorDialog.toggle()
        }
    },
    updateSession:function(userVS, sessionData) {
        this.sessionData = sessionData
        this.userVS = userVS
        if(this.sessionData != null) {
            this.userVSList = this.sessionData.userVSList
            if(this.sessionData.userVS != null) this.lastSessionNif = this.sessionData.userVS.nif
            if(this.sessionData.isConnected && this.sessionData.userVS != null) this.userVS = this.sessionData.userVS
        }
        if(this.userVS != null) this.isConnected = true
        else this.isConnected = false
        if(this.isConnected) {
            this.connectButtonLbl = this.userVS.nif
            this.connecButtonIcon ="account-circle"
        } else {
            this.connectButtonLbl = "<g:message code="connectLbl"/>"
            this.connecButtonIcon ="settings-remote"
        }
        console.log(this.tagName + " - updateUserStatus - userVS: " + userVS + " - sessionData: " + sessionData +
                " - isConnected: " + this.isConnected)
    },
    togglePanel: function() {
        this.$.coreDrawerPanel.togglePanel();
    },
    openDrawer: function() {
      this.$.coreDrawerPanel.openDrawer();
    },
    searchVisible: function(isVisible) {
        if(isVisible) this.$.searchButton.style.display = 'block'
        else this.$.searchButton.style.display = 'none'
    },
    closeDrawer: function() {
        this.$.coreDrawerPanel.closeDrawer();
    },
    toogleSearchPanel:function () {
        if('block' == this.$.searchPanel.style.display) this.$.searchPanel.style.display = 'none'
        else this.$.searchPanel.style.display = 'block'
    }
  });
</script>
</polymer-element>
