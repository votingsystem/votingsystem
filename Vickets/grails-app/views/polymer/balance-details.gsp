<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-overlay', file: 'core-overlay.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">

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
            padding:30px 42px;
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
<polymer-element name="balance-details">
    <template>
        <x-dialog id="dialog" class="dialog">
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
                padding:30px 42px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);

            }

            #dialog {
                width: 500px;
            }
            </style>
            <div layout vertical style="padding: 0px 10px 0px 20px;" >
                <div layout horizontal style="padding: 0px 10px 0px 20px;" >
                    <h3 id="caption" flex style="color: #6c0404; font-weight: bold;">Detalles del balance</h3>
                    <div style="cursor:pointer; z-index:10;" on-click="{{tapHandler}}">
                        <core-icon-button icon="close" style="fill:#6c0404;"></core-icon-button>
                    </div>
                </div>


            </div>
        </div>
        </x-dialog>

    </template>
    <script>

        Polymer('balance-details', {

            inputHandler: function(e) {
                if (e.target.value === 'something') {
                    this.$.confirmation.toggle();
                }
            },

            tapHandler: function() {
                this.$.dialog.toggle();
            }

        });

    </script>
</polymer-element>