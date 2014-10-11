<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-html-echo', file: 'votingsystem-html-echo.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-dialog', file: 'votingsystem-dialog.html')}">


<polymer-element name="vicket-tag-group" attributes="tag, vicketArray">
    <template>
        <style>
            .tagHeader {font-size: 1.1em; text-decoration: underline;font-weight: bold;margin:5px 0 0 0;
                color: #432; text-align: center;}
            .vicket {
                background: #f9f9f9;
                color: #61a753;
                padding:10px;
                margin:10px;
                border-radius: 5px;
                border: 1px solid #61a753;
                font-size: 2em;
                text-align: center;
            }
            .vicket i {margin:5px 10px 0 0;}
            .core-tooltip {padding: 8px;background: #f9f9f9;border: 1px solid #61a753; color:#61a753; }
        </style>
        <g:include view="/include/styles.gsp"/>
        <div style="max-width: 700px;">
            <div class="tagHeader">{{tag | tagDescription}}</div>
            <template repeat="{{vicket in vicketArray}}">
                <core-tooltip position="right" noarrow="false">
                    <div horizontal layout center center-justified class="vicket">
                        <i class="fa {{vicket.currencyCode | currencyIcon}}" style=""></i>
                        {{vicket.vicketValue}} {{vicket.currencyCode}}
                        <template if="{{vicket | isTimeLimited}}">
                            <div style="font-size: 1em; margin: 0 0 0 5px;">
                                <i class="fa fa-clock-o"></i>
                            </div>
                        </template>
                    </div>
                    <div tip>
                        <template if="{{vicket | isTimeLimited}}">
                            <div style="font-size: 2em; margin: 10px 10px 20px 10px;"><g:message code="expendBeforeMonday"/></div>
                        </template>
                        <div id="{{vicket.hashCertVS}}" style="font-size: 1.4em; margin: 10px 10px 15px 10px;">{{vicket | dateInfo}}</div>
                        <div style="font-size: 1.2em; margin: 10px;"><g:message code="serverLbl"/>: {{vicket.vicketServerURL}}</div>
                        <div style="font-size: 1.2em; margin: 10px;">hashCertVS: {{vicket.hashCertVS}}</div>
                    </div>
                </core-tooltip>
            </template>
        </div>


    </template>
    <script>
        Polymer('vicket-tag-group', {
            //Info inside vicket: vicketServerURL, vicketValue, currencyCode, tag, notBefore, notAfter, hashCertVS
            publish: {
                vicketArray: {value: {}}
            },
            tagDescription:function(tag) {
                if("WILDTAG" === tag) return "<g:message code="wildTagLbl"/>".toUpperCase()
                else return tag
            },
            dateInfo:function(vicket) {
                var notBefore = getDateFormatted(vicket.notBefore,"yyyy/MM/dd' 'HH:mm:ss", null, null)
                var notAfter = getDateFormatted(vicket.notAfter,"yyyy/MM/dd' 'HH:mm:ss", null, null)
                return "<g:message code="dateRangeMsg"/>".format(notBefore, notAfter)
            },
            isTimeLimited:function(vicket) {
                var notBefore = DateUtils.parse(vicket.notBefore)
                var notAfter = DateUtils.parse(vicket.notAfter)
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
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            vicketArrayChanged:function() {
                console.log(this.tagName + " - vicketArrayChanged: " + JSON.stringify(this.vicketArray))
            }
        });
    </script>
</polymer-element>


