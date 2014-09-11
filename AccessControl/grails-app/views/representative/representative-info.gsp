<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/image-viewer-dialog']"/>">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/select-representative-dialog']"/>">
<link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">

<polymer-element name="representative-info" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .tabContent {
                padding: 10px 20px 10px 20px;
                margin:0px auto 0px auto;
                width:auto;
            }
            paper-tabs.transparent-teal {
                background-color: transparent;
                color:#ba0011;
                box-shadow: none;
                cursor: pointer;
            }
            paper-tabs.transparent-teal::shadow #selectionBar {
                background-color: #ba0011;
            }
            paper-tabs.transparent-teal paper-tab::shadow #ink {
                color: #ba0011;
            }
        </style>
        <div class="pageContentDiv">
            <div style="margin: 0 30px 0 30px;">
                <div class="text-center row" style="margin:20px auto 15px 15px;">
                    <div class="representativeNameHeader">
                        <div>{{representativeFullName}}</div>
                    </div>
                    <div  class="representativeNumRepHeader" style="">
                        {{representative.numRepresentations}} <g:message code="numDelegationsPartMsg"/>
                    </div>
                    <template if="{{'user' == menuType}}">
                        <div>
                            <button type="button" class="btn btn-default" on-click="{{selectRepresentative}}"
                                    style="margin:15px 20px 15px 0px;">
                                <g:message code="saveAsRepresentativeLbl"/> <i class="fa fa-hand-o-right"></i>
                            </button>
                        </div>
                    </template>

                </div>
                <div  style="width: 1000px; margin:0px auto 0px auto;">
                    <paper-tabs  style="width: 1000px;margin:0px auto 0px auto;" class="transparent-teal center" valueattr="name"
                                 selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                        <paper-tab name="profile" style="width: 400px"><g:message code="profileLbl"/></paper-tab>
                        <paper-tab name="votingHistory"><g:message code="votingHistoryLbl"/></paper-tab>

                    </paper-tabs>
                    <div class="tabContent" style="display:{{selectedTab == 'profile'?'block':'none'}}">
                        <div style=">
                            <div style="">
                                <img id="representativeImg" on-click="{{showImage}}" src="{{representative.imageURL}}"
                                     style="text-align:center; width: 100px;margin-right: 20px;"></img>
                            </div>
                            <div style="margin:0px auto 15px auto;">
                                <votingsystem-html-echo html="{{representative.description}}"></votingsystem-html-echo>
                            </div>
                        </div>
                    </div>

                    <div class="tabContent" style="display:{{selectedTab == 'votingHistory'?'block':'none'}}">
                        <template if="{{'admin' == menuType}}">
                            <div style="margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
                                <div style="display:table-cell;">
                                    <button id="votingHistoryButton" type="button" class="btn btn-default"
                                            style="margin:0px 20px 0px 0px; width:300px;">
                                        <g:message code="requestVotingHistoryLbl"/>
                                    </button>
                                </div>
                                <div style="display:table-cell;">
                                    <button type="button" id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;"
                                            class="btn btn-default">
                                        <g:message code="requestRepresentativeAcreditationsLbl"/>
                                    </button>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
            </div>
        </div>
        <image-viewer-dialog id="representativeImage" url="{{representative.imageURL}}" description="{{representativeFullName}}"></image-viewer-dialog>
        <select-representative-dialog id="selectRepresentativeDialog"></select-representative-dialog>
    </template>
    <script>
        Polymer('representative-info', {
            menuType:menuType,
            selectedTab:'profile',
            subpage:false,
            publish: {
                representative: {value: {}}
            },
            selectRepresentative:function() {
                console.log("selectRepresentative")
                this.$.selectRepresentativeDialog.show(this.representative)
            },
            representativeChanged: function() {
                console.log(this.tagName + "- subpage:  " + this.subpage)
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            ready: function() {
                console.log(this.tagName + "- subpage:  " + this.subpage)
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            },
            back:function() {
                this.fire('core-signal', {name: "representative-closed", data: null});
            }
        });
    </script>
</polymer-element>