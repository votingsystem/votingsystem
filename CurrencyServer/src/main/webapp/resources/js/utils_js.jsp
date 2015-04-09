<%@page contentType="text/javascript" %>

    window['serverURL'] = "${contextURL}"

    function WebAppMessage(operation, statusCode) {
        this.statusCode = statusCode == null ? 700: statusCode; //700 -> ResponseVS.SC_PROCESSING
        this.operation = operation
        this.caption;
        this.message;
        this.subject ;
        this.signedContent;
        this.serviceURL;
        this.documentURL;
        this.receiverName = "${serverName}";
        this.serverURL = "${contextURL}";
        this.timeStampServerURL = "${timeStampServerURL}"
        this.objectId = Math.random().toString(36).substring(7);
    }

    WebAppMessage.prototype.setCallback = function(callbackFunction) {
        window[this.objectId] = callbackFunction;
    }


    function updateLinksVS(elementsArray) {
        for (var i = 0; i < elementsArray.length; i++) {
            //console.log("elementsArray[i].href: " + elementsArray[i].href)
            if(elementsArray[i].href.indexOf("${contextURL}") > -1) {
                elementsArray[i].addEventListener('click', function(e) {
                    document.querySelector('#navBar').loadURL(e.target.href)
                    e.preventDefault()
                });
            } else if("" != elementsArray[i].href.trim()) console.log("utils_js - not system url: " + elementsArray[i].href)
        }
    }

    //http://jsfiddle.net/cckSj/5/
    Date.prototype.getElapsedTime = function() {
        // time difference in ms
        var timeDiff = this - new Date();

        if(timeDiff <= 0) {
            return "${msg.timeFinsishedLbl}"
        }

        // strip the miliseconds
        timeDiff /= 1000;

        // get seconds
        var seconds = Math.round(timeDiff % 60);

        // remove seconds from the date
        timeDiff = Math.floor(timeDiff / 60);

        // get minutes
        var minutes = Math.round(timeDiff % 60);

        // remove minutes from the date
        timeDiff = Math.floor(timeDiff / 60);

        // get hours
        var hours = Math.round(timeDiff % 24);

        // remove hours from the date
        timeDiff = Math.floor(timeDiff / 24);

        // the rest of timeDiff is number of days
        var resultStr
        var days = timeDiff;
        if(days > 0) {
            resultStr = days + " " + "${msg.daysLbl}" + " " + "${msg.andLbl}" + " " + hours + " " + "${msg.hoursLbl}"
        } else if (hours > 0) {
            resultStr = hours + " " + "${msg.hoursLbl}" + " " + "${msg.andLbl}" + " " + minutes + " " + "${msg.minutesLbl}"
        } else if (minutes > 0) {
            resultStr = minutes + " " + "${msg.minutesLbl}" + " " + "${msg.andLbl}" + " " + seconds + " " + "${msg.secondsLbl}"
        }
        return resultStr
    };

    function getTransactionVSDescription(transactionType) {
        var transactionDescription = "transactionDescription"
        switch(transactionType) {
            case 'CURRENCY_REQUEST':
                transactionDescription = "${msg.selectCurrencyRequestLbl}"
                break;
            case 'CURRENCY_SEND':
                transactionDescription = "${msg.selectCurrencySendLbl}"
                break;
            case 'CURRENCY_INIT_PERIOD':
                transactionDescription = "${msg.currencyInitPeriodLbl}"
                break;
            case 'CURRENCY_INIT_PERIOD_TIME_LIMITED':
                transactionDescription = "${msg.currencyInitPeriodTimeLimitedLbl}"
                break;
            case 'FROM_BANKVS':
                transactionDescription = "${msg.bankVSInputLbl}"
                break;
            case 'FROM_USERVS':
                transactionDescription = "${msg.transactionVSFromUserVS}"
                break;
            case 'FROM_GROUP_TO_MEMBER':
                transactionDescription = "${msg.transactionVSFromGroupToMember}"
                break;
            case 'FROM_GROUP_TO_MEMBER_GROUP':
                transactionDescription = "${msg.transactionVSFromGroupToMemberGroup}"
                break;
            case 'FROM_GROUP_TO_ALL_MEMBERS':
                transactionDescription = "${msg.transactionVSFromGroupToAllMembers}"
                break;
            default: transactionDescription = transactionType
        }
        return transactionDescription
    }

    window._originalAlert = window.alert;
    window.alert = function(text) {
        if (document.querySelector("#_votingsystemMessageDialog") != null && typeof
                document.querySelector("#_votingsystemMessageDialog").setMessage != 'undefined'){
            document.querySelector("#_votingsystemMessageDialog").setMessage(text,
                    "${msg.messageLbl}")
        }  else {
            console.log('utils_js - alert-dialog not found');
            window._originalAlert(text);
        }
    }
    window.sendAndroidURIMessage = function(encodedData) {
        document.querySelector("#_votingsystemMessageDialog").sendAndroidURIMessage(encodedData)
    }

    function getDateFormatted(dateToFormat, dateFormat, stringFormat, callback) {
        var result
        var webAppMessage = new WebAppMessage(Operation.FORMAT_DATE)
        webAppMessage.document = {dateStr: dateToFormat, dateFormat:dateFormat, stringFormat:stringFormat}
        webAppMessage.setCallback(callback)
        try {
            result = VotingSystemClient.call(webAppMessage);
        } catch(ex) { } finally {
            return result || dateToFormat
        }
    }
