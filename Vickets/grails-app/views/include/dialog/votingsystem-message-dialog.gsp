<polymer-element name="votingsystem-message-dialog">
    <template>
        <style>
        .card {
            position: relative;
            display: inline-block;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 12px 15px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid #ccc;
        }
        paper-button.button {
            background-color: #f9f9f9;
            color: #6c0404;
            border: 1px solid #ccc;
            margin:10px;
            vertical-align: middle;
            line-height: 24px;
            height: 35px;
        }
        </style>
        <div class="card" style="width:400px;z-index: 3000; display: {{isVisible?'block':'none'}}">
            <div layout horizontal center center-justified style="background: #6c0404;">
                <div flex style="font-size: 1.3em; margin:0px 0px 0px 30px;font-weight: bold; color:#f9f9f9;display:{{caption? 'block':'none'}}">
                    {{caption}}
                </div>
                <div>
                    <core-icon-button on-click="{{accept}}" icon="close" style="fill:#f9f9f9;"></core-icon-button>
                </div>
            </div>
            <div style="font-size: 1.3em; color:#6c0404; font-weight: bold; text-align: center; padding:30px 20px 30px 20px;">{{message}}</div>
        </div>
    </template>
    <script>
        Polymer('votingsystem-message-dialog', {
            isVisible:false,

            ready: function() { },

            setMessage: function(message, caption, callerId) {
                this.message = message
                this.caption = caption
                this.callerId = callerId
                this.isVisible = true
            },

            accept: function(message, caption) {
                this.message = null
                this.caption = null
                this.isVisible = false
                this.fire('message-accepted', this.callerId)
            }
        });
    </script>
</polymer-element>
