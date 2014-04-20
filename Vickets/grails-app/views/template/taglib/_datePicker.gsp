<div id="dateSupportedDiv${attrs.id}" class="" style='width: 145px; display: inline;'>
    <input type="date" id='${attrs.id}Native' class="form-control" style='width: 160px; display: inline;'>
    <i id='${attrs.id}NativeIcon' class="fa fa-calendar form-control-feedback" style="color:#870000; margin: 0 0 0 -20px;"></i>
</div>

<div id="dateNotSupportedDiv${attrs.id}" class="form-inline" role="form" style="width: 145px; display: inline;">
    <input class="datePickerVS form-control" type="text" id='${attrs.id}' readonly
           style='width: 150px; display: inline;'
           title='${attrs.title}'
           placeholder='${attrs.placeholder}'
           oninvalid='${attrs.oninvalid}'
           onchange='${attrs.onchange}'/>
    <i class="fa fa-calendar form-control-feedback" style="color:#870000; margin: 0 0 0 -20px;"></i>
    <span>&nbsp;</span>
</div>

<r:script>

    $("#${attrs.id}NativeIcon").click(function(e) {
        console.log("=============")
        $("#${attrs.id}Native").click()
    });


    var ${attrs.id}Required = true
    if ("" == '${attrs.required}'.trim()) ${attrs.id}Required = false;

    var inputDateSupported${attrs.id} = checkInputType("date")


    if(inputDateSupported${attrs.id}) {
        $("#dateNotSupportedDiv${attrs.id}").css("display", "none")
        if( ${attrs.id}Required) {
            $("#${attrs.id}Native").attr("required", true)
        }
    } else {
        $("#dateSupportedDiv${attrs.id}").css("display", "none")
        //dd/MM/yy 00:00:00"
        $("#${attrs.id}").datepicker({dateFormat: 'dd/MM/yy'});
        if( ${attrs.id}Required) {
            $("#${attrs.id}").attr("required", true)
        }
    }

    document.getElementById("${attrs.id}").getDate = function() {
            var result = null
        if(inputDateSupported${attrs.id}) {
            if("" != $("#${attrs.id}Native").val().trim()) result = $("#${attrs.id}Native").val()
        } else {
            if ($("#${attrs.id}").datepicker("getDate") != null) result = $("#${attrs.id}").datepicker("getDate").format()
            if(result == null) $("#${attrs.id}").val("")
            document.getElementById("${attrs.id}").setCustomValidity("DummyInvalid");
        }
        return result
    }

</r:script>




