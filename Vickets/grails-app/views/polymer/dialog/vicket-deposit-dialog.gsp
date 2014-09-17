<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/vicket-deposit-form']"/>">


<polymer-element name="vicket-deposit-dialog">
<template>
    <votingsystem-dialog id="xDialog" class="vicketDepositDialog" opened="{{opened}}" on-core-overlay-open="{{onCoreOverlayOpen}}">
        <style no-shim>
            .vicketDepositDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: #fefefe;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 650px;
            }
        </style>
        <div id="container">
            <div layout horizontal center center-justified>
                <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                    <votingsystem-html-echo html="{{caption}}"></votingsystem-html-echo>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                </div>
            </div>
            <vicket-deposit-form id="depositForm"></vicket-deposit-form>
        </div>

    </votingsystem-dialog>
</template>
<script>
    Polymer('vicket-deposit-dialog', {
        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.$.depositForm.addEventListener('operation-finished', function (e) {
                this.close()
            }.bind(this))
        },
        openedChanged:function() {
            if(!this.opened) this.close()
        },
        close: function(e) {
            console.log(this.tagName + " - close")
            if(e) console.log(this.tagName + " - close - e.detail: " + JSON.stringify( e.detail))
            this.$.xDialog.opened = false
            this.$.depositForm.reset()
        },
        show:function(operation, fromUser, fromIBAN, validTo, targetGroupId) {
            console.log(this.id + " - show - operation: " + operation)
            this.caption = this.$.depositForm.init(operation, fromUser, fromIBAN, validTo, targetGroupId, true)
            this.$.xDialog.opened = true
        }
    });
</script>
</polymer-element>
