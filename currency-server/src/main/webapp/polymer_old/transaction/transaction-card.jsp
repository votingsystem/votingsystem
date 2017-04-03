<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transaction-card">
    <template>
        <style>
        .card {
            position: relative;
            display: inline-block;
            width: 350px;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid rgba(0, 0, 0, 0.24);
            margin: 10px;
            color: #667;
            cursor: pointer;
        }
        .date {margin:3px 10px 0 0; color: #888; font-size: 0.8em;}
        .transactionDescription {color:#621; font-size: 1.2em; text-decoration: underline;}
        .subject {color: #888; margin: 3px 3px 5px 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 0.8em;}
        .amount {color: #f9f9f9; background:#621; font-weight: bold; padding: 1px 5px 1px 5px;font-size: 0.9em;}
        .tag {color:#388746; margin: 0 0 0 5px;font-size: 0.9em;}
        .timeInfo {color:#621; text-transform: uppercase; text-align: right; margin: 0 5px 0 0; font-size: 0.8em;}
        .expenseTrans { background: #fee; }
        </style>
        <div on-click="showTransactionDetails" class="card {{getClass(transaction)}}">
            <div class="horizontal layout">
                <div class="flex horizontal layout center-justified transactionDescription">{{transactionDescription(transaction)}}</div>
                <div class="date">{{getDate(transaction.dateCreated)}}</div>
            </div>
            <div id="subjectDiv" class="subject horizontal layout center-justified">{{getSubject(transaction)}}</div>
            <div class="horizontal layout">
                <div class="amount">{{amount(transaction)}}</div>
                <div class="tag">{{tag}}</div>
                <div class="flex" class="timeInfo">{{timeLimitedDescription(transaction)}}</div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'transaction-card',
            properties: {
                transaction: {type:Object, observer:'transactionChanged'}
            },
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            getSubject: function(transaction) {
                this.isConfirmMessage = this.isConfirmMessage || false
                if("${msg.selectCurrencyRequestLbl}" === transaction.subject) return null
                else return transaction.subject
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            getClass: function(transaction) {
                if(!this.isUserTable) return
                if(transaction.fromUserName && transaction.fromUserName.nif == this.userNif) {
                    return "expenseTrans"
                } else return ""
            },
            timeLimitedDescription: function(transaction) {
                if(transaction.timeLimited === true || transaction.validTo != null) return "${msg.timeLimitedLbl}"
                else if  (transaction.timeLimited === false) return ""
                else return transaction.timeLimited
            },
            transactionDescription: function(transaction) {
                var result = null
                if(this.userNif) {
                    if(transaction.fromUserName && transaction.fromUserName.nif == this.userNif) {
                        result = "${msg.spendingLbl} - "
                    }
                    if(transaction.toUserName && transaction.toUserName.nif == this.userNif) {
                        result = result? result + "${msg.incomeLbl} - " : "${msg.incomeLbl} - "
                    }
                }
                var transactionTypeLbl = transactionsMap[transaction.type].lbl
                return result? result.toUpperCase() + transactionTypeLbl : transactionTypeLbl
            },
            amount: function(transaction) {
                var amount
                if(isNaN(transaction.amount)) amount = transaction.amount.toAmountStr() + " " + transaction.currency
                else  amount = transaction.amount + " " + transaction.currencyCode
                return amount
            },
            transactionChanged: function(e) {
                this.tag = this.transaction.tags[0]
            },
            showTransactionDetails: function(e) {
                console.log("cmsMessageURL: " + this.transaction.cmsMessageURL)
                page.show(this.transaction.cmsMessageURL)
            }
        });
    </script>
</dom-module>