<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-currency-selector/vs-currency-selector.html" rel="import"/>
<link href="../tagVS/tagvs-select-dialog.vsp" rel="import"/>
<link href="../currency/currency-request-result-dialog.vsp" rel="import"/>

<dom-module name="currency-request-form">
    <template>
        <div>
            <div style="margin:4px auto 10px auto; width: 600px;">
                <div class="horizontal layout center-justified" style="font-size: 1.3em;color: #6c0404;">
                    ${msg.selectAmountLbl}
                </div>
                <div class="layout horizontal center center-justified" style="max-width: 600px; margin:5px auto;">
                    <div class="layout horizontal center center-justified">
                        <input type="text" id="inputAmount" class="form-control"
                               style="width:150px;margin:0 10px 0 0;" pattern="^[0-9]*$" required
                               title="${msg.amountLbl}" placeholder="${msg.amountLbl}"/>
                        <vs-currency-selector id="currencySelector" >
                        </vs-currency-selector>
                    </div>
                </div>
            </div>
            <div class="layout horizontal center center-justified" style="margin: 0 0 20px 0;">
                <div style="margin: 0 10px 0 0;">
                    <input type="checkbox" id="timeLimitedButton" style="height: 1.3em; width: 1.3em;">
                </div>
                <div style="color:#888; font-size: 1.1em;"  title="${msg.receptorTimeLimitedAdviceMsg}">
                ${msg.timeLimitedAdviceMsg}</div>
            </div>
            <div style="max-width: 600px; margin:15px auto; border-bottom: 1px solid #6c0404;border-top: 1px solid #6c0404;">
                <div class="layout vertical center center-justified">
                <div hidden="{{!selectedTagsHidden}}" class="horizontal layout center center-justified">
                    <div style="width: 250px;">
                        <button on-click="showTagDialog" style="font-size: 0.8em;margin:10px 10px 10px 10px;">
                            <i class="fa fa-tag"></i> ${msg.addTagLbl}
                        </button>
                    </div>
                    <div>${msg.transactionvsWithTagAdvertMsg}</div>
                </div>
                <div hidden="{{selectedTagsHidden}}" class="layout horizontal center center-justified"
                     style="font-weight:bold;font-size: 0.9em; color: #888;
                        margin:0 0 10px 0;">
                    <div style="margin: 0 10px 0 0; vertical-align: bottom;">
                        ${msg.currencyRequestSelectedTagsLbl}
                    </div>
                    <template is="dom-repeat" items="{{selectedTags}}" as="tag">
                        <a class="btn btn-default" data-tag-id$='{{tag.id}}' on-click="removeTag"
                           style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                            <i class="fa fa-minus" style="color: #6c0404;"></i> <span>{{tag.name}}</span></a>
                    </template>
                </div>
                </div>
            </div>

            <div class="layout horizontal center center-justified" >
                <div class="layout horizontal" style="margin:10px 20px 0px 0px; width: 600px;">
                    <div class="flex"></div>
                    <div>
                        <button on-click="submit" style="margin: 0 0 5px 5px;font-size: 1.1em;">
                            <i class="fa fa-check" style="color: #388746;"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
            <div>
                <tagvs-select-dialog id="tagDialog" caption="${msg.addTagDialogCaption}"></tagvs-select-dialog>
            </div>
            <currency-request-result-dialog id="resultDialog"></currency-request-result-dialog>
        </div>
    </template>
    <script>
        Polymer({
            is:'currency-request-form',
            properties:{
                selectedTags: {object:Array, value:[], observer:'selectedTagsChanged'},
                selectedTagsHidden: {object:Boolean, value:true}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.selectedTags = []
                this.$.tagDialog.addEventListener('tag-selected', function (e) {
                    console.log("-- tag-selected: " + JSON.stringify(e.detail))
                    this.selectedTags = toJSON(JSON.stringify(e.detail))

                }.bind(this))
                this.$.timeLimitedButton.checked = false
            },
            showTagDialog: function() {
                this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
            },
            showWallet: function() {
                VotingSystemClient.setMessage(new OperationVS(Operation.WALLET_OPEN));
            },
            removeTag: function(e) {
                var tagToDelete = e.model.tag
                for(tagIdx in this.selectedTags) {
                    if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                        this.selectedTags.splice(tagIdx, 1)
                    }
                }
                this.selectedTags = this.selectedTags.slice(0); //hack to notify array changes
            },
            selectedTagsChanged: function(e) {
                console.log(this.tagName + " - selectedTagsChanged - num. tags: " + this.selectedTags.length)
                this.selectedTagsHidden = (this.selectedTags.length === 0)
            },
            submit:function() {
                console.log("submit - amountValue: " + this.$.inputAmount.value + " " +
                        this.$.currencySelector.getSelected())
                if(!this.$.inputAmount.validity.valid || !(this.$.inputAmount.value > 0)) {
                    showMessageVS("${msg.enterValidAmountMsg}", "${msg.errorLbl}")
                    return
                }
                var tagList = []
                if(this.selectedTags.length > 0) {
                    for(tagIdx in this.selectedTags) {
                        //tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                        tagList.push(this.selectedTags[tagIdx].name);
                    }
                } else tagList.push('WILDTAG'); //No tags, receptor can expend money with any tag

                var operationVS = new OperationVS(Operation.CURRENCY_REQUEST)
                operationVS.serviceURL = contextURL + "/rest/currency/processRequest"
                operationVS.signedMessageSubject = "${msg.currencyRequestLbl}"
                var signedContent = {operation:Operation.CURRENCY_REQUEST, amount:this.$.inputAmount.value,
                    timeLimited:this.$.timeLimitedButton.checked, currencyCode:this.$.currencySelector.getSelected(),
                    tags:tagList}
                operationVS.jsonStr = JSON.stringify(signedContent)
                operationVS.setCallback(function(appMessage) { this.showResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            showResponse:function(appMessage) {
                var appMessageJSON = JSON.parse(appMessage)
                var caption
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.currencyRequestOKCaption}"
                    this.$.resultDialog.showMessage(caption, appMessageJSON.message)
                } else {
                    caption = '${msg.currencyRequestERRORCaption}'
                    showMessageVS(appMessageJSON.message, caption)
                }
                this.click() //hack to refresh screen
            }
        });
    </script>
</dom-module>