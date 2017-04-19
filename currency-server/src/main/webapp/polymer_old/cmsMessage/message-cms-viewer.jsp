<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-cms-viewer">
    <template>
        <div id="messageViewer" class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
        </div>
    </template>
    <script>
        Polymer({
            is:'message-currency-viewer',
            properties: {
                cmsMessageDto:{type:Object, value:{}, observer:'cmsMessageChanged'},
                MESSAGE_CMS:{type:Object, value:null},
                FROM_BANK:{type:Object, value:null},
                CURRENCY_REQUEST:{type:Object, value:null},
                CURRENCY_CHANGE:{type:Object, value:null},
                viewer: {type:String, value: ""},
                url:{type:String, observer:'getHTTP'},
                isClientToolConnected: {type:Boolean, value: false}
            },
            cmsMessageChanged: function() {
                switch (this.cmsMessageDto.viewer) {
                    case "message-currency":
                        break;
                    case "message-currency-transaction-from-bank":
                        if(!this.FROM_BANK) {
                            Polymer.Base.importHref(vs.contextURL + '/cmsMessagePEM/message-currency-transaction-from-bank.vsp', function(e) {
                                console.log(this.tagName + " - message-currency-transaction-from-bank: " + this.FROM_BANK)
                                this.FROM_BANK = document.createElement('message-currency-transaction-from-bank');
                                this.loadMainContent(this.FROM_BANK)
                            }.bind(this));
                        } else this.loadMainContent(this.FROM_BANK)
                        break;
                    case "message-currency-transaction-currency-request":
                        break;
                    case "message-currency-transaction":
                        break;
                    case "message-currency-transaction-currency-change":
                        break;
                }
            },
            loadMainContent: function(element) {
                element.cmsMessageDto = this.cmsMessageDto
                vs.loadMainContent(element)
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.cmsMessageDto = toJSON(responseText)
                }.bind(this))
            }
        });
    </script>
</dom-module>