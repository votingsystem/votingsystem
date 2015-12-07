<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/image-viewer-dialog.vsp" rel="import"/>
<link href="representative-select-dialog.vsp" rel="import"/>
<link href="representative-request-accreditations-dialog.vsp" rel="import"/>
<link href="representative-request-votinghistory-dialog.vsp" rel="import"/>
<link href="../resources/bower_components/paper-tabs/paper-tabs.html" rel="import"/>

<dom-module name="representative-info">
    <template>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            paper-tabs, paper-toolbar {
                background-color: #ba0011;
                color: #fff;
                box-shadow: 0px 3px 6px rgba(0, 0, 0, 0.2);
            }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px; text-align: center;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
        </style>
        <iron-ajax auto url="{{url}}" last-response="{{representative}}" handle-as="json" content-type="application/json"></iron-ajax>
        <div>
            <div hidden="{{'user' !== menuType}}" class="horizontal layout center-justified" style="font-size: 0.9em;">
                <button on-click="selectRepresentative">
                    <i class="fa fa-hand-o-right"></i> ${msg.saveAsRepresentativeLbl}
                </button>
            </div>
            <div class="text-center" style="margin:20px auto 15px 15px;">
                <div class="layout vertical center center-justified" style="width:100%;">
                    <div data-representative-id$="{{representative.id}}" class="flex representativeNameHeader">
                        <div>{{representativeFullName}}</div>
                    </div>
                    <div class="representativeNumRepHeader">
                        <span>{{representative.numRepresentations}}</span> ${msg.numDelegationsPartMsg}
                    </div>
                </div>
            </div>
            <div style="margin:0px auto 0px auto;">
                <div class="horizontal layout" style="margin: 20px 0 0 0;">
                    <paper-tabs selected="{{selectedTab}}" style="width: 100%; margin: 0 0 10px 0;">
                        <paper-tab>${msg.profileLbl}</paper-tab>
                        <paper-tab>${msg.votingHistoryLbl}</paper-tab>
                    </paper-tabs>
                </div>

                <div hidden="{{votingTabSelected}}" class="tabContent">
                    <div class="layout horizontal">
                        <div hidden="{{!representative.imageURL}}">
                            <img id="representativeImg" on-click="showImage"
                                 style="text-align:center; width: 100px;margin-right: 20px;"/>
                        </div>
                        <div style="margin:auto auto">
                            <vs-html-echo html="{{decodeBase64(representative.description)}}"></vs-html-echo>
                        </div>
                    </div>
                </div>

                <div hidden="{{!votingTabSelected}}" class="tabContent">
                    <div hidden="{{!isAdmin}}">
                        <div class="horizontal layout center center-justified" style="margin: 10px 0 0 0;">
                            <div>
                                <button id="votingHistoryButton" style="margin:0px 20px 0px 0px; width:300px;"
                                         on-click="requestVotingHistory">
                                    ${msg.requestVotingHistoryLbl}
                                </button>
                            </div>
                            <div>
                                <button id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;"
                                        on-click="requestAccreditations">
                                    ${msg.requestRepresentativeAcreditationsLbl}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <image-viewer-dialog id="representativeImage" url="{{representative.imageURL}}" description="{{representativeFullName}}"></image-viewer-dialog>
        <representative-select-dialog id="selectRepresentativeDialog"></representative-select-dialog>
        <representative-request-accreditations-dialog id="accreditationsDialog"></representative-request-accreditations-dialog>
        <representative-request-votinghistory-dialog id="votingHistoryDialog"></representative-request-votinghistory-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-info',
            properties: {
                representative:{type:Object, value:{}, observer:'representativeChanged'},
                selectedTab:{type:Number, value:0, observer:'selectedTabChanged'},
                isAdmin:{computed:'_checkIfAdmin(representative)'}
            },
            requestAccreditations:function(){
                this.$.accreditationsDialog.show(this.representative)
            },
            requestVotingHistory:function() {
                this.$.votingHistoryDialog.show(this.representative)
            },
            _checkIfAdmin:function(representative) {
                return 'admin' === menuType
            },
            setProfileView:function() {
                this.selectedTab = 'profile'
            },
            setVotingHistoryView:function() {
                this.selectedTab = 'votingHistory'
            },
            selectedTabChanged:function() {
                console.log(this.tagName + " selectedTabChanged - selectedTab: " + this.selectedTab)
                if(this.selectedTab === 0) {
                    this.votingTabSelected = false
                } else {
                    this.votingTabSelected = true
                }
            },
            decodeBase64:function(base64EncodedString) {
                if(base64EncodedString == null) return null
                return decodeURIComponent(escape(window.atob(base64EncodedString)))
            },
            selectRepresentative:function() {
                console.log("selectRepresentative")
                this.$.selectRepresentativeDialog.show(this.representative)
            },
            representativeChanged: function() {
                console.log(this.tagName + ".representativeChanged - selectedTab: " + this.selectedTab +
                " - img: " + this.representative.imageURL)
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                if(this.representative.imageURL != null) this.$.representativeImg.src = this.representative.imageURL
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            }
        });
    </script>
</dom-module>