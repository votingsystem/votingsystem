<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">

<polymer-element name="transactionvs-selector">
    <template>
        <g:include view="/include/styles.gsp"/>
        <select id="transactionvsTypeSelect" style="margin:0px auto 10px auto;color:black; max-width: 400px;" class="form-control"
                on-change="{{transactionvsTypeSelect}}">
            <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>
            <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
            <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
            <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
            <option value="VICKET_INIT_PERIOD"> - <g:message code="vicketInitPeriodLbl"/> - </option>
            <option value="VICKET_INIT_PERIOD_TIME_LIMITED"> - <g:message code="vicketInitPeriodTimeLimitedLbl"/> - </option>
            <option value="FROM_BANKVS"> - <g:message code="bankVSInputLbl"/> - </option>
            <option value="FROM_USERVS"> - <g:message code="transactionVSFromUserVS"/> - </option>
            <option value="FROM_GROUP_TO_MEMBER"> - <g:message code="transactionVSFromGroupToMember"/> - </option>
            <option value="FROM_GROUP_TO_MEMBER_GROUP"> - <g:message code="transactionVSFromGroupToMemberGroup"/> - </option>
            <option value="FROM_GROUP_TO_ALL_MEMBERS"> - <g:message code="transactionVSFromGroupToAllMembers"/> - </option>
        </select>
    </template>
    <script>
        Polymer('transactionvs-selector', {
            ready: function() { console.log(this.tagName + " - ready")},
            transactionvsTypeSelect:function() {
                this.fire("selected", this.$.transactionvsTypeSelect.value)
                this.fire('core-signal', {name: "transactionvs-selector-selected", data: this.$.transactionvsTypeSelect.value});
            }
        });
    </script>
</polymer-element>
