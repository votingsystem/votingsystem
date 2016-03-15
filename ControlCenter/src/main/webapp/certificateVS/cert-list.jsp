<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="vs-cert.vsp" rel="import"/>

<dom-module name="cert-list">
    <template>
        <style>
            .certDiv {
                width:500px;
                padding: 10px;
                border:1px solid #ccc;
                background-color: #f9f9f9;
                margin: 10px 5px 5px 10px;
                -moz-border-radius: 5px; border-radius: 5px;
                cursor: pointer;
                overflow:hidden;
                position: relative;
                display: inline-block;
                vertical-align: top;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            }
        </style>
        <div>
            <div class="layout horizontal center center-justified">
                <select id="certTypeSelect" class="form-control" style="margin:0px auto 0px auto;color:black;
                                max-width: 400px;" on-change="certTypeSelect">
                    <option value="&type=USER&state=OK"> - ${msg.certUserStateOKLbl} - </option>
                    <option value="&type=CERTIFICATE_AUTHORITY&state=OK"> - ${msg.certAuthorityStateOKLbl} - </option>
                    <option value="&type=USER&state=CANCELED"> - ${msg.certUserStateCancelledLbl} - </option>
                    <option value="&type=CERTIFICATE_AUTHORITY&state=CANCELED"> - ${msg.certAuthorityStateCancelledLbl} - </option>
                </select>
            </div>

            <div class="flex" horizontal wrap around-justified layout>
                <template is="dom-repeat" items="{{certListDto.resultList}}">
                    <div class="certDiv" on-tap="showCert">
                        <div>
                            <div class="horizontal layout">
                                <div class='groupSubjectDiv'><span style="font-weight: bold;">
                                            ${msg.serialNumberLbl}: </span>{{item.serialNumber}}</div>
                                <div id="certStateDiv" style="margin:0px 0px 0px 20px; font-size: 1.2em;
                                        font-weight: bold; float: right;">{{getState(item.state)}}</div>
                            </div>
                        </div>
                        <div class='groupSubjectDiv'><span style="font-weight: bold;">${msg.subjectLbl}:</span>
                            <span>{{item.subjectDN}}</span></div>
                        <div><span style="font-weight: bold;">${msg.issuerLbl}:</span> <span>{{item.issuerDN}}</span></div>
                        <div><span style="font-weight: bold;">${msg.signatureAlgotithmLbl}: </span>{{item.sigAlgName}}</div>
                        <div class="horizontal layout">
                            <div>
                                <span style="font-weight: bold;">${msg.noBeforeLbl}:</span> <span>{{getDate(item.notBefore)}}</span></div>
                            <div style="margin:0px 0px 0px 20px;">
                                <span style="font-weight: bold;">${msg.noAfterLbl}:</span> <span>{{getDate(item.notAfter)}}</span></div>
                        </div>
                        <div>
                            <div hidden="{{!isRoot(item)}}" class="text-center" style="font-weight: bold;
                                    margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;">${msg.rootCertLbl}</div>
                        </div>
                    </div>
                </template>
            </div>
            <vs-pager on-pager-change="pagerChange" max="{{certListDto.max}}"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{certListDto.offset}}" total="{{certListDto.totalCount}}"></vs-pager>
        </div>
        <div id="certDetailsDialog" class="modalDialog">
            <div style="max-width:450px;">
                <div style="position: absolute; top: 0px; right: 0px;">
                    <i class="fa fa-times closeIcon" on-click="closeDialog"></i>
                </div>
                <vs-cert id="certDetails" cert="{{cert}}"></vs-cert>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'cert-list',
            properties: {
                url:{type:String, observer:'getHTTP'},
                certListDto:{type:Object,observer:'certListDtoChanged'},
            },
            ready: function() {
                console.log(this.tagName + " - ready - ")
                this.url = vs.contextURL + "/rest/certificateVS/certs"
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            isRoot : function(item) {
                return item.root
            },
            showCert :  function(e) {
                console.log(this.tagName + " - showCertDetails")
                this.$.certDetails.certvs = e.model.item;
                this.$.certDetailsDialog.style.opacity = 1
                this.$.certDetailsDialog.style['pointer-events'] = 'auto'
            },
            closeDialog:function() {
                this.$.certDetailsDialog.style.opacity = 0
                this.$.certDetailsDialog.style['pointer-events'] = 'none'
            },
            certListDtoChanged:function() {
                if(this.certListDto == null) return
                var certType = getURLParam('type')
                var certState = getURLParam('state')
                console.log(this.tagName + " - certListDtoChanged - certType: " + certType + " - certState: " + certState)
            },
            getState:function(state){
                var stateLbl
                if("OK" == state) stateLbl = "${msg.certOKLbl}"
                else if("CANCELED" ==  state) stateLbl = "${msg.certCancelledLbl}"
                else stateLbl = state
            },
            pagerChange:function(e) {
                var certTypeSelectValue = this.$.certTypeSelect.value
                targetURL = vs.contextURL + "/rest/certificateVS/certs?menu=" + menuType + certTypeSelectValue +
                        "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.url = targetURL
            },
            certTypeSelect: function () {
                var optionSelected = this.$.certTypeSelect.value
                if("" != optionSelected) {
                    targetURL = vs.contextURL + "/rest/certificateVS/certs?menu=" + menuType + optionSelected
                    history.pushState(null, null, targetURL);
                    this.url = targetURL
                }
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.certListDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>