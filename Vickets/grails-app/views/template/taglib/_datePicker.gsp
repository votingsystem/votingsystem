<div id="dateSupportedDiv${attrs.id}" class="" style='width: 145px; display: inline;'>
    <input type="date" id='${attrs.id}Native' class="form-control has-error" style='width: 145px; display: inline; padding-right: 0px;'>
</div>

<div id="dateNotSupportedDiv${attrs.id}" class="form-inline" role="form" style="width: 145px; display: inline;">
    <input class="datePickerVS form-control" type="text" id='${attrs.id}' readonly
           style='width: 145px; display: inline; cursor: pointer;'
           title='${attrs.title}'
           placeholder='${attrs.placeholder}'
           oninvalid='${attrs.oninvalid}'
           onchange='${attrs.onchange}'/>
    <i class="fa fa-calendar form-control-feedback" style="color:#6c0404; margin: 0 0 0 -20px;"></i>
    <span>&nbsp;</span>
</div>

<r:script>

    var ${attrs.id}Required = true
    if("false" == '${attrs.required}'.toLowerCase()) ${attrs.id}Required = false;

    var inputDateSupported${attrs.id} = checkInputType("date")

    if(inputDateSupported${attrs.id}) {
        $("#dateNotSupportedDiv${attrs.id}").css("display", "none")
        if( ${attrs.id}Required) {
            $("#${attrs.id}Native").attr("required", true)
        }
    } else {
        $("#dateSupportedDiv${attrs.id}").css("display", "none")
        //yy/dd/MM 00:00:00"
        $("#${attrs.id}").datepicker({dateFormat: 'yy/dd/MM'});
        if( ${attrs.id}Required) {
            $("#${attrs.id}").attr("required", true)
        }
    }

    //return date with format format "yyyy/mm/dd HH:mm:ss"
    document.getElementById("${attrs.id}").getDate = function() {
        var result = null
        if(inputDateSupported${attrs.id}) {
            if("" != $("#${attrs.id}Native").val().trim()) result = $("#${attrs.id}Native").val();
            result = DateUtils.parseInputType(result)
        } else {
            if ($("#${attrs.id}").datepicker("getDate") != null) result = $("#${attrs.id}").datepicker("getDate")
            if(result == null) $("#${attrs.id}").val("")
            document.getElementById("${attrs.id}").setCustomValidity("DummyInvalid");
        }
        return result
    }

    document.getElementById("${attrs.id}").getValidatedDate = function() {
        var result = document.getElementById("${attrs.id}").getDate()
        if(result == null) {
            $("#dateSupportedDiv${attrs.id}").addClass( "has-error" );
            $("#dateNotSupportedDiv${attrs.id}").addClass( "has-error" );
        } else {
            $("#dateSupportedDiv${attrs.id}").removeClass( "has-error" );
            $("#dateNotSupportedDiv${attrs.id}").removeClass( "has-error" );
        }
        return result
    }

    document.getElementById("${attrs.id}").reset = function() {
        $('#${attrs.id}').val( "" );
        $('#dateSupportedDiv${attrs.id}').removeClass( "has-error" );
        $("#${attrs.id}Native").val("")
        $('#dateNotSupportedDiv${attrs.id}').removeClass( "has-error" );
    }

</r:script>




