<script>
    window['serverURL'] = "${grailsApplication.config.grails.serverURL}"

    function EventVS() {}

    EventVS.State = {
        ACTIVE:"ACTIVE",
        TERMINATED:"TERMINATED",
        CANCELLED:"CANCELLED",
        PENDING:"PENDING",
        PENDING_SIGNATURE:"PENDING_SIGNATURE",
        DELETED_FROM_SYSTEM:"DELETED_FROM_SYSTEM"
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


    window._originalAlert = window.alert;
    window.alert = function(text) {
        if (document.querySelector("#_votingsystemMessageDialog") != null && typeof
                document.querySelector("#_votingsystemMessageDialog").setMessage != 'undefined'){
            document.querySelector("#_votingsystemMessageDialog").setMessage(text,
                    "<g:message code="messageLbl"/>")
        }  else {
            console.log('alert-dialog not found');
            window._originalAlert(text);
        }
    }

</script>