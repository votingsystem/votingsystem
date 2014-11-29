<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="bankVS-list">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .bankvs {border: 1px solid #6c0404; margin: 10px; padding:0 10px 10px 10px;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24); text-align: center;
                cursor: pointer; text-overflow: ellipsis; max-width: 300px;
            }
        </style>
        <template repeat="{{bankVS in bankVSMap.bankVSList}}">
            <div class="bankvs" on-click="{{bankVSSelected}}')">
                <div class="nameColumn linkVS" style="font-size: 1.2em;">{{bankVS.name}}</div>
                <div class="descriptionColumn">{{bankVS.description}}</div>
            </div>
        </template>
    </template>
    <script>
        Polymer('bankVS-list', {
            ready: function() { console.log(this.tagName + " - ready")},
            publish: { bankVSMap: {value: {}} },
            bankVSSelected: function(e) {
                loadURL_VS("${createLink( controller:'userVS', action:" ", absolute:true)}/" + e.target.templateInstance.model.bankVS.id)
            }
        });
    </script>
</polymer-element>
