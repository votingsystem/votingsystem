<%@page contentType="text/javascript" %>

    var contextURL = "${contextURL}"

    function OperationVS(operation, statusCode) {
        this.statusCode = statusCode == null ? 700: statusCode; //700 -> ResponseVS.SC_PROCESSING
        this.operation = operation
        this.receiverName = "${serverName}";
        this.serverURL = "${contextURL}";
        this.callerCallback = Math.random().toString(36).substring(7);
    }

    OperationVS.prototype.setCallback = function(callbackFunction) {
        document.querySelector("#voting_system_page").addEventListener(this.callerCallback, function(e) { callbackFunction(e.detail) });
    }

    function EventVS() {}

    EventVS.State = {
        ACTIVE:"ACTIVE",
        TERMINATED:"TERMINATED",
        CANCELED:"CANCELED",
        PENDING:"PENDING",
        PENDING_SIGNATURE:"PENDING_SIGNATURE",
        DELETED_FROM_SYSTEM:"DELETED_FROM_SYSTEM"
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

    window.addEventListener('WebComponentsReady', function() {
        Polymer.Base.importHref(contextURL + '/element/alert-dialog.vsp', function(e) {
            var alertDialog = document.createElement('alert-dialog');
            window.alert = function(message, caption, callerId, isConfirmMessage) {
                if(!caption) caption = "${msg.messageLbl}"
                alertDialog.setMessage(message, caption, callerId, isConfirmMessage)
            }
            document.querySelector("body").appendChild(alertDialog)
        });
    })

    var weekdays = [${msg.weekdaysShort}];
    var months = [${msg.monthsShort}];

    Date.prototype.getDayWeekFormat = function() {
        return weekdays[this.getDay()] + " " + this.getDate() + " " + months[ this.getMonth()] + " " + this.getFullYear();
    };

    Date.prototype.getDayWeekAndHourFormat = function() {
        return this.getDayWeekFormat() + " - " + this.getHours() + ":" + this.getMinutes();
    };
