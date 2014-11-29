<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-slider', file: 'paper-slider.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/cooin/cooin-wallet-tag-group']"/>">


<polymer-element name="cooin-wallet"  on-core-select="{{selectAction}}">
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
        <div  layout horizontal center-justified>
            <template repeat="{{tag in plainWalletTagArray}}">
                <cooin-wallet-tag-group tag={{tag}} cooinArray="{{plainWalletTagGroups[tag]}}"
                                         style="margin:10px 10px 0px 0px; border: 1px solid #ccc;"></cooin-wallet-tag-group>
            </template>
        </div>
        <template repeat="{{tag in tagArray}}">
            <cooin-wallet-tag-group tag={{tag}} cooinArray="{{tagGroups[tag]}}"></cooin-wallet-tag-group>
        </template>
    </template>
    <script>
        Polymer('cooin-wallet', {
            selectedTags: [],
            currencyCode:null,
            plainWallet:null,
            cooinsWalletArray:[],
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
                var walletState = toJSON(VotingSystemClient.call(new WebAppMessage(Operation.WALLET_STATE)));
                this.loadPlainWallet(walletState.plainWallet)

                //this.showPasswdDialog()
            },
            showPasswdDialog: function(){
                var webAppMessage = new WebAppMessage(Operation.WALLET_OPEN)
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
            loadPlainWallet:function(cooinsWalletArray) {
                console.log(this.tagName + " - loadPlainWallet")
                this.plainWalletTagGroups = {}
                for(cooinIdx in cooinsWalletArray) {
                    var cooin = cooinsWalletArray[cooinIdx]
                    if(this.plainWalletTagGroups[cooin.tag]) this.plainWalletTagGroups[cooin.tag].push(cooin)
                    else this.plainWalletTagGroups[cooin.tag] = [cooin]
                }
                this.plainWalletTagArray = Object.keys(this.plainWalletTagGroups)
            },
            loadSecureWallet:function(cooinsWalletArray) {
                console.log(this.tagName + " - loadSecureWallet")
                this.tagGroups = {}
                this.cooinsWalletArray = cooinsWalletArray
                for(cooinIdx in this.cooinsWalletArray) {
                    var cooin = this.cooinsWalletArray[cooinIdx]
                    if(this.tagGroups[cooin.tag]) this.tagGroups[cooin.tag].push(cooin)
                    else this.tagGroups[cooin.tag] = [cooin]
                }
                this.tagArray = Object.keys(this.tagGroups)
                this.walletLoaded = true
            }
        });
    </script>
</polymer-element>