<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="image-viewer-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div >
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 30px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div hidden="{{!description}}" style="text-align: center;">{{description}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div class="layout horizontal center center-justified">
                    <img id="representativeImage" src="{{url}}" style="max-width: 400px;"/>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'image-viewer-dialog',
            properties: {
                description:{type:String},
                url:{type:String, value:null, observer:'urlChanged'}
            },
            urlChanged: function() {
                console.log(this.tagName + " - urlChanged")
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show:function() {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
