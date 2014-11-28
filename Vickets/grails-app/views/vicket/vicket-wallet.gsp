<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-slider', file: 'paper-slider.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-wallet-tag-group']"/>">


<polymer-element name="vicket-wallet"  on-core-select="{{selectAction}}">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .green-slider paper-slider::shadow #sliderKnobInner,
            .green-slider paper-slider::shadow #sliderKnobInner::before,
            .green-slider paper-slider::shadow #sliderBar::shadow #activeProgress {
                background-color: #0f9d58;
            }
            .messageToUser {font-weight: bold;margin:10px auto 30px auto;
                background: #f9f9f9;padding:10px 20px 10px 20px; max-width:400px;
            }
        </style>
        <div vertical layout>
            <template if="{{messageToUser}}">
                <div style="color: {{status == 200?'#388746':'#ba0011'}};">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{status == 200?'#388746':'#ba0011'}};"></core-icon>
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>
            </template>
            <template if="{{!walletLoaded}}">
                <div horizontal layout center center-justified>
                    <paper-button raised on-click="{{showPasswdDialog}}" style="margin: 30px 0px 0px 5px;">
                        <i class="fa fa-money"></i> <g:message code="accessToWalletMsg"/>
                    </paper-button>
                </div>
            </template>
        </div>
        <div horizontal layout center center-justifed>
            <template repeat="{{tag in plainWalletTagArray}}">
                <vicket-wallet-tag-group tag={{tag}} vicketArray="{{plainWalletTagGroups[tag]}}"></vicket-wallet-tag-group>
            </template>
        </div>
        <template repeat="{{tag in tagArray}}">
            <vicket-wallet-tag-group tag={{tag}} vicketArray="{{tagGroups[tag]}}"></vicket-wallet-tag-group>
        </template>
    </template>
    <script>
        Polymer('vicket-wallet', {
            selectedTags: [],
            currencyCode:null,
            plainWallet:null,
            vicketsWalletArray:[],
            tagGroups:{},
            plainWalletTagGroups:{},
            tagArray:[],
            plainWalletTagArray:[],
            messageToUser:null,
            walletLoaded:false,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            domReady: function(){
                var walletStateBase64 = VotingSystemClient.call(new WebAppMessage(Operation.WALLET_STATE));
                var walletState = toJSON((window.atob(walletStateBase64)));
                this.loadPlainWallet(walletState.plainWallet)

                //this.showPasswdDialog()
            },
            showPasswdDialog: function(){
                var webAppMessage = new WebAppMessage(Operation.WALLET_OPEN)
                webAppMessage
                webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        this.loadSecureWallet(appMessageJSON.message)
                    } else {
                        var caption = '<g:message code="errorLbl"/>'
                        showMessageVS(appMessageJSON.message, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            },
            loadPlainWallet:function(vicketsWalletArray) {
                console.log(this.tagName + " - loadPlainWallet")
                this.plainWalletTagGroups = {}
                for(vicketIdx in vicketsWalletArray) {
                    var vicket = vicketsWalletArray[vicketIdx]
                    if(this.plainWalletTagGroups[vicket.tag]) this.plainWalletTagGroups[vicket.tag].push(vicket)
                    else this.plainWalletTagGroups[vicket.tag] = [vicket]
                }
                this.plainWalletTagArray = Object.keys(this.plainWalletTagGroups)
            },
            loadSecureWallet:function(vicketsWalletArray) {
                console.log(this.tagName + " - loadSecureWallet")
                this.tagGroups = {}
                this.vicketsWalletArray = vicketsWalletArray
                for(vicketIdx in this.vicketsWalletArray) {
                    var vicket = this.vicketsWalletArray[vicketIdx]
                    if(this.tagGroups[vicket.tag]) this.tagGroups[vicket.tag].push(vicket)
                    else this.tagGroups[vicket.tag] = [vicket]
                }
                this.tagArray = Object.keys(this.tagGroups)
                this.walletLoaded = true
            }
        });
    </script>
</polymer-element>