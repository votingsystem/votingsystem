<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-table/vs-table.html" rel="import"/>

<dom-module name="test-table">
    <style>
        #vsTable::shadow table{
            color: #333;
            border-spacing: 0;
            border-collapse: collapse;
            font-size: 14px;
            border: 1px solid #bfbfbf;
            box-shadow: 0 0 4px rgba(0,0,0,0.10);
        }
        #vsTable::shadow th.sortable,
        #vsTable::shadow .rows td {
            white-space: nowrap;
            padding: 8px 5px;
            border: 1px solid #bfbfbf;
            text-align: center;
        }
        #vsTable::shadow tbody tr.rows:hover,
        #vsTable::shadow .rows.selected {
            background-color: #f3f3f3;
            color: #1B8CCD;
        }
        #vsTable::shadow th {
            border: 1px solid #bfbfbf;
            background: #efefef;
            color: #434343;
            font-weight: bold;
        }
        #vsTable::shadow .search-head input {
            width: 40px;
            min-width: 90%;
        }
        #vsTable::shadow .paging {
            display: block;
            float: left;
        }
        #vsTable::shadow .pager input {
            border: none;
            outline: none;
            background: #fafafa;
        }
        #vsTable::shadow .pagesize {
            display: none;
            float: left;
        }
        #vsTable::shadow .summary {
            display: block;
            float: right;
        }
        #vsTable::shadow tfoot td {
            border: 1px solid #bfbfbf   ;
            background-color: #fafafa;
        }
        #vsTable::shadow .vs-${msg.subjectLbl}-th{
            width:300px;
        }
        #vsTable::shadow .vs-${msg.dateLbl}-th{
            width: 95px;
        }
        #vsTable::shadow .vs-${msg.tagLbl}-th{
            width: 110px;
        }
        #vsTable::shadow .vs-${msg.amountLbl}-th{
            width: 100px;
        }
        #vsTable::shadow .rows .vs-${msg.amountLbl}-td{
            text-align: right;
        }
    </style>
    <template>
        <div class="horizontal layout center center-justified" style="margin: 30px auto;">

            <vs-table id="vsTable"
                      searchable
                      pagesize="100"
                      pagetext="${msg.pageLbl}:"
                      pageoftext="${msg.ofLbl}"
                      itemoftext="${msg.ofLbl}">
                <vs-column name="${msg.dateLbl}"
                           type="date"
                           searchable
                           sortable
                           required></vs-column>

                <vs-column name="${msg.subjectLbl}"
                           type="text"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="${msg.tagLbl}"
                           type="string"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="TestNumber"
                           type="string"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

                <vs-column name="${msg.amountLbl}"
                           type="amount"
                           sortable
                           searchable
                           required
                           default=""></vs-column>

            </vs-table>
        </div>
        <button id="create" on-click="{{addRow}}">Create</button>
        <div id="console"></div>
    </template>
    <script>
        Polymer({
            is:'test-table',
            ready: function() {
                this.initTable();
            },
            busca: function(e, p) {
                console.log("")
            },
            initTable: function() {
                var events = [
                { ${msg.subjectLbl}: "gym tonu", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                    ${msg.amountLbl}: {amount:-123, currencyCode:'EUR'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "breakfast", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                    ${msg.amountLbl}: {amount:0, currencyCode:'EUR'}, ${msg.tagLbl}:'WILDTAG', TestNumber:0.00},
                { ${msg.subjectLbl}: "breakfast", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                    ${msg.amountLbl}: {amount:2, currencyCode:'EUR'}, ${msg.tagLbl}:'WILDTAG', TestNumber:2.00},
                { ${msg.subjectLbl}: "breakfast", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:23, currencyCode:'EUR'}, ${msg.tagLbl}:'WILDTAG', TestNumber:23.00},
                { ${msg.subjectLbl}: "work", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'EUR'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "lunch", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:123, currencyCode:'USD'}, TestNumber:123.00 },
                { ${msg.subjectLbl}: "work", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:0, currencyCode:'USD'} , TestNumber:0.00 },
                { ${msg.subjectLbl}: "lunch", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:0.1, currencyCode:'USD'}, TestNumber:0.1 },
                { ${msg.subjectLbl}: "work", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'CNY'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'CNY'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "work", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'CNY'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'CNY'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "meeting", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'JPY'}, TestNumber:-123.00 },
                { ${msg.subjectLbl}: "dinner", ${msg.dateLbl}: new Date().toISOString().slice(0, 10),
                        ${msg.amountLbl}: {amount:-123, currencyCode:'JPY'}, TestNumber:-123.00 }
                ];
                this.$.vsTable.data = JSON.parse(JSON.stringify(events));//deep copy so that they have independent data source.

                this.$.vsTable.addEventListener('after-td-click', function(e) {
                    console.log('after-td-click', e.detail);
                    //e.detail.row._editing=true;
                    //e.detail.event.target.focus();
                });


                this.$.vsTable.customRenderer = function(datum, row, col, ahaTable) {
                    console.log("customRenderer", datum)
                    return '<div style="color: red;">' + datum + '</div>';
                }

                this.$.vsTable.addEventListener('filter', function (e) {
                    console.log("NoFilteredRows:", e.detail);
                })
            },
            addRow: function() {
                this.$.vsTable.create();
            }
        });
    </script>
</dom-module>