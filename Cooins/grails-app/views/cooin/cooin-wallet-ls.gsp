<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-slider" file="paper-slider.html"/>
<vs:webresource dir="core-tooltip" file="core-tooltip.html"/>
<vs:webresource dir="polymer-localstorage" file="polymer-localstorage.html"/>
<vs:webcomponent path="/cooin/cooin-wallet-tag-group"/>

<!--Test with wallet inside browser localstorage--->
<polymer-element name="cooin-wallet-ls"  on-core-select="{{selectAction}}">
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
        </div>
        <polymer-localstorage id="localstorage" name="cooin-request-localstorage" value="{{cooinsWallet}}"></polymer-localstorage>

        <template repeat="{{tag in tagArray}}">
            <cooin-wallet-tag-group tag={{tag}} cooinArray="{{tagGroups[tag]}}"></cooin-wallet-tag-group>
        </template>
    </template>
    <script>
        Polymer('cooin-wallet-ls', {
            selectedTags: [],
            currencyCode:null,
            cooinsWalletArray:[],
            tagGroups:{},
            tagArray:[],
            messageToUser:null,

            ready: function() {
                console.log(this.tagName + " - ready")
            },
            cooinsWalletChanged:function() {
                console.log(this.tagName + " - cooinsWalletChanged")
                this.tagGroups = {}
                this.cooinsWalletArray = JSON.parse(this.cooinsWallet)
                this.testWallet = JSON.stringify({wallet:this.cooinsWalletArray})

                for(cooinIdx in this.cooinsWalletArray) {
                    var cooin = this.cooinsWalletArray[cooinIdx]
                    if(this.tagGroups[cooin.tag]) this.tagGroups[cooin.tag].push(cooin)
                    else this.tagGroups[cooin.tag] = [cooin]
                }
                this.tagArray = Object.keys(this.tagGroups)
            },
            valueChanged:function() {
                this.amountValue = this.value * 10;
            }
        });
    </script>
</polymer-element>