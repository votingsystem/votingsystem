<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/image-viewer-dialog.vsp" rel="import"/>
<link href="representative-select-dialog.vsp" rel="import"/>
<link href="representative-request-accreditations-dialog.vsp" rel="import"/>
<link href="representative-request-votinghistory-dialog.vsp" rel="import"/>

<dom-module name="representative-info">
    <template>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px; text-align: center;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
        </style>
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
            <div style="margin: 20px 20px 0 20px;">
                <div hidden={{!smallScreen}} class="horizontal layout"
                     style="cursor: pointer;padding:5px 0 0 0;background-color: #ba0011;color:#fefefe;">
                    <div id="profileDiv" on-click="setProfileView" style="width: 100%;font-weight: bold; font-size: 1.1em;border-bottom: 3px orange solid;" class="horizontal layout center-justified">
                        <div>${msg.profileLbl}</div>
                    </div>
                    <div id="votingHistoryDiv" on-click="setVotingHistoryView" style="width: 100%;font-weight: bold; font-size: 1.1em;" class="horizontal layout center-justified">
                        <div>${msg.votingHistoryLbl}</div>
                    </div>
                </div>
                <div hidden="{{votingTabSelected}}" class="tabContent">
                    <div class="layout horizontal">
                        <div hidden="{{!representative.imageURL}}">
                            <img id="representativeImg" on-click="showImage"
                                 style="text-align:center; width: 100px;margin-right: 20px;"/>
                        </div>
                        <div style="margin:auto auto">
                            <div id="representativeDescription"></div>
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
                url:{type:String, observer:'getHTTP'},
                representative:{type:Object, value:{}, observer:'representativeChanged'},
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
                this.modeProfile = true
                this.modeHistoryView = false
                this.selectedTabChanged()
            },
            setVotingHistoryView:function() {
                this.modeProfile = false
                this.modeHistoryView = true
                this.selectedTabChanged()
            },
            selectedTabChanged:function() {
                console.log(this.tagName + " selectedTabChanged")
                this.$.profileDiv.style['border-bottom'] = "0px orange solid"
                this.$.votingHistoryDiv.style['border-bottom'] = "0px orange solid"
                if(this.modeProfile === true) {
                    this.votingTabSelected = false
                    this.$.profileDiv.style['border-bottom'] = "3px orange solid"
                } else {
                    this.votingTabSelected = true
                    this.$.votingHistoryDiv.style['border-bottom'] = "3px orange solid"
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
                d3.select(this).select("#representativeDescription").html(this.decodeBase64(this.representative.description))
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.representative = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>