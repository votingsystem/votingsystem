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
        <div class="card" style="width:400px; padding:20px">
            <div style="margin:10px 10px 20px 10px; font-size: 1.3em; font-weight: bold; color:#6c0404;display:{{caption? 'block':'none'}}">{{caption}}</div>
            <div class="center" style="font-size: 1.3em; color:#888; font-weight: bold;">{{message}}</div>
            <div layout horizontal style="margin:10px 20px 0px 0px;">
                <div flex></div>
                <paper-button raisedButton class="button" label="<g:message code="acceptLbl"/>"
                              on-click="{{accept}}" style=""></paper-button>
            </div>
        </div>
    </template>
    <script>
        Polymer('votingsystem-message-dialog', {
            ready: function() {
                this.style.display = 'none'
            },

            setMessage: function(message, caption) {
                this.message = message
                this.caption = caption
                this.style.display = 'block'
            },

            accept: function(message, caption) {
                this.message = null
                this.caption = null
                this.style.display = 'none'
                this.fire('message-accepted', '')
            }
        });
    </script>
</polymer-element>
