<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/transactionVS/transactionvs-card']"/>">


<polymer-element name="transactionvs-list" attributes="url transactionsMap">
<template>
    <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get"
               contentType="json"  on-core-complete="{{ajaxComplete}}"></core-ajax>
    <div layout flex horizontal wrap around-justified>
        <template repeat="{{transaction in transactionRecords}}">
            <transactionvs-card transaction="{{transaction}}"></transactionvs-card>
        </template>
    </div>
</template>
<script>
    Polymer('transactionvs-list', {
        publish: {
            transactionsMap: {value: {}}
        },
        ready :  function(e) { console.log(this.tagName + " - ready") },
        urlChanged:function() {
            console.log(this.tagName + " - urlChanged: " + this.url)
        },
        transactionsMapChanged:function() {
            console.log(this.tagName + " - transactionsMapChanged - transactionsMap: " +
                    Object.prototype.toString.call(this.transactionsMap))
            this.transactionsMap = toJSON(this.transactionsMap)
            this.transactionRecords = this.transactionsMap.transactionRecords
        },
        ajaxComplete: function() {
            this.transactionsMap = this.responseData
            console.log(this.tagName + " - ajaxComplete")
        },
        addTransaction:function(transactionvs) {
            this.transactionsMap.transactionRecords.push(transactionvs)
        }
    });</script>
</polymer-element>
