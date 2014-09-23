<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/messageSMIME/vicket-transactionvs']"/>">

<polymer-element name="vicket-transactionvs-dialog" attributes="transactionvsURL opened">
    <template>
        <votingsystem-dialog id="xDialog" class="vicketTransactionDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
            .vicketTransactionDialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 1em;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 600px;
                min-height: 300px;
            }
        </style>
        <g:include view="/include/styles.gsp"/>
        <core-ajax id="ajax" auto url="{{transactionvsURL}}" response="{{transactionvs}}" handleAs="json" method="get" contentType="json"></core-ajax>
            <div layout horizontal center center-justified>
                <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;"><g:message code="transactionVSLbl"/></div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                </div>
            </div>
            <div style="display:{{isProcessing? 'block':'none'}}">
                <div layout vertical center center-justified  style="text-align:center;min-height: 150px;">
                    <progress style="margin:20px auto;"></progress>
                </div>
            </div>
            <div>
                <vicket-transactionvs id="transactionViewer"></vicket-transactionvs>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('vicket-transactionvs-dialog', {
            transactionvsURL:null,
            isProcessing:true,
            ready: function() {
                console.log(this.tagName + " - " + this.id + " - ready")
                this.isClientToolConnected = window['isClientToolConnected']
            },
            transactionvsChanged:function() {
                this.transactionvsURL = ""
                this.$.transactionViewer.signedDocument = this.transactionvs.signedContentMap
                this.$.transactionViewer.timeStampDate = this.transactionvs.timeStampDate
                this.$.transactionViewer.smimeMessage = this.transactionvs.smimeMessage
                this.isProcessing = false
                this.$.xDialog.opened = true
            },
            show: function(transactionvsURL) {
                this.isProcessing = true
                this.transactionvsURL = transactionvsURL
                console.log(this.tagName + " - show - transactionvsURL:" + this.transactionvsURL)
                this.$.xDialog.opened = true
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>