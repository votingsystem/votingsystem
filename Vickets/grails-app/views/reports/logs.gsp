<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal title="<g:message code="reportsPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <polymer-element name="record-list" attributes="url menuType">
        <template>
            <style></style>
            <core-ajax id="ajax" auto url="{{url}}" response="{{responseData}}" handleAs="json" method="get" contentType="json"></core-ajax>
            <div layout vertical center>
                <div id="record_tableDiv" style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
                    <table class="table tableHeadervs" id="record_table" style="">
                        <thead>
                        <tr style="color: #ff0000;">
                            <th style="width: 220px;"><g:message code="dateLbl"/></th>
                            <th style="max-width:80px;"><g:message code="messageLbl"/></th>
                        </tr>
                        </thead>
                        <tbody>
                            <template repeat="{{record in responseData.records}}">
                                <tr><td class="text-center">{{record.date}}</td><td class="text-center">{{record.message}}</td></tr>
                            </template>
                        </tbody>
                    </table>
                </div>
            </div>
        </template>
        <script> Polymer('record-list', {});</script>
    </polymer-element>

    <record-list id="recordList" url= "${createLink(controller: 'reports', action: 'index')}"></record-list>

</div>
</body>
</html>
<asset:script>
</asset:script>