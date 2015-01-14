<!DOCTYPE html>
<html>
<head>
    <title>ShopExampleController</title>
    <vs:webscript dir='webcomponentsjs' file="webcomponents.min.js"/>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <vs:webresource dir="core-ajax" file="core-ajax.html"/>
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
                    <g:message code="transactionValueLbl"/>:
                    ${transactionRequest.getAmount()} ${transactionRequest.getCurrencyCode()} - ${transactionRequest.getTagVS()}
                </h3>
            </div>
            <g:message code="paymentOptionsLbl"/>: ${transactionRequest.paymentOptions}
            <img src="http://cooins:8086/Cooins/QR?cht=qr&chs=250x250&chl=${paymentInfoServiceURL}" alt="read it with your mobile" height="250" width="250">
            <core-ajax auto response="{{data}}" on-core-response="{{ajaxHandler}}" handleAs="json"
                       url="${createLink(controller:'shopExample', action:"listenTransactionChanges", absolute:true)}/${shopSessionID}"></core-ajax>
            <h3>{{data.message}}</h3>
        </div>
    </template>
</body>
</html>