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

    <img src="http://cooins:8086/Cooins/QR?cht=qr&chs=250x250&chl=${paymentInfoServiceURL}" alt="read it with your mobile" height="250" width="250">

    <template is="auto-binding">
        <core-ajax auto response="{{data}}" on-core-response="{{ajaxHandler}}"
                   url="${createLink(controller:'shopExample', action:"listenTransactionChanges", absolute:true)}" handleAs="json"></core-ajax>
        <h3>{{data.message}}</h3>
    </template>
</body>
</html>