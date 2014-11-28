<script>
    if(${grails.util.Metadata.current.isWarDeployed()}) {
        window['accessControlURL'] = "${grailsApplication.config.vs.prod.accessControlURL}"
    } else  window['accessControlURL'] = "${grailsApplication.config.vs.dev.accessControlURL}"

    window['serverURL'] = "${grailsApplication.config.grails.serverURL}"
    window.CKEDITOR_BASEPATH = '${grailsApplication.config.grails.serverURL}/bower_components/ckeditor/';

    function WebAppMessage(operation, statusCode) {
        this.statusCode = statusCode == null ? 700: statusCode; //700 -> ResponseVS.SC_PROCESSING
        this.operation = operation
        this.caption;
        this.message;
        this.subject ;
        this.signedContent;
        this.serviceURL;
        this.documentURL;
        this.receiverName = "${grailsApplication.config.vs.serverName}";
        this.serverURL = "${grailsApplication.config.grails.serverURL}";
        this.urlTimeStampServer = "${grailsApplication.config.vs.urlTimeStampServer}"
        this.objectId = Math.random().toString(36).substring(7);
    }

    WebAppMessage.prototype.setCallback = function(callbackFunction) {
        window[this.objectId] = callbackFunction;
    }


    function updateLinksVS(elementsArray) {
        for (var i = 0; i < elementsArray.length; i++) {
            //console.log("elementsArray[i].href: " + elementsArray[i].href)
            if(elementsArray[i].href.indexOf("${grailsApplication.config.grails.serverURL}") > -1) {
                elementsArray[i].addEventListener('click', function(e) {
                    document.querySelector('#navBar').loadURL(e.target.href)
                    e.preventDefault()
                });
            } else if("" != elementsArray[i].href.trim()) console.log("main.gsp - not system url: " + elementsArray[i].href)
        }
    }

    //http://jsfiddle.net/cckSj/5/
    Date.prototype.getElapsedTime = function() {
        // time difference in ms
        var timeDiff = this - new Date();

        if(timeDiff <= 0) {
            return "<g:message code='timeFinsishedLbl'/>"
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
            resultStr = days + " " + "<g:message code="daysLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + hours + " " + "<g:message code="hoursLbl"/>"
        } else if (hours > 0) {
            resultStr = hours + " " + "<g:message code="hoursLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + minutes + " " + "<g:message code="minutesLbl"/>"
        } else if (minutes > 0) {
            resultStr = minutes + " " + "<g:message code="minutesLbl"/>" + " " + "<g:message code="andLbl"/>" + " " + seconds + " " + "<g:message code="secondsLbl"/>"
        }
        return resultStr
    };

    function getTransactionVSDescription(transactionType) {
        var transactionDescription = "transactionDescription"
        switch(transactionType) {
            case 'VICKET_REQUEST':
                transactionDescription = "<g:message code="selectVicketRequestLbl"/>"
                break;
            case 'VICKET_SEND':
                transactionDescription = "<g:message code="selectVicketSendLbl"/>"
                break;
            case 'VICKET_CANCELLATION':
                transactionDescription = "<g:message code="selectVicketCancellationLbl"/>"
                break;
            case 'VICKET_INIT_PERIOD':
                transactionDescription = "<g:message code="vicketInitPeriodLbl"/>"
                break;
            case 'VICKET_INIT_PERIOD_TIME_LIMITED':
                transactionDescription = "<g:message code="vicketInitPeriodTimeLimitedLbl"/>"
                break;
            case 'FROM_BANKVS':
                transactionDescription = "<g:message code="bankVSInputLbl"/>"
                break;
            case 'FROM_USERVS':
                transactionDescription = "<g:message code="transactionVSFromUserVS"/>"
                break;
            case 'FROM_GROUP_TO_MEMBER':
                transactionDescription = "<g:message code="transactionVSFromGroupToMember"/>"
                break;
            case 'FROM_GROUP_TO_MEMBER_GROUP':
                transactionDescription = "<g:message code="transactionVSFromGroupToMemberGroup"/>"
                break;
            case 'FROM_GROUP_TO_ALL_MEMBERS':
                transactionDescription = "<g:message code="transactionVSFromGroupToAllMembers"/>"
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
                    "<g:message code="messageLbl"/>")
        }  else {
            console.log('utils_js.gsp - alert-dialog not found');
            window._originalAlert(text);
        }
    }
    window.sendAndroidURIMessage = function(encodedData) {
        document.querySelector("#_votingsystemMessageDialog").sendAndroidURIMessage(encodedData)
    }

    function getDateFormatted(dateToFormat, dateFormat, stringFormat, callback) {
        var result
        var webAppMessage = new WebAppMessage( Operation.FORMAT_DATE)
        webAppMessage.document = {dateStr: dateToFormat, dateFormat:dateFormat, stringFormat:stringFormat}
        webAppMessage.setCallback(callback)
        try {
            result = VotingSystemClient.call(webAppMessage);
        } catch(ex) { } finally {
            return result || dateToFormat
        }
    }

</script>