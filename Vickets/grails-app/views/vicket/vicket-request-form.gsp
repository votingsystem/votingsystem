<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-slider', file: 'paper-slider.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-tooltip', file: 'core-tooltip.html')}">
<link rel="import" href="${resource(dir: '/bower_components/polymer-localstorage', file: 'polymer-localstorage.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-radio-button', file: 'paper-radio-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-currency-selector', file: 'vs-currency-selector.html')}">

<link rel="import" href="<g:createLink  controller="element" params="[element: '/tagVS/tagvs-select-dialog']"/>">

<polymer-element name="vicket-request-form">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
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

                        <vs-currency-selector id="currencySelector" style="margin:0px 0 0 15px; font-size: 1.5em; width: 100px;"></vs-currency-selector>
                    </div>
                </div>
            </div>

            <div horizontal layout style="margin: 20px auto;width: 600px;">
                <div>
                    <div style="color: #ff0000; font-size: 2.5em;"></div>

                    <div class="linkVS" on-click="{{showWallet}}" style=" font-size: 1.2em;">
                        <i class="fa fa-money"></i> <g:message code="vicketWalletLbl"/>
                    </div>
                </div>

                <div flex></div>
                <paper-fab icon="done" class="green" on-click="{{submit}}"></paper-fab>
            </div>
        </div>
        <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                             serviceURL="<g:createLink controller="tagVS" action="index" />"></tagvs-select-dialog>

        <polymer-localstorage id="localstorage" name="vicket-request-localstorage" value="{{vicketsWallet}}"></polymer-localstorage>
    </template>
    <script>
        Polymer('vicket-request-form', {
            selectedTags: [],
            messageToUser:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.tagDialog.addEventListener('tag-selected', function (e) {
                    console.log("tag-selected: " + JSON.stringify(e.detail))
                    this.selectedTags = e.detail
                }.bind(this))
            },
            valueChanged:function() {
                this.amountValue = this.value * 10;
            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            showWallet: function() {
                loadURL_VS("${createLink( controller:'vicket', action:"wallet")}")
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
                    isTimeLimited:this.$.timeLimitedRButton.checked, currencyCode:this.$.currencySelector.getSelected(),
                    tag:tagList[0]}
                webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='vicketRequestOKCaption'/>"
                    } else caption = '<g:message code="vicketRequestERRORCaption"/>'
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>