<%@page contentType="text/javascript; charset=UTF-8" %>
    var vs = {}
    vs.contextURL = "${contextURL}"
    vs.timeStampServiceURL = "${timeStampServerURL}" + "/timestamp"

    function OperationVS(operation, statusCode) {
        this.statusCode = statusCode == null ? 700: statusCode; //700 -> ResponseVS.SC_PROCESSING
        this.operation = operation
        this.receiverName = "${serverName}";
        this.serverURL = "${contextURL}";
    }

    OperationVS.prototype.setCallback = function(callbackFunction) {
        this.callerCallback = Math.random().toString(36).substring(7);
        document.querySelector("#voting_system_page").addEventListener(this.callerCallback, function(e) { callbackFunction(e.detail) });
        return this
    }

    //http://jsfiddle.net/cckSj/5/
    Date.prototype.getElapsedTime = function() {
        // time difference in ms
        var timeDiff = this - new Date();

        if(timeDiff <= 0) {
            return "${msg.timeFinishedLbl}"
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

    var transactionsMap = {
        CURRENCY_REQUEST:{lbl:"${msg.selectCurrencyRequestLbl}"},
        CURRENCY_SEND:{lbl:"${msg.selectCurrencySendLbl}"},
        CURRENCY_PERIOD_INIT:{lbl:"${msg.currencyPeriodInitLbl}"},
        CURRENCY_PERIOD_INIT_TIME_LIMITED:{lbl:"${msg.lapsedLbl}"},
        FROM_BANK:{lbl:"${msg.bankInputLbl}"},
        FROM_USER:{lbl:"${msg.transactionFromUser}"}
    }


    vs.getCurrencySymbol = function(currencyCode) {
        switch (currencyCode) {
            case "EUR": return '€';
            case "USD": return '$';
            case "CNY":
            case "JPY":
                return '¥';
        }
    }

    window.addEventListener('WebComponentsReady', function() {
        Polymer.Base.importHref(vs.contextURL + '/element/alert-dialog.vsp', function(e) {
            var alertDialog = document.createElement('alert-dialog');
            window.alert = function(message, caption, callerId, isConfirmMessage) {
                if(!caption) caption = "${msg.messageLbl}"
                alertDialog.setMessage(message, caption, callerId, isConfirmMessage)
            }
            document.querySelector("body").appendChild(alertDialog)
        });
    })

    vs.weekdays = [${msg.weekdaysShort}];
    vs.months = [${msg.monthsShort}];

    Date.prototype.getDayWeekFormat = function() {
        var currentDate = new Date()
        if(currentDate.getFullYear() === this.getFullYear()) {
            return vs.weekdays[this.getDay()] + " " + this.getDate() + " " + vs.months[ this.getMonth()];
        } else return this.getDate() + " " + vs.months[ this.getMonth()] + " " + this.getFullYear();
    };

    Date.prototype.getDayWeekAndHourFormat = function() {
        return this.getDayWeekFormat() + " " + pad(this.getHours(), 2) + ":" + pad(this.getMinutes(), 2);
    };

    Number.prototype.getDayWeekFormat = function() {
        return new Date(this).getDayWeekFormat()
    }

    Number.prototype.getDayWeekAndHourFormat = function() {
        return new Date(this).getDayWeekAndHourFormat()
    }