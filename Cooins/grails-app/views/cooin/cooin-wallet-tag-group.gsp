<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/vs-html-echo', file: 'vs-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-progress', file: 'paper-progress.html')}">

<polymer-element name="cooin-wallet-tag-group" attributes="tag, cooinArray">
    <template>
        <style>
            .tagHeader {font-size: 1.1em; text-decoration: underline;font-weight: bold;margin:5px 0 0 0;
                color: #432; text-align: center;}
            .cooin {
                background: #f9f9f9;
                color: #61a753;
                padding:10px;
                margin:10px;
                border-radius: 5px;
                border: 1px solid #61a753;
                font-size: 1.2em;
                text-align: center;
            }
            .cooin i {margin:5px 10px 0 0;}
            .core-tooltip {padding: 8px;background: #f9f9f9;border: 1px solid #61a753; color:#61a753; }
        </style>
        <g:include view="/include/styles.gsp"/>
        <div vertical layout center center-justified>
            <div class="tagHeader">{{tag | tagDescription}}</div>
            <template repeat="{{currencyMsg in currencyMsgs}}">
                <div style="margin: 5px 0 0 20px;color: #888;">{{currencyMsg}}</div>
            </template>
            <div>
                <template repeat="{{cooin in cooinArray}}">
                    <core-tooltip position="top" noarrow="false">
                        <div horizontal layout center center-justified class="cooin" on-click="{{showCooin}}">
                            <i class="fa {{cooin.currencyCode | currencyIcon}}" style=""></i>
                            {{cooin.cooinValue}} {{cooin.currencyCode}}
                            <template if="{{cooin | isTimeLimited}}">
                                <div style="font-size: 1em; margin: 0 0 0 5px;">
                                    <i class="fa fa-clock-o"></i>
                                </div>
                            </template>
                        </div>
                        <div tip>
                            <template if="{{cooin | isTimeLimited}}">
                                <div style="font-size: 2em; margin: 10px 10px 20px 10px;"><g:message code="expendBeforeMonday"/></div>
                            </template>
                            <div style="font-size: 1.4em; margin: 10px 10px 15px 10px;">{{cooin | dateInfo}}</div>
                            <div style="font-size: 1em; margin: 10px;">{{cooin.cooinServerURL}}</div>
                        </div>
                    </core-tooltip>
                </template>
            </div>
        </div>
    </template>
    <script>
        Polymer('cooin-wallet-tag-group', {
            //Info inside cooin: cooinServerURL, cooinValue, currencyCode, tag, notBefore, notAfter, hashCertVS
            currencyMsgs:[],
            publish: {
                cooinArray: {value: {}}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            tagDescription:function(tag) {
                if("WILDTAG" === tag) return "<g:message code="wildTagLbl"/>".toUpperCase()
                else return tag
            },
            dateInfo:function(cooin) {
                var notBefore = getDateFormatted(cooin.notBefore,"yyyy/MM/dd' 'HH:mm:ss", "dd MMM yyyy' 'HH:mm", null)
                var notAfter = getDateFormatted(cooin.notAfter,"yyyy/MM/dd' 'HH:mm:ss", "dd MMM yyyy' 'HH:mm", null)
                return "<g:message code="dateRangeMsg"/>".format(notBefore, notAfter)
            },
            isTimeLimited:function(cooin) {
                var notBefore = DateUtils.parse(cooin.notBefore)
                var notAfter = DateUtils.parse(cooin.notAfter)
                var dayDiff = notAfter.daydiff(notBefore)
                return dayDiff <= 7 //if lower than seven days -> timeLimited
            },
            currencyIcon: function(currencyCode) {
                switch(currencyCode) {
                    case 'EUR': return "fa-eur"
                    case 'USD': return "fa-dollar"
                    case 'JPY': return "fa-yen"
                    case 'CNY': return "fa-cny"
                    default: return "fa-money"
                }
            },
            cooinArrayChanged:function() {
                console.log(this.tagName + " - cooinArrayChanged")
                var tagGroupMap = {}
                this.currencyMsgs = []
                for(cooinIdx in this.cooinArray) {
                    var cooin = this.cooinArray[cooinIdx]
                    try {
                        //{{cooin.cooinValue}} {{cooin.currencyCode}} {{cooin.isTimeLimited}}
                        if(tagGroupMap[cooin.currencyCode]) {
                            if(cooin.isTimeLimited === true) {
                                tagGroupMap[cooin.currencyCode].isTimeLimited = addNumbers(
                                                tagGroupMap[cooin.currencyCode].isTimeLimited, cooin.cooinValue)
                            }
                            tagGroupMap[cooin.currencyCode].total = addNumbers(tagGroupMap[cooin.currencyCode].total, cooin.cooinValue)
                        } else {
                            var timeLimited = (cooin.isTimeLimited === true)?cooin.cooinValue:0
                            tagGroupMap[cooin.currencyCode] = {isTimeLimited: timeLimited, total:cooin.cooinValue}
                        }
                    } catch(ex) {console.log(Ex)}

                }
                var currencies = Object.keys(tagGroupMap)
                console.log("currencies: " + currencies)
                for(currencyIdx in currencies) {
                    var currency = currencies[currencyIdx]
                    var msg = currency + ". <g:message code="totalLbl"/>: " + tagGroupMap[currency].total
                    if(tagGroupMap[currency].isTimeLimited > 0) msg = msg +
                            " (<g:message code="timeLimitedForTagShortMsg"/>)".format(tagGroupMap[currency].isTimeLimited)
                    this.currencyMsgs.push(msg)
                }
            },
            showCooin: function(e) {
                var webAppMessage = new WebAppMessage(Operation.OPEN_COOIN)
                webAppMessage.document = e.target.templateInstance.model.cooin
                webAppMessage.setCallback(function(appMessage) {
                    console.log("showCooin - message: " + appMessage);
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>


