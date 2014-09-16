<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/uservs-data']"/>">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">

<polymer-element name="uservs-data-dialog" attributes="">
<template>
    <votingsystem-dialog id="xDialog" class="uservsDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
        <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
            .uservsDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 10px 30px 10px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 800px;
            }
        </style>
        <core-ajax id="ajax" auto on-core-response="{{ajaxResponse}}" response="{{userVS}}" handleAs="json"
                   method="get" contentType="json"></core-ajax>
        <div layout horizontal center center-justified>
            <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                <div style="text-align: center;"><g:message code="userDataDialogCaption"/></div>
            </div>
            <div style="position: absolute; top: 0px; right: 0px; z-index: 10;">
                <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
            </div>
        </div>
        <div style="display:{{isProcessing? 'block':'none'}}">
            <div layout vertical center center-justified  style="text-align:center;min-height: 150px;">
                <progress style="margin:20px auto;"></progress>
            </div>
        </div>
        <uservs-data id="uservsData"></uservs-data>
    </votingsystem-dialog>
</template>

<script>
    Polymer('uservs-data-dialog', {
        isProcessing:false,
        ready: function() {
            this.menuType = menuType
            this.isClientToolConnected = window['isClientToolConnected']
            console.log(this.tagName + " - ready - menuType: " + this.menuType + " - isClientToolConnected: " + isClientToolConnected)
        },
        close: function() {
            this.$.xDialog.opened = false
        },
        ajaxResponse:function() {
            console.log(this.tagName + " - ajaxResponse")
            this.$.uservsData.uservs = this.userVS
            this.isProcessing = false
        },
        showByIBAN:function(IBAN) {
            var serviceURL =  "${createLink( controller:'userVS', action:"index")}?IBAN=" + IBAN
            if(this.$.ajax.url != serviceURL) {
                this.isProcessing = true
                this.$.ajax.url = serviceURL
            }
            this.$.xDialog.opened = true
        },
        show:function(uservs) {
            console.log(this.tagName + " - uservsChanged")
            this.$.uservsData.uservs = uservs
            this.$.xDialog.opened = true
        }
    });

</script>
</polymer-element>
