<%@ page contentType="text/html; charset=UTF-8" %>

<link href="representation-state.vsp" rel="import"/>

<dom-module name="representative-list">
    <template>
        <style>
        .representativeDiv:hover {
            border: 1px solid orange;
        }
        .representativeDiv {
            width:300px;
            background-color: #fefefe;
            color: #888;
            border: 1px solid #888;
            box-shadow: 0 3px 3px 0 rgba(0, 0, 0, 0.24);
            margin: 10px 15px 10px 0px;
            -moz-border-radius: 5px;
            border-radius: 3px;
            cursor: pointer;
            height:90px;
            text-overflow: ellipsis;
        }
        </style>
        <div style="padding:10px;">
            <div hidden="{{!representationInfo}}" class="linkVS" on-click="showRepresentativeListDto"
                 style="margin: 10px 0 10px 0; text-align: center;font-weight: bold;font-size: 1.2em;" >${msg.userRepresentativeLbl}</div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{representativeListDto.resultList}}">
                    <div on-tap="showRepresentativeDetails" class='representativeDiv horizontal layout center center center-justified'>
                        <div>
                            <img  style=' max-width: 90px; margin: 0 5px 0 0;' src='{{item.imageURL}}'/>
                        </div>
                        <div class="flex">
                            <p style="text-overflow: ellipsis; font-weight: bold;"><span>{{item.firstName}} {{item.lastName}}</span></p>
                            <div style='margin: 0px 10px 3px 0px;text-align: right;font-size: 0.9em;'>
                                <span>{{item.numRepresentations}}</span> ${msg.numDelegationsPartMsg}
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </div>
        <representation-state id="representationState"></representation-state>
    </template>
    <script>
        Polymer({
            is:'representative-list',
            properties: {
                representativeListDto:{type:Object},
                representationInfo:{type:Boolean, value:false},
                url:{type:String, value: vs.contextURL + "/rest/representative", observer:'getHTTP'}
            },
            ready : function(e) {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('updated-state',
                        function(e) { this.representationInfo = true }.bind(this))
            },
            showRepresentativeListDto : function() {
                this.$.representationState.show()
            },
            showRepresentativeDetails :  function(e) {
                console.log(this.tagName + " - showRepresentativeDetails")
                vs.representative = e.model.item;
                page("/rest/representative/id/" + vs.representative.id)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                new XMLHttpRequest().header("Content-Type", "application/json").get(targetURL, function(responseText){
                    this.representativeListDto = toJSON(responseText)
                }.bind(this));
            }
        });
    </script>
</dom-module>
