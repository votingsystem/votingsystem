<script>
    window['serverURL'] = "${grailsApplication.config.grails.serverURL}"

    var SocketService = function () {

        this.socket = null;

        this.connect = function () {
            var host = "${grailsApplication.config.grails.serverURL}/websocket/service".replace('http', 'ws')
            if ('WebSocket' in window) {
                this.socket = new WebSocket(host);
            } else if ('MozWebSocket' in window) {
                this.socket = new MozWebSocket(host);
            } else {
                console.log('browserWithoutWebsocketSupport');
                return
            }
            this.socket.onopen = function () {
                console.log('Info: WebSocket connection opened');
            };

            this.socket.onclose = function (event) {
                console.log('Info: WebSocket connection closed, Code: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));

            };
        }

        this.sendMessage = function(message) {
            var messageStr = JSON.stringify(message);
            console.log("sendMessage to simulation service: " + messageStr)
            if(this.socket == null || 3 == this.socket.readyState) {
                console.log("missing message - socket closed")
            } else if(messageStr != ''){
                this.socket.send(messageStr);
            }
        }

        this.close = function() {
            //states: CONNECTING	0, OPEN	1, CLOSING	2, CLOSED	3
            if(this.socket == null || 3 == this.socket.readyState) {
                console.log("socket already closed")
                return
            } else console.log(" closing socket connection")
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
    EventVS.State = {
        ACTIVE:"ACTIVE",
        TERMINATED:"TERMINATED",
        CANCELLED:"CANCELLED",
        AWAITING:"AWAITING",
        PENDING_SIGNATURE:"PENDING_SIGNATURE",
        DELETED_FROM_SYSTEM:"DELETED_FROM_SYSTEM"
    }

    function EventVS(eventJSON, eventTemplate, subSystem) {

        if(eventJSON != null) {
            this.id = eventJSON.id
            this.dateFinish = eventJSON.dateFinish
            this.dateBegin = eventJSON.dateBegin
            this.userVS = eventJSON.userVS
            this.state = eventJSON.state
            this.subject = eventJSON.subject
            this.checkDateURL ="${createLink( controller:'eventVSElection')}/checkDates?id=" + this.id
            this.isActive = DateUtils.checkDate(this.dateBegin.getDate(), this.dateFinish.getDate())
            if(EventVS.State.ACTIVE == this.state) {
                if(!this.isActive) httpGet(this.checkDateURL)
            } else if(EventVS.State.AWAITING == this.state) {
                if(this.isActive) httpGet(this.checkDateURL)
            }
        }
        this.subSystem = subSystem
        this.dateCreated
        this.backupAvailable
        this.type
        this.operation
        this.hashAccessRequestBase64
        this.hashSolicitudAccesoHex
        this.hashCertVSBase64
        this.hashCertVoteHex
        this.cardinality
        this.accessControl
        this.controlCenterVS
        this.fieldsEventVS
        this.optionSelected
        this.duracion
        this.urlPDF
        this.URL
        this.numSignatures
        this.content
        this.fieldsEventVS
        if(eventTemplate != null) {
            var subjectStr = this.subject.substring(0,50) +  ((this.subject.length > 50)? "...":"");
            this.eventHTML = eventTemplate.format(subjectStr, this.userVS, this.dateBegin,
                    this.dateFinish.getElapsedTime(), this.getMessage());
        }


    }

    EventVS.prototype.getURL = function() {
        var result
        if(this.subSystem == SubSystem.VOTES) result = "${createLink( controller:'eventVSElection')}/" + this.id
        if(this.subSystem == SubSystem.CLAIMS) result = "${createLink( controller:'eventVSClaim')}/" + this.id
        if(this.subSystem == SubSystem.MANIFESTS) result = "${createLink( controller:'eventVSManifest')}/" + this.id
        return result;
    }

    EventVS.prototype.getMessage = function () {
        var result =  "";
        if(EventVS.State.ACTIVE == this.state) {
            result = "<g:message code='openLbl'/>";
        } else if(EventVS.State.AWAITING == this.state) {
            result =  "<g:message code='pendingLbl'/>";
        } else if(EventVS.State.TERMINATED == this.state) {
            result =  "<g:message code='closedLbl'/>";
        } else if(EventVS.State.CANCELLED == this.state) {
            result =  "<g:message code='cancelledLbl'/>";
        }
        return result;
    }

    EventVS.prototype.getElement = function() {
        var $newEvent = $(this.eventHTML)
        var $li = $newEvent.find("li");

        if(EventVS.State.AWAITING == this.state) $li.addClass("eventVSAwaiting");
        if(EventVS.State.ACTIVE == this.state) $li.addClass("eventVSActive");
        if(EventVS.State.TERMINATED == this.state) $li.addClass("eventVSFinished");
        if(EventVS.State.CANCELLED == this.state) {
            $li.addClass("eventVSFinished");
            $li.find(".cancelMessage").fadeIn(100)
        }

        $li.attr('data-href', this.getURL())
        return $newEvent.html();
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