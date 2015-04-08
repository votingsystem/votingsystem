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
            this.receiverName = "${config.serverName}";
            this.serverURL = "${elementURL}";
            this.timeStampServerURL = "${config.timeStampServerURL}"
            this.objectId = Math.random().toString(36).substring(7);
        }

        WebAppMessage.prototype.setCallback = function(callbackFunction) {
            window[this.objectId] = callbackFunction;
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
