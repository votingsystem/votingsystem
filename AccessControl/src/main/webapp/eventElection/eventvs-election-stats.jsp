<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="eventvs-election-stats">
    <style>
        .mainDiv {
            border: 1px solid #888;
            border-bottom: 0px;
        }
    </style>
    <template>
        <div class="mainDiv">
            <div id="header" style="font-size: 1.4em; font-weight: bold; background-color:[[headerColor]]; color: #f9f9f9;
                text-align: center; border-bottom: 1px solid [[headerColor]];"></div>
            <template is="dom-repeat" items="{{fieldsEventVS}}">
                <div class="" style="font-size: 2em; border-bottom: 1px solid #888;">
                    <div style="color: #888;padding:0 0 0 10px;">
                        {{item.content}}
                    </div>
                    <div class="numVotesClass" style="padding:0 0 0 100px; background-color: #efefef; font-style: italic;">{{item.numVotes}} ${msg.votesLbl}</div>
                </div>
            </template>
        </div>
    </template>
<script>
    Polymer({
        is:'eventvs-election-stats',
        properties: {
            statsDto:{type:Object, observer:'statsDtoChanged'},
            eventvs:{type:Object, observer:'eventvsChanged'}
        },
        ready: function() {
            console.log(this.tagName + " - ready: ", this.statsDto)
        },
        eventvsChanged: function() {
            console.log("eventvsChanged: ", this.eventvs)
            var dateBegin =  new Date(this.eventvs.dateBegin)
            if(this.eventvs.state === 'PENDING') {
                this.headerColor = "#fba131"
                this.$.header.textContent = new Date(this.eventvs.dateBegin).getElapsedTime() + " ${msg.toOpenLbl}"
                this.eventvs.fieldsEventVS.forEach(function(element, index, array) {
                    element.numVotes = 0
                })
                this.fieldsEventVS = this.eventvs.fieldsEventVS
            } else {
                this.getHTTP("${contextURL}/rest/eventElection/id/" + this.eventvs.id + "/stats")
            }
        },
        statsDtoChanged: function() {
            if(this.statsDto.eventState === 'ACTIVE') {
                this.headerColor = "#388746"
                this.$.header.textContent = new Date(this.statsDto.dateFinish).getElapsedTime() + " ${msg.toCloseLbl}"
            } else if(this.statsDto.eventState === 'PENDING') {
                this.headerColor = "#fba131"
                this.$.header.textContent = new Date(this.statsDto.dateBegin).getElapsedTime() + " ${msg.toOpenLbl}"
            } else {
                this.headerColor = "#ba0011"
                this.$.header.textContent = "${msg.finalResultLbl}"
            }
            this.fieldsEventVS = this.statsDto.fieldsEventVS
        },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            new XMLHttpRequest().header("Content-Type", "application/json").get(targetURL, function(responseText){
                this.statsDto = toJSON(responseText)
            }.bind(this));
        }
    });
</script>
</dom-module>
