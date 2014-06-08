<script>
    var SocketService = function () {

        this.socket = null;

        this.connect = function () {
            var host = "${grailsApplication.config.webSocketURL}"
            console.log("SocketService - Connecting to host " + host + " ...")
            if ('WebSocket' in window) {
                this.socket = new WebSocket(host);
            } else if ('MozWebSocket' in window) {
                this.socket = new MozWebSocket(host);
            } else {
                console.log('SocketService - browserWithoutWebsocketSupport');
                return
            }
            this.socket.onopen = function () {
                console.log('SocketService - socket.onopen - host ' + host);
            };

            this.socket.onclose = function (event) {
                console.log('SocketService - socket.onclose - event: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));

            };
        }

        this.sendMessage = function(message) {
            var messageStr = JSON.stringify(message);
            console.log("SocketService - sendMessage: " + messageStr)
            if(this.socket == null || 3 == this.socket.readyState) {
                console.log("SocketService - missing message - socket closed")
            } else if(messageStr != ''){
                this.socket.send(messageStr);
            }
        }

        this.close = function() {
            //states: CONNECTING	0, OPEN	1, CLOSING	2, CLOSED	3
            if(this.socket == null || 3 == this.socket.readyState) {
                console.log("SocketService - socket already closed")
                return
            } else console.log("SocketService - closing socket connection")
            this.socket.close()
        }

    };

    var dynatableInputs = {
        queries: null,
        sorts: null,
        multisort: ['ctrlKey', 'shiftKey', 'metaKey'],
        page: null,
        queryEvent: 'blur change',
        recordCountTarget: null,
        recordCountPlacement: 'after',
        paginationLinkTarget: null,
        paginationLinkPlacement: 'after',
        paginationPrev: '«',
        paginationNext: '»',
        paginationGap: [1,2,2,1],
        searchTarget: null,
        searchPlacement: 'before',
        perPageTarget: null,
        perPagePlacement: 'before',
        perPageText: '',
        recordCountText: '',
        pageText:'',
        recordCountPageBoundTemplate: '{pageLowerBound} a {pageUpperBound} de',
        recordCountTotalTemplate: '{recordsQueryCount}',
        processingText: '<span class="dynatableLoading">"<g:message code="updatingLbl"/><i class="fa fa-refresh fa-spin"></i></span>'
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
</script>