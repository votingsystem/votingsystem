<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="core-tooltip" file="core-tooltip.html"/>
<vs:webresource dir="paper-radio-button" file="paper-radio-button.html"/>
<vs:webresource dir="core-icon" file="core-icon.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="vs-currency-selector" file="vs-currency-selector.html"/>
<vs:webcomponent path="/tagVS/tagvs-select-dialog"/>
<vs:webcomponent path="/cooin/cooin-request-result-dialog"/>
<vs:webresource dir="paper-input" file="paper-input.html"/>

<polymer-element name="cooin-request-form">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 30px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
                max-width:400px;
            }
            paper-fab.green { background: #259b24; color: #f0f0f0; }
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

            <div layout horizontal center center-justified>
                <div style="margin: 0 10px 0 0;"><paper-radio-button toggles id="timeLimitedRButton"></paper-radio-button></div>
                <div style="color:#6c0404; background: #fefefe;font-size: 1.2em; text-align: center;font-weight: bold;">
                    <core-tooltip large label="<g:message code="receptorTimeLimitedAdviceMsg"/>" position="bottom">
                        <g:message code="timeLimitedAdviceMsg"/>
                    </core-tooltip>
                </div>
            </div>

            <div style="max-width: 600px; margin:15px auto; border-bottom: 1px solid #6c0404;border-top: 1px solid #6c0404;">
                <div layout vertical center center-justified>
                <div horizontal layout center center-justified style="">
                    <div style="width: 250px;">
                        <paper-button raised on-click="{{showTagDialog}}" style="font-size: 0.7em;margin:10px 10px 10px 10px;
                            display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                            <i class="fa fa-tag"></i> <g:message code="addTagLbl"/>
                        </paper-button>
                    </div>
                    <div><g:message code="transactionvsWithTagAdvertMsg"/></div>
                </div>
                <template if="{{selectedTags.length > 0}}">
                    <div layout horizontal center center-justified style="font-weight:bold;font-size: 0.8em; color: #888;
                    margin:0 0 10px 0;">
                        <div style="margin: 0 10px 0 0; vertical-align: bottom;">
                            <g:message code="cooinRequestSelectedTagsLbl"/>
                        </div>
                        <template repeat="{{tag in selectedTags}}">
                            <a class="btn btn-default" data-tagId='{{tag.id}}' on-click="{{removeTag}}"
                               style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                <i class="fa fa-minus" style="color: #6c0404;"></i> {{tag.name}}</a>
                        </template>
                    </div>
                </template>

                </div>
            </div>

            <div style="margin:0 auto; width: 600px;">
                <div horizontal layout center-justified style="font-size: 1.3em;color: #6c0404;">
                    <g:message code="selectAmountLbl"/>
                </div>
                <div layout horizontal center center-justified style="max-width: 600px; margin:0 auto;">
                    <div layout horizontal center center-justified>
                        <paper-input-decorator label="<g:message code="amountLbl"/>" floatingLabel
                                               error="<g:message code="onlyNumbersErrorMsg"/>"
                                               isInvalid="{{!$.inputAmount.validity.valid}}">
                            <input id="inputAmount" value="{{amountValue}}" is="core-input" pattern="\d*">
                        </paper-input-decorator>

                        <vs-currency-selector id="currencySelector" style="margin:0px 0 0 15px; font-size: 1.5em; width: 100px;"></vs-currency-selector>
                    </div>
                </div>
            </div>

            <div horizontal layout style="margin: 20px auto;width: 600px;">
                <div>
                    <div style="color: #ff0000; font-size: 2.5em;"></div>

                    <div class="linkVS" on-click="{{showWallet}}" style=" font-size: 1.2em;">
                        <i class="fa fa-money"></i> <g:message code="cooinWalletLbl"/>
                    </div>
                </div>

                <div flex></div>
                <paper-fab mini icon="done" class="green" on-click="{{submit}}"></paper-fab>
            </div>
        </div>
        <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                 serviceURL="<g:createLink controller="tagVS" action="index"/>"></tagvs-select-dialog>
        <cooin-request-result-dialog id="resultDialog"></cooin-request-result-dialog>
    </template>
    <script>
        Polymer('cooin-request-form', {
            selectedTags: [],
            messageToUser:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                this.selectedTags = []
                this.$.tagDialog.addEventListener('tag-selected', function (e) {
                    console.log("tag-selected: " + JSON.stringify(e.detail))
                    this.selectedTags = e.detail
                }.bind(this))
            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            showWallet: function() {
                VotingSystemClient.setJSONMessageToSignatureClient(new WebAppMessage(Operation.WALLET_OPEN));
            },
            removeTag: function(e) {
                var tagToDelete = e.target.templateInstance.model.tag
                for(tagIdx in this.selectedTags) {
                    if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                        this.selectedTags.splice(tagIdx, 1)
                    }
                }
            },
            submit:function() {
                console.log("submit - amountValue: " + this.amountValue + " " + this.$.currencySelector.getSelected())
                this.messageToUser = null
                if(this.amountValue == 0) {
                    this.messageToUser = "<g:message code='amountTooLowMsg'/>"
                    return
                }
                if(!this.$.inputAmount.validity.valid) {
                    showMessageVS('<g:message code="onlyNumbersErrorMsg"/>', '<g:message code="dataFormERRORLbl"/>')
                    return false
                }
                var tagList = []
                if(this.selectedTags.length > 0) {
                    for(tagIdx in this.selectedTags) {
                        //tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                        tagList.push(this.selectedTags[tagIdx].name);
                    }
                } else tagList.push('WILDTAG'); //No tags, receptor can expend money with any tag

                var webAppMessage = new WebAppMessage(Operation.COOIN_REQUEST)
                webAppMessage.serviceURL = "${createLink( controller:'cooin', action:"request", absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code='cooinRequestLbl'/>"
                webAppMessage.signedContent = {operation:Operation.COOIN_REQUEST, totalAmount:new Number(this.amountValue),
                    isTimeLimited:this.$.timeLimitedRButton.checked, currencyCode:this.$.currencySelector.getSelected(),
                    tag:tagList[0]}
                webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='cooinRequestOKCaption'/>"
                        this.$.resultDialog.showMessage(caption, appMessageJSON.message)
                    } else {
                        caption = '<g:message code="cooinRequestERRORCaption"/>'
                        showMessageVS(appMessageJSON.message, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>