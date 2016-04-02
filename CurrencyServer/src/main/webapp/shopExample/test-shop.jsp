<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="test-shop">
    <template>
        <div class="vertical layout center center-justified">
            <div class="horizontal layout">
                <b>${msg.subjectLbl}:</b><span>{{transactionRequest.subject}}</span>
            </div>
            <h3>
                ${msg.amountLbl}:
                <span>{{transactionRequest.amount}}</span> <span>{{transactionRequest.currencyCode}}</span> -
                <span>{{transactionRequest.tags}}</span>
            </h3>
            <div class="horizontal layout">
                <b>${msg.receptorLbl}:</b><span style="margin: 0 20px 0 0;">{{transactionRequest.toUserName}}</span>
                <b>IBAN:</b><span>{{transactionRequest.toUserIBAN}}</span>
            </div>
            <div id="qrCodeImgDiv" class="vertical layout center center-justified">
                <div style="margin: 25px 0 0 0;font-size: 1.2em;"><b>${msg.readQRMsg}:</b></div>
                <img src="{{qrCodeServiceUrl}}" alt="read it with your mobile"/>
            </div>
            <div hidden="{{!messageDto}}" class="horizontal layout" style="border: 1px solid #ba0011; padding: 5px 10px 5px 10px; margin: 20px 0 0 0;">
                <h3 style="min-width: 150px;">statusCode: <span style="color: #888;">{{messageDto.statusCode}}</span></h3>
                <h3> - message: <span style="color: #888;">{{messageDto.message}}</span></h3>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'test-shop',
            properties: {
                paymentInfoServiceUrl:{type:String, observer:'paymentInfoServiceUrlChanged'},
                qrCodeServiceUrl:{type:String},
                transactionServiceURL:{type:String, observer:'getHTTP'},
                sessionId:{type:String, observer:'sessionIdChanged'},
                transactionRequest:{type:Object, observer:'messageDtoChanged'},
                messageDto:{type:Object, value:null, observer:'messageDtoChanged'}
            },
            paymentInfoServiceUrlChanged: function() {
                this.qrCodeServiceUrl = "${contextURL}/qr?cht=qr&chs=250x250&chl=" + this.paymentInfoServiceUrl
                console.log(this.tagName + " - qrCodeServiceUrl: " + this.qrCodeServiceUrl)
            },
            messageDtoChanged: function() {
                console.log(this.tagName + " - messageDtoChanged - messageDto: " + this.messageDto)
                if(this.messageDto != null) this.$.qrCodeImgDiv.style.display = 'none'
            },
            sessionIdChanged: function() {
                this.transactionServiceURL = vs.contextURL + "/rest/shop/listenTransactionChanges/" + this.sessionId
            },
            ready: function() {
                console.log(this.tagName + " - ready - sessionID: " + this.sessionId)
                console.log(this.tagName + " - ready - paymentInfoServiceURL: " + this.paymentInfoServiceUrl)
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.messageDto = toJSON(responseText)
                }.bind(this))
            }
        });
    </script>
</dom-module>