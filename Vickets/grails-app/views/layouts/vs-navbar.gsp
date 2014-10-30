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
    :host {
        display: block;
    }
    [drawer] {
        background-color: #ba0011;
        color: #f9f9f9;
        box-shadow: 1px 0 1px rgba(0, 0, 0, 0.1);
    }
    [main] { height: 100%; background-color: #fefefe;  }
    core-toolbar { background-color: #ba0011; color: #fff; }
    core-header-panel #mainContainer { height: 1000px; }
    #drawerPanel:not([narrow]) #menuButton { display: none; }
    icon-button.white  { fill: #f9f9f9; }
    #drawer { width: 300px; }
    .userInfoPanel { border: 1px solid #6c0404; padding: 10px; background: #fff; width: 300px; color: #888; font-size: 1em;
    }
    ::shadow #tooltip { padding: 0px; background: #f9f9f9; font-size: 1.2em; margin:0 20px 0 0;}
    ::shadow #arrow {fill:#f9f9f9;}
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
        <content select="[tool]">
        </content>
        <core-icon-button id="searchButton" icon="search" on-tap="{{toogleSearchPanel}}" style="fill: #f9f9f9;"></core-icon-button>
        <div horizontal layout style="font-size: 0.8em;">
            <div flex></div>
            <div horizontal layout center center-justified style=" margin:0 30px 0 0; width: 250px;">
                <core-icon-button id="connectButton" icon="{{connecButtonIcon}}" on-click="{{connectButtonClicked}}">
                    <span id="connectButtonDiv">{{connectButtonLbl}}</span>
                </core-icon-button>
                <core-tooltip id="userInfoPanel" style="padding: 0px;"position="left">
                    <div vertical layout class="userInfoPanel" tip>
                        <div style="height: 30px; color: #008000;">userVS: {{userVS.nif}}</div>
                        <button>Desconectar</button>
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
                <paper-radio-group id="userSelector" selected="{{userVS.nif}}">
                    <template repeat="{{user in userVSList}}">
                        <paper-radio-button name="{{user.nif}}" label="{{user.nif}}" style="margin:0 0 10px 0;"></paper-radio-button><br/>
                    </template>
                </paper-radio-group>
            </div>
        <div horizontal layout style="font-size: 0.9em; padding: 3px">
            <div flex></div>
            <paper-button raised label="<g:message code="acceptLbl"/>" affirmative autofocus
                    on-click="{{connect}}" style="color: #008000; margin:0 0 0 30px;">
                <i class="fa fa-check" style="margin:0px 10px 0px 0px;"></i>
            </paper-button>
        </div>
    </paper-dialog>
</template>
<script>
  Polymer('vs-navbar', {
    // 100000px -> Always closed
      sessionData:null,
    responsiveWidth: '100000px',
    //Used to control the header and scrolling behaviour of `core-header-panel`
    mode: 'seamed',
    isConnected:false,
    connecButtonIcon: "settings-remote",
    connectButtonLbl:"<g:message code="connectLbl"/>",
    userVS:{id: '4', nif: '7553172H'},
    userVSList: [
          {id: '4', nif: '7553172H'},
          {id: '5', nif: '111111A'}],
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
    connect: function(e) {
        var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.CONNECT)
        if(this.userVSList && this.userVSList.length > 1) {
            Array.prototype.forEach.call(this.userVSList, function(userVS) {
                if(userVS.nif === this.$.userSelector.selected) {
                    this.userVS = userVS
                }
            }.bind(this));
        }
        webAppMessage.document = this.userVS
        console.log(this.tagName + " - connect - userVS: " + this.userVS.nif)
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    },
    connectButtonClicked: function(e) {
        console.log(this.tagName + " - connectButton")
        if(this.isConnected) {
            this.$.userInfoPanel.show = true
        } else {
            if(this.userVSList && this.userVSList.length > 1) {
                this.$.userSelectorDialog.toggle()
            } else this.connect()
        }
    },
    userVSChanged: function() {
        if(this.sessionData != null) this.sessionData.userVS = this.userVS
        console.log(this.tagName + " - userVSChanged - sessionData: " + this.sessionData)
    },
    sessionDataChanged: function() {
        this.userVS = this.sessionData.userVS
        this.userVSList = this.sessionData.userVSList
        if(this.userVS != null) {
            this.sessionData.isConnected = true
            if(this.userVSList == null || this.userVSList.length === 0) this.userVSList = [this.userVS]
        }
        this.isConnected = this.sessionData.isConnected
        if(this.isConnected) {
            this.connectButtonLbl = this.userVS.nif
            this.connecButtonIcon ="account-circle"
        } else this.connecButtonIcon ="settings-remote"
        console.log(this.tagName + " - sessionDataChanged - isConnected: " + this.isConnected)
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
