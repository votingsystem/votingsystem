<!DOCTYPE html> <%@ page contentType="text/html; charset=UTF-8" %> <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%> <html>
<head>
    <title>ShopExampleController</title>
    <script src="${resourceURL}/webcomponentsjs/webcomponents.min.js" type="text/javascript"></script>
    <link href="${resourceURL}/polymer/polymer.html" rel="import"/>
    <link href="${resourceURL}/core-ajax/core-ajax.html" rel="import"/>
</head>
<script>
    addEventListener('template-bound', function(e) {
        console.log('template-bound');
        e.target.ajaxHandler = function(event) {
            console.log(event.target.response);
    }});
</script>
<body>
    <template is="auto-binding">
        <div vertical layout>
            <div horizontal layout>
                <h3>
                    ${msg.transactionValueLbl}:
                    ${transactionRequest.getAmount()} ${transactionRequest.getCurrencyCode()} - ${transactionRequest.getTagVS()}
                </h3>
            </div>
            ${msg.paymentOptionsLbl}: ${transactionRequest.paymentOptions}
            <img src="http://currencyserver:8086/CurrencyServer/QR?cht=qr&chs=250x250&chl=${paymentInfoServiceURL}" alt="read it with your mobile" height="250" width="250">
            <core-ajax auto response="{{data}}" on-core-response="{{ajaxHandler}}" handleAs="json"
                       url="${restURL}/shopExample/listenTransactionChanges/${shopSessionID}"></core-ajax>
            <h3>{{data.message}}</h3>
        </div>
    </template>
</body>
</html>