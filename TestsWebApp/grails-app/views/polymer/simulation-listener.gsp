<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="simulation-listener">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
        </style>
        <div layout horizontal center center-justified class="pageContentDiv">
            <template if="{{subpage}}">
                <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                    <i class="fa fa-arrow-left"></i></votingsystem-button>
            </template>
        </div>
    </template>
    <script>
        Polymer('simulation-listener', {
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            back:function() {
                this.fire('core-signal', {name: "simulation-listener-closed", data: null});
            },
            submitClaim:function() {
                console.log("submitClaim")
            }
        });
    </script>
</polymer-element>