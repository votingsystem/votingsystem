<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">


<polymer-element name="x-dialog" attributes="opened autoCloseDisabled">
    <template>
        <style>
        :host {
            box-sizing: border-box;
            -moz-box-sizing: border-box;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 13px;
            -webkit-user-select: none;
            -moz-user-select: none;
            overflow: hidden;
            background: white;
            outline: 1px solid rgba(0,0,0,0.2);
            box-shadow: 0 4px 16px rgba(0,0,0,0.2);
        }
        </style>
        <core-overlay id="overlay" layered backdrop opened="{{opened}}" autoCloseDisabled="{{autoCloseDisabled}}" ></core-overlay>
        <content></content>
    </template>
    <script>
        Polymer('x-dialog', {
            ready: function() {
                this.$.overlay.target = this;
            },
            toggle: function() {
                this.$.overlay.toggle();
                this.fire('core-signal', {name: "balance-details-closed", data: this.callerId});
            }
        });
    </script>
</polymer-element>


<!-- an element that uses the x-dialog element and core-overlay -->
<polymer-element name="balance-details" attributes="opened balance">
    <template>
        <x-dialog id="xDialog" class="dialog">
            <!-- place all overlay styles inside the overlay target -->
            <style no-shim>
            .dialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 500px;
            }
            </style>
            <div layout vertical style="" >
                <div layout horizontal center center-justified style="" >
                    <h3 id="caption" flex style="color: #6c0404; font-weight: bold;"><g:message code="userBalanceLbl"/></h3>
                    <div style="cursor:pointer;" on-click="{{tapHandler}}">
                        <core-icon-button icon="close" style="fill:#6c0404;color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div>
                    {{balance.nif}}
                </div>
                <div layout horizontal>
                    <div layout vertical>
                        <g:message code="incomesLbl"/>
                        <template repeat="{{transactionFrom in balance.transactionFromList}}">
                            {{transactionFrom.amount}}
                        </template>
                    </div>
                    <div layout vertical>
                        <g:message code="expensesLbl"/>
                        <template repeat="{{transactionFrom in balance.transactionFromList}}">
                            {{transactionFrom.amount}}
                        </template>
                    </div>
                </div>



            </div>
        </div>
        </x-dialog>

    </template>
    <script>

        Polymer('balance-details', {
            ready: function(e) {
            },
            balanceChanged: function() {

            },
            openedChanged:function() {
                this.$.xDialog.opened = this.opened
            },
            tapHandler: function() {
                this.$.xDialog.toggle();
            }
        });

    </script>
</polymer-element>