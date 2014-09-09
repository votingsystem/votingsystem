<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="receipt-votevs">
    <template>
        <style></style>
        <div layout vertical style="margin: 10px auto; max-width:1000px;">
            <div><b><g:message code="eventVSLbl"/>: </b><a href="{{receipt.eventURL}}">{{receipt.eventURL}}</a></div>
            <div><b><g:message code="optionSelectedLbl"/>: </b>{{receipt.optionSelected.content}}</div>
        </div>
    </template>
    <script>
        Polymer('receipt-votevs', {
            publish: {
                receipt: {value: {}}
            },
            receiptChanged:function() {
                console.log("this.receipt: " + this.receipt)
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            }
        });
    </script>
</polymer-element>