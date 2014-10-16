<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="bankVS-list">
    <template>
        <style>
            .nifColumn {width:50px; margin: 10px auto;}
            .IBANColumn {width: 220px; margin: 10px auto;}
            .nameColumn {width: 170px; margin: 10px auto;}
            .stateColumn {width: 40px; margin: 10px auto;}
            .row {margin: 0px  auto;text-align: center; cursor: pointer;}
            .descriptionColumn {width: 350px; text-overflow: ellipsis;margin: 10px auto;}
        </style>
        <template repeat="{{bankVS in bankVSMap.bankVSList}}">
            <div horizontal layout class="row" on-click="{{bankVSSelected}}')">
                <div class="nifColumn">{{bankVS.nif}}</div>
                <div class="IBANColumn">{{bankVS.IBAN}}</div>
                <div class="nameColumn">{{bankVS.name}}</div>
                <div class="stateColumn">{{bankVS.state}}</div>
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
