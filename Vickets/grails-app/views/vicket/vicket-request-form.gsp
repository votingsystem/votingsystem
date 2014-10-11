<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-slider', file: 'paper-slider.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dropdown-menu', file: 'paper-dropdown-menu.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="${resource(dir: '/bower_components/polymer-localstorage', file: 'polymer-localstorage.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-radio-button', file: 'paper-radio-button.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/vicketTagVS/tagvs-select-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/vicket/vicket-tag-group']"/>">


<polymer-element name="vicket-request-form"  on-core-select="{{selectAction}}">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            body /deep/ paper-dropdown-menu.narrow { max-width: 200px; width: 300px; }
            .green-slider paper-slider::shadow #sliderKnobInner,
            .green-slider paper-slider::shadow #sliderKnobInner::before,
            .green-slider paper-slider::shadow #sliderBar::shadow #activeProgress {
                background-color: #0f9d58;
            }
            .messageToUser {
                font-weight: bold;
                margin:10px auto 30px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
                max-width:400px;
            }
            paper-fab.green {
                background: #259b24;
                color: #f0f0f0;            }
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
                    <div style="width: 200px;">
                        <votingsystem-button on-click="{{showTagDialog}}" style="font-size: 0.9em;margin:10px 0px 10px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                            <i class="fa fa-tag" style="margin:0 5px 0 2px;"></i> <g:message code="addTagLbl"/>
                        </votingsystem-button>
                    </div>
                    <div><g:message code="transactionvsWithTagAdvertMsg"/></div>
                </div>
                <template if="{{selectedTags.length > 0}}">
                    <div layout horizontal center center-justified style="font-weight:bold;font-size: 0.8em; color: #888;
                    margin:0 0 10px 0;">
                        <g:message code="vicketRequestSelectedTagsLbl"/>
                        <template repeat="{{tag in selectedTags}}">
                            <a class="btn btn-default" data-tagId='{{tag.id}}' on-click="{{removeTag}}"
                               style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                <i class="fa fa-minus" style="color: #6c0404;"></i> {{tag.name}}</a>
                        </template>
                    </div>
                </template>

                </div>
            </div>

            <div horizontal layout style="margin:0 auto; width: 600px;">
                <div class="green-slider">
                    <div style="font-size: 1.2em; margin:0 0 25px 0; text-align: center;"><g:message code="selectAmountLbl"/></div>
                    <paper-slider id="amount" pin snaps value="{{value}}" max="10" step="1" value="10" style="width: 400px;"></paper-slider>
                </div>
                <div layout horizontal center center-justified style="max-width: 600px; margin:0 auto;">
                    <div layout horizontal center center-justified>
                        <div style="font-size: 2.2em; width: 50px;">{{amountValue}}</div>

                        <paper-dropdown-menu valueattr="label" halign="right" selected="0"
                                             style="margin:0px 0 0 15px; font-size: 1.5em; width: 100px;">
                            <template repeat="{{currencyCodes}}">
                                <paper-item id="{{code}}" label="{{name}}"></paper-item>
                            </template>
                        </paper-dropdown-menu>
                    </div>
                </div>
            </div>

            <div horizontal layout style="margin: 20px auto;width: 600px;">
                <div flex></div>
                <paper-fab icon="done" class="green" on-click="{{submit}}"></paper-fab>
            </div>
        </div>
        <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                             serviceURL="<g:createLink controller="vicketTagVS" action="index" />"></tagvs-select-dialog>

        <polymer-localstorage id="localstorage" name="vicket-request-localstorage" value="{{vicketsWallet}}"></polymer-localstorage>

    </template>
    <script>
        Polymer('vicket-request-form', {
            selectedTags: [],
            currencyCode:null,
            currencyCodes: [
                {name: 'Euro', code: 'EUR'},
                {name: 'Dollar', code: 'USD'},
                {name: 'Yuan', code: 'CNY'},
                {name: 'Yen', code: 'JPY'}],
            messageToUser:null,

            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.tagDialog.addEventListener('tag-selected', function (e) {
                    console.log("tag-selected: " + JSON.stringify(e.detail))
                    this.selectedTags = e.detail
                }.bind(this))
            },
            currencyCodeSelect:function() {
                this.fire("selected", this.$.currencyCodeSelect.value)
                this.fire('core-signal', {name: "amount-selected", data: this.$.currencyCodeSelect.value});
            },
            valueChanged:function() {
                this.amountValue = this.value * 10;
            },
            selectAction: function(e, details) {
                if(details.isSelected) {
                    this.currencyCode = details.item.id
                }

            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            removeTag: function(e) {
                var tagToDelete = e.target.templateInstance.model.tag
                for(tagIdx in this.selectedTags) {
                    if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                        this.selectedTags.splice(tagIdx, 1)
                    }
                }
            },
            updateLocalStorage:function(appMessageJSON) {
                if(this.vicketsWallet) {
                    console.log("updateLocalStorage")
                    var vicketsWalletJSON
                    try {vicketsWalletJSON = JSON.parse(this.vicketsWallet)} catch(ex) {console.log(ex)}
                    if(Array.isArray(vicketsWalletJSON)) vicketsWalletJSON = vicketsWalletJSON.concat(appMessageJSON.vicketList)
                    else vicketsWalletJSON = appMessageJSON.vicketList
                    this.vicketsWallet = JSON.stringify(vicketsWalletJSON)
                } else this.vicketsWallet = JSON.stringify(appMessageJSON.vicketList)
            },
            submit:function() {
                console.log("submit")
                this.messageToUser = null
                if(this.amountValue == 0) {
                    this.messageToUser = "<g:message code='amountTooLowMsg'/>"
                    return
                }

                var tagList = []
                if(this.selectedTags.length > 0) {
                    for(tagIdx in this.selectedTags) {
                        //tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                        tagList.push(this.selectedTags[tagIdx].name);
                    }
                } else tagList.push('WILDTAG'); //No tags, receptor can expend money with any tag

                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.VICKET_REQUEST)
                webAppMessage.serviceURL = "${createLink( controller:'vicket', action:"request", absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code='vicketRequestLbl'/>"
                webAppMessage.signedContent = {operation:Operation.VICKET_REQUEST, totalAmount:this.amountValue,
                    isTimeLimited:this.$.timeLimitedRButton.checked, currencyCode:this.currencyCode, tag:tagList[0]}

                webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='vicketRequestOKCaption'/>"
                        this.updateLocalStorage(appMessageJSON)
                    } else caption = '<g:message code="vicketRequestERRORCaption"/>'
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>