    <div id="timeSupportedDiv${attrs.id}" class="form-inline" role="form" style='display: inline; ${attrs.style}'>
        <div class="form-group" style="">
            <input id="${attrs.id}" class="form-control" type="time" style="width: 95px;" value="00:00">
            <span style="color: #870000;margin: 0 0 0 0px;" ><g:message code="hourLbl"/>
                <i class="fa fa-clock-o"></i></span>
        </div>
    </div>

    <div id="timeNotSupportedDiv${attrs.id}" class="form-inline" role="form" style="display: inline; ${attrs.style}">
        <div class="form-group">
            <input type="text" id="${attrs.id}HourInput" class="form-control" style="width: 45px;" pattern="\d{1,2}"
                   oninvalid="this.setCustomValidity('<g:message code="timePickerHourValidationMsg"/>')" maxlength="2" value="00">
            <span style="color: #870000; font-weight: bold;">:</span>

            <input type="text" id="${attrs.id}MinuteInput" class="form-control" style="width: 45px;" pattern="\d{1,2}"
                   oninvalid="this.setCustomValidity('<g:message code="timePickerMinuteValidationMsg"/>')" maxlength="2" value="00">
            <span style="color: #870000;"><g:message code="hourLbl"/>
                <i class="fa fa-clock-o"></i></span>
        </div>
    </div>

<r:script>

    var ${attrs.id}Required = true
    if ("" == '${attrs.required}'.trim())  ${attrs.id}Required = false;

    document.getElementById('${attrs.id}HourInput').addEventListener('change', hourInputValidator${attrs.id}, false);
    document.getElementById('${attrs.id}MinuteInput').addEventListener('change', minuteValidator${attrs.id}, false);

    function hourInputValidator${attrs.id}(event){
        var numHours = $("#${attrs.id}HourInput").val()
        if(numHours > 23) {
            $("#${attrs.id}HourInput").val(23)
        }
        if (isNaN(Number(event.target.value))) {
            event.target.setCustomValidity("DummyInvalid");
        } else event.target.setCustomValidity("");
    }

    function minuteValidator${attrs.id}(event){
        var numMinutes = $("#${attrs.id}MinuteInput").val()
        if(numMinutes > 59) {
            $("#${attrs.id}MinuteInput").val(59)
        }
        if (isNaN(Number(event.target.value))) {
            event.target.setCustomValidity("DummyInvalid");
        } else event.target.setCustomValidity("");
    }

    var inputTimeSupported${attrs.id} = checkInputType("time")

    if(inputTimeSupported${attrs.id}) {
        $("#timeNotSupportedDiv${attrs.id}").css("display", "none")
        if( ${attrs.id}Required) {
            $("#${attrs.id}").attr("required", true)
        }
    } else {
        $("#timeSupportedDiv${attrs.id}").css("display", "none")
        if( ${attrs.id}Required) {
            $("#${attrs.id}HourInput").attr("required", true)
            $("#${attrs.id}MinuteInput").attr("required", true)
        }
    }

    document.getElementById("${attrs.id}").getTime = function() {
        var result = null
        if(inputTimeSupported${attrs.id}) {
            if("" != $("#${attrs.id}").val().trim()) result = $("#${attrs.id}").val() + ":00"
        } else {
            if("" != $("#${attrs.id}HourInput").val().trim() && "" != $("#${attrs.id}MinuteInput").val().trim()) {
                result = $("#${attrs.id}HourInput").val() + ":" + $("#${attrs.id}MinuteInput").val() + ":00"
            }
        }
        return result
    }

    document.getElementById("${attrs.id}").getValidatedTime = function() {
        var result = document.getElementById("${attrs.id}").getTime()
        if(result == null) {
            $("#timeSupportedDiv${attrs.id}").addClass("has-error");
            $("#timeNotSupportedDiv${attrs.id}").addClass("has-error");
        } else {
            $("timeSupportedDiv${attrs.id}").removeClass("has-error");
            $("#timeNotSupportedDiv${attrs.id}").removeClass("has-error");
        }
        return result
    }

    document.getElementById("${attrs.id}").reset = function() {
        $("#${attrs.id}").val("00:00")
        $("#${attrs.id}HourInput").val("00")
        $("#${attrs.id}MinuteInput").val("00")
    }

</r:script>



