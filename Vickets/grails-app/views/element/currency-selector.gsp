<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dropdown-menu', file: 'paper-dropdown-menu.html')}">

<polymer-element name="currency-selector" on-core-select="{{selectAction}}">
    <template>
        <style>
            body /deep/ paper-dropdown-menu.narrow { max-width: 200px; width: 300px; }
        </style>
        <paper-dropdown-menu id="dropDownMenu" valueattr="id" halign="right" selected="EUR">
            <template repeat="{{currencyCodes}}">
                <paper-item id="{{code}}" label="{{name}}"></paper-item>
            </template>
        </paper-dropdown-menu>
    </template>
    <script>
        Polymer('currency-selector', {
            currencyCodes: [
                {name: 'Euro', code: 'EUR'},
                {name: 'Dollar', code: 'USD'},
                {name: 'Yuan', code: 'CNY'},
                {name: 'Yen', code: 'JPY'}],
            ready: function() { console.log(this.tagName + " - ready")},
            getSelected:function() {
                return this.$.dropDownMenu.selected
            },
            selectAction: function(e, details) {
                if(details.isSelected) {
                    this.fire("selected", details.item.id)
                    this.fire('core-signal', {name: "currency-selector-selected", data: details.item.id});
                }
            }
        });
    </script>
</polymer-element>
