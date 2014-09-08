<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">

<polymer-element name="eventvs-vote-confirm-dialog" attributes="opened">
    <template>
        <votingsystem-dialog id="xDialog" class="dialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <g:include view="/include/styles.gsp"/>
            <style no-shim> </style>
            <div id="container" layout vertical style="overflow-y: auto; width:450px; padding:10px;">
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="confirmOptionDialogCaption"/></div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <p style="text-align: center;">
                    <g:message code="confirmOptionDialogMsg"/>:<br>
                    <div style="font-size: 1.2em; text-align: center;"><b>{{optionSelected}}</b></div>
                </p>
                <div layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div>
                        <votingsystem-button on-click="{{optionConfirmed}}" style="margin: 0px 0px 0px 5px;">
                            <g:message code="acceptLbl"/> <i class="fa fa-check"></i>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
        </votingsystem-dialog>
    </template>
    <script>
        Polymer('eventvs-vote-confirm-dialog', {
            publish: {
                eventvs: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            onCoreOverlayOpen:function(e) {
                this.opened = this.$.xDialog.opened
            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
                if(this.opened == false) this.close()
            },
            show: function(optionSelected) {
                this.optionSelected = optionSelected
                this.opened = true
            },
            optionConfirmed: function() {
                this.fire('optionconfirmed', this.optionSelected);
                this.opened = false
            },
            close: function() {
                this.opened = false
                this.messageToUser = null
            }
        });
    </script>
</polymer-element>
