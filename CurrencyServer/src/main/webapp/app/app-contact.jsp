<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="app-contact">
    <template>
        <div class="pageContentDiv" style="font-size: 1.2em; margin: 30px auto; text-align: center;">
            <a  href="#" onclick="javascript:return false" on-click="mailTo"
                style="font-weight: bold;">${msg.emailLbl} <i class="fa fa-envelope-o"></i>
            </a>
        </div>
    </template>
    <script>
        Polymer({
            is:'app-contact',
            properties: { },
            ready: function() {
                //hack to open mail client from javafx app
                if(getURLParam("openMailClient")) {
                    document.location.href = "mailto:${config.emailAdmin}";
                }
            },
            mailTo:function () {
                if( window['isClientToolConnected']) {
                    var operationVS = new OperationVS(Operation.MAIL_TO)
                    operationVS.message = "${config.emailAdmin}"
                    VotingSystemClient.setMessage(operationVS);
                } else document.location.href = "mailto:${config.emailAdmin}";
            }
        });
    </script>
</dom-module>