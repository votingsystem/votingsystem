<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/image-viewer-dialog.vsp" rel="import"/>
<link href="representative-select-dialog.vsp" rel="import"/>
<link href="representative-request-accreditations-dialog.vsp" rel="import"/>
<link href="representative-request-votinghistory-dialog.vsp" rel="import"/>
<link href="representative-cancel-dialog.vsp" rel="import"/>

<dom-module name="representative-info">
    <template>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px; text-align: center;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
        </style>
        <div class="pagevs">
            <div class="flex horizontal layout center center-justified" style="font-size: 0.9em;">
                <div on-click="selectRepresentative" class="buttonvs layout horizontal center">
                    <i class="fa fa-hand-o-right"></i> ${msg.saveAsRepresentativeLbl}
                </div>
                <div class="flex"></div>
                <div>
                    <div class="configIcon" on-click="showAdminDialog">
                        <i class="fa fa-cogs"></i>
                    </div>
                </div>
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
            <div style="margin: 20px auto 0 auto;">
                <div class="horizontal layout"
                     style="cursor: pointer;padding:5px 0 0 0;color:#ba0011; border-bottom: 1px solid #ba0011;">
                    <div id="profileDiv" on-click="setProfileView" style="width: 100%;font-weight: bold; font-size: 1.1em;
                            border-bottom: 2px #ba0011 solid;" class="horizontal layout center-justified">
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
                    <div class="vertical layout center" style="margin: 10px auto;">
                        <div class="buttonvs" id="votingHistoryButton" style="margin:10px 0 0 0; width:230px;"
                             on-click="requestVotingHistory">
                            ${msg.requestVotingHistoryLbl}
                        </div>
                        <div class="buttonvs" id="accreditationRequestButton" style="margin:20px 0 0 0; width:230px;"
                             on-click="requestAccreditations">
                            ${msg.requestRepresentativeAcreditationsLbl}
                        </div>
                    </div>
                </div>
            </div>
            <div id="adminDialog" class="modalDialog">
                <div>
                    <div class="layout horizontal center center-justified">
                        <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                            <div style="text-align: center;">
                                ${msg.removeRepresentativeLbl}
                            </div>
                        </div>
                        <div style="position: absolute; top: 0px; right: 0px;">
                            <i class="fa fa-times closeIcon" on-click="closeAdminDialog"></i>
                        </div>
                    </div>
                    <div class="textDialog" style="padding:10px 20px 10px 20px;">
                        <div class="buttonvs" on-click="revokeRepresentative">
                            <i class="fa fa-times"></i> ${msg.removeRepresentativeLbl}
                        </div>
                        <div class="buttonvs"  on-click="editRepresentative" style="margin: 10px 0 0 0;">
                            <i class="fa fa-pencil-square-o"></i> ${msg.editRepresentativeLbl}
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <image-viewer-dialog id="representativeImage" url="{{representative.imageURL}}" description="{{representativeFullName}}"></image-viewer-dialog>
        <representative-cancel-dialog id="representativeCancelDialog"></representative-cancel-dialog>
        <representative-select-dialog id="selectRepresentativeDialog"></representative-select-dialog>
        <representative-request-accreditations-dialog id="accreditationsDialog"></representative-request-accreditations-dialog>
        <representative-request-votinghistory-dialog id="votingHistoryDialog"></representative-request-votinghistory-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-info',
            properties: {
                url:{type:String, observer:'getHTTP'},
                votingTabSelected:{type:Boolean, value:false},
                representative:{type:Object, value:{}, observer:'representativeChanged'},
            },
            revokeRepresentative:function(){
                this.$.representativeCancelDialog.show(this.representative)
                this.closeAdminDialog();
            },
            editRepresentative:function(){
                vs.representative = this.representative
                page("/representative/edit")
                this.closeAdminDialog();
            },
            requestAccreditations:function(){
                this.$.accreditationsDialog.show(this.representative)
            },
            requestVotingHistory:function() {
                this.$.votingHistoryDialog.show(this.representative)
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
                this.$.profileDiv.style['border-bottom'] = "0px #ba0011 solid"
                this.$.votingHistoryDiv.style['border-bottom'] = "0px #ba0011 solid"
                if(this.modeProfile === true) {
                    this.votingTabSelected = false
                    this.$.profileDiv.style['border-bottom'] = "2px #ba0011 solid"
                } else {
                    this.votingTabSelected = true
                    this.$.votingHistoryDiv.style['border-bottom'] = "2px #ba0011 solid"
                }
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
                try {
                    this.$.representativeDescription.innerHTML = window.atob(this.representative.description)
                } catch (e) {console.log(e)}
            },
            ready: function() { },
            closeAdminDialog: function() {
                this.$.adminDialog.style.opacity = 0
                this.$.adminDialog.style['pointer-events'] = 'none'
            },
            showAdminDialog: function() {
                this.$.adminDialog.style.opacity = 1
                this.$.adminDialog.style['pointer-events'] = 'auto'
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.representative = toJSON(responseText)
                }.bind(this));
            }
        });
    </script>
</dom-module>