<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="bankVS-list">
    <template>
        <style>
            .bankvs {border: 1px solid #6c0404; margin: 10px; padding:0 10px 10px 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); text-align: center;
                cursor: pointer; text-overflow: ellipsis; max-width: 300px;-moz-border-radius: 3px; border-radius: 4px;
            }
        </style>
        <div class="layout flex horizontal wrap around-justified">
            <template is="dom-repeat" items="{{bankvsListDto}}" as="bankVS">
                <div class="bankvs" on-click="showDetails">
                    <div class="nameColumn" style="font-size: 1.2em;">{{bankVS.name}}</div>
                    <div class="descriptionColumn">{{bankVS.description}}</div>
                </div>
            </template>
        </div>
    </template>
    <script>
        Polymer({
            is:'bankVS-list',
            properties: {
                bankvsListDto: {type:Object, value: {}},
                url: {type:String, value: vs.contextURL + "/rest/userVS/bankVSList", observer:'getHTTP'}
            },
            ready: function() { console.log(this.tagName + " - ready")},
            showDetails: function(e) {
                page.show(vs.contextURL + "/rest/userVS/id/" + e.model.bankVS.id)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.bankvsListDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>
