<!DOCTYPE HTML>
<html>
<head>
    <meta name="layout" content="simplePage" />
    <!--https://github.com/josdejong/jsoneditor-->
    <link rel="stylesheet" type="text/css" href="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.css')}">
    <script type="text/javascript" src="${resource(dir: '/bower_components/jsoneditor', file: 'jsoneditor.min.js')}"></script>
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>

<body>
    <div layout flex horizontal wrap around-justified>
        <div layout vertical>
            <votingsystem-button onclick="sendTransactionVSFromBankVS()" style="margin: 0px 0px 0px 5px;">
                TransactionVS from BankVS
            </votingsystem-button>
            <div id="transactionvsFromBankVS" style="width: 500px; height: 300px;"></div>
        </div>

        <div layout vertical>
            <div layout horizontal center center-justified>
                <votingsystem-button style="margin: 0px 0px 0px 5px;">TransactionVS </votingsystem-button>
                - http://vickets:8086/Vickets/transaction
            </div>
            <div id="transactionvsEditor" style="width: 500px; height: 300px;"></div>
        </div>

        <div layout vertical>
            New BankVS
            <div id="newBankVSEditor" style="width: 500px; height: 400px;"></div>
        </div>

    </div>
</body>
</html>
<asset:script>
    //date -> 'yyyy/MM/dd HH:mm:ss'
    var transactionvsFromBankVS = new JSONEditor(document.querySelector("#transactionvsFromBankVS"));
    var signedContent
    var jsonTransactionVSFromBankVS = {operation:"TRANSACTIONVS_FROM_BANKVS",
        signedMessageSubject:"TransactionVS from BankVS",
        signedContent:{operation:"TRANSACTIONVS_FROM_BANKVS", bankIBAN:"ES1877777777450000000050",
            fromUser: "ClientBankVS App1 App2", currency: "EUR",
            fromUserIBAN:"ES8477777777450000012345", toUserIBAN:["ES8978788989450000000004"],
            amount:"30000", tags:[{name:"HIDROGENO"}], isTimeLimited:true, subject:"BankVS transactionvs" },
        serviceURL:"${createLink( controller:'transactionVS', action:" ", absolute:true)}",
        serverURL:"${grailsApplication.config.grails.serverURL}",
        urlTimeStampServer:"${grailsApplication.config.VotingSystem.urlTimeStampServer}",
        }
    //fromUserIBAN -> IBAN external to Vicket System controlled by BankVS
    //if validTo present -> change to limited in time Vickets
    transactionvsFromBankVS.set(jsonTransactionVSFromBankVS);


    function sendTransactionVSFromBankVS() {
        console.log("transactionvsFromBankVS")
        var webAppMessage = transactionvsFromBankVS.get();
        webAppMessage.statusCode = ResponseVS.SC_PROCESSING
        webAppMessage.objectId = Math.random().toString(36).substring(7)
        window[webAppMessage.objectId] = function(appMessage) {
            console.log("sendTransactionVSFromBankVS - message: " + appMessage);
            appMessageJSON = JSON.parse(appMessage)
            showMessageVS(JSON.stringify(appMessageJSON.message), "sendTransactionVSFromBankVS " + appMessageJSON.statusCode)
        }
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }



    var transactionvsEditor = new JSONEditor(document.querySelector("#transactionvsEditor"));
    var jsonTransactionVSEditor = {  "operation": "TRANSACTIONVS_FROM_GROUP_TO_MEMBER", "amount": "10", "fromUser":
        "Cheques comida &apos;proyecto Vickets&apos;", "subject": "Transacción 'sábado' \"21 junio\" a 20C",
        "fromUserIBAN": "ES8978788989450000000004", "toUserIBAN":  [ "ES9478788989450000000011"], "currency": "EUR",
        "validTo": "2014/06/23 00:00:00"}
    transactionvsEditor.set(jsonTransactionVSEditor);



    var bankVSEditor = new JSONEditor(document.querySelector("#newBankVSEditor"));
    var certChain = "-----BEGIN CERTIFICATE-----\n" +
            "MIICuzCCAiSgAwIBAgIGAUZWGs81MA0GCSqGSIb3DQEBBQUAMD4xLDAqBgNVBAMM\n" +
            "I1ZvdGluZyBTeXN0ZW0gQ2VydGlmaWNhdGUgQXV0aG9yaXR5MQ4wDAYDVQQLDAVD\n" +
            "ZXJ0czAeFw0xNDA2MDEwNjI1MzNaFw0xNTAxMTgxNzU4NTNaMDoxEjAQBgNVBAUT\n" +
            "CTkwMDAwMDAwQjEkMCIGA1UEAxMbVm90aW5nIFN5c3RlbSBWaWNrZXQgU2VydmVy\n" +
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDAStOOwaNJ5egUrspY5tnT7asX\n" +
            "dcbwtOOFRrHFQJVUAry9CQBDBMl1UWhQ6dDyza/Etucn0RNKwe0V/YujVtALlSv8\n" +
            "PjSiEeC/MVXLIcF3VycydB6JO9Tb1jyt7edw2XawsOp6geGGbuCR2fLgn0bs3US4\n" +
            "6TyS+Yv4PAVh7m6AzQIDAQABo4HHMIHEMG0GA1UdIwRmMGSAFPDThkkuJ1MxJINP\n" +
            "guo7UNa0tkYpoUKkQDA+MSwwKgYDVQQDDCNWb3RpbmcgU3lzdGVtIENlcnRpZmlj\n" +
            "YXRlIEF1dGhvcml0eTEOMAwGA1UECwwFQ2VydHOCCFz8lN5fdijOMB0GA1UdDgQW\n" +
            "BBR3dG6BiPLQOll9QIJLirtT6tglJzAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQE\n" +
            "AwIFoDAWBgNVHSUBAf8EDDAKBggrBgEFBQcDCDANBgkqhkiG9w0BAQUFAAOBgQAx\n" +
            "XjUMuqVL7SYYlGyhJt61MW39O5jG6eXkDvbrBvvpi3In1wUQheh8SXws8BrsHyGx\n" +
            "iDMkmC/CTxCxAz9ASd+LxFWw/vfZ1NCd1muYkEpX7TPk07sNDGkF0K0wCBic6o2t\n" +
            "t3UeDZwwOEWugyyp2cDDI/16ApdAsorhs0urCo21dg==\n" +
            "-----END CERTIFICATE-----"
    var jsonBankVSEditor = {IBAN:'ES1877777777450000000050',
        operation:"BANKVS_NEW", info:"Información del nuevo banco",
        certChainPEM:certChain,
    };

    bankVSEditor.set(jsonBankVSEditor);
</asset:script>
<asset:deferredScripts/>