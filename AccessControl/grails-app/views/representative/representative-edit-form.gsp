<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/representative/representative-editor']"/>">

<polymer-element name="representative-edit-form">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
        </style>
        <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>

        <div style="display:{{step == 'requestrepresentativeNIF' && !isLoading?'block':'none'}}">
            <div class="pageHeader"  layout horizontal center center-justified>
                <h3><g:message code="editRepresentativeLbl"/></h3>
            </div>
            <div layout vertical center center-justified style="margin:0px auto 0px auto; width: 500px;">
                <label style="margin:0px 0px 20px 0px"><g:message code="nifForEditRepresentativeLbl"/></label>
                <input type="text" id="representativeNif" style="width:350px; margin:0px auto 0px auto;" class="form-control"/>
                <div>
                    <div layout horizontal style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                        <div flex></div>
                        <votingsystem-button on-click="{{checkRepresentativeNIF}}" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check" style="margin:0 7px 0 3px;"></i> <g:message code="acceptLbl"/>
                        </votingsystem-button>
                    </div>
                </div>
            </div>
            <div style="display:{{(step == 'requestrepresentativeNIF' && isLoading)?'block':'none'}}">
                <p style='text-align: center;'><g:message code="checkingDataLbl"/></p>
                <progress style='display:block;margin:0px auto 10px auto;'></progress>
            </div>
        </div>

        <div style="visibility:{{step == 'editData'?'visible':'hidden'}}">
            <representative-editor id="representativeEditor"></representative-editor>
        </div>

    </div>
    </template>
    <script>
        Polymer('representative-edit-form', {
            selectedImagePath:null,
            step:'requestrepresentativeNIF',
            isLoading:false,
            responseData:null,
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.representativeNif.onkeypress = function(event){
                    if (event.keyCode == 13) this.checkRepresentativeNIF()
                }.bind(this)
            },
            checkRepresentativeNIF: function() {
                console.log(this.tagName + " - ready")
                var validatedNif = validateNIF(this.$.representativeNif.value)
                if(validatedNif == null) showMessageVS('<g:message code="nifERRORMsg"/>','<g:message code="errorLbl"/>')
                else {
                    if(this.url != "${createLink(controller:'representative')}/edit/" + validatedNif) this.isLoading = true
                    this.url = "${createLink(controller:'representative')}/edit/" + validatedNif
                    console.log("url: " + this.url)
                }
            },
            responseDataChanged:function() {
                this.isLoading = false
                this.url = ""
                this.step = 'editData'
                this.$.representativeEditor.pageHeader = "<g:message code="editingRepresentativeMsgTitle"/>".format(this.responseData.fullName)
                this.$.representativeEditor.editorData = this.responseData.info
            }
        });
    </script>

</polymer-element>