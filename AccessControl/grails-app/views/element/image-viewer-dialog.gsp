<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-icon-button" file="core-icon-button.html"/>
<vs:webresource dir="vs-html-echo" file="vs-html-echo.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>


<polymer-element name="image-viewer-dialog" attributes="url description">
    <template>
        <paper-dialog id="xDialog" layered backdrop on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style no-shim>
            .dialog {
                box-sizing: border-box;
                -moz-box-sizing: border-box;
                font-family: Arial, Helvetica, sans-serif;
                font-size: 13px;
                -webkit-user-select: none;
                -moz-user-select: none;
                overflow: auto;
                background: white;
                padding:10px 30px 30px 30px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
                width: 500px;
            }
            </style>
            <div layout horizontal center center-justified>
                <div flex style="font-size: 1.5em; margin:5px 30px 10px 10px;font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;display:{{description? 'block':'none'}}">{{description}}</div>
                </div>
                <div style="position: absolute; top: 0px; right: 0px;">
                    <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                </div>
            </div>
            <div layout horizontal center center-justified>
                <img src="{{url}}" style="width:100%; height: 100%;"></img>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('image-viewer-dialog', {
            url:null,
            description:null,
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show:function() {
                this.$.xDialog.opened = true
            },
            show:function() {
                this.$.xDialog.opened = true
            },
            onCoreOverlayOpen:function(e) { },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</polymer-element>
