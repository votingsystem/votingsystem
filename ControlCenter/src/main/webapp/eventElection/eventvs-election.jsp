<%@ page contentType="text/html; charset=UTF-8" %>


<link href="eventvs-election-stats.vsp" rel="import"/>

<dom-module name="eventvs-election">
    <template>
        <style>
            :host {
                display: none;
            }
            #eventContentDiv {
                border: #f2f2f2 solid 1px;
                -moz-border-radius: 3px; border-radius: 3px;
                margin:0px 0px 0px 0px;
                min-height:100px;
                padding: 10px 10px 10px 10px;
                word-wrap:break-word;
                overflow: hidden;
            }
            #eventAuthorDiv {
                color: #52515e;
                font-style: italic;
            }
        </style>
        <div class="pagevs">
            <div class="layout horizontal center center-justified" style="width:100%;text-align: center;">
                <div id="pageTitle" data-eventvs-id$="{{eventvs.id}}" class="pageHeader">{{eventvs.subject}}</div>
            </div>
            <div style="color: #888;font-size: 1.1em; text-align: center">{{getDate(eventvs.dateBegin)}}</div>
            <div style="width: 100%;">
                <div id="eventContentDiv" style="border: 1px solid #ccc;padding: 0 7px;"></div>

                <div class="horizontal layout center center-justified" style="margin: 5px 0 0 0;">
                    <div style="font-size: 1.1em;margin: 0 0 0 15px;">
                        <a href="{{eventvs.url}}" target="_blank">${msg.accessControlLbl}</a>
                    </div>
                    <div id="eventAuthorDiv" class="flex" style="margin:0px 20px 0 30px; color:#888; font-size: 0.85em;text-align: right;">
                        {{eventvs.user}}
                    </div>
                </div>

                <div style="width: 600px;margin: 20px auto;">
                    <eventvs-election-stats id="eventStats"></eventvs-election-stats>
                </div>
            </div>
        </div>
        <eventvs-admin-dialog id="eventVSAdminDialog" eventvs="{{eventvs}}"></eventvs-admin-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-election',
            properties: {
                url:{type:String, observer:'getHTTP'},
                eventvs:{type:Object, observer:'eventvsChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            eventvsChanged:function() {
                console.log("eventvsChanged - eventvs: " + this.eventvs.state)
                this.$.eventStats.eventvs = this.eventvs
                this.isTerminated = ('TERMINATED' === this.eventvs.state)? true : false
                this.isCanceled = ('CANCELED' === this.eventvs.state)? true : false
                this.adminMenuHidden = true
                if(this.eventvs.state === 'ACTIVE' || this.eventvs.state === 'PENDING') this.adminMenuHidden = false
                this.$.eventContentDiv.innerHTML = decodeURIComponent(escape(window.atob(this.eventvs.content)))
                this.style.display = 'block'
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.show()
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                vs.getHTTPJSON(targetURL, function(responseText){
                    this.eventvs = toJSON(responseText)
                }.bind(this));
            }
        });
    </script>
</dom-module>