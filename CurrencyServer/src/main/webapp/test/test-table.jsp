<%@ page contentType="text/html; charset=UTF-8" %>

<link href="./vs-table/vs-table.html" rel="import"/>

<dom-module name="test-table">
    <style>
        #vsTable::shadow table{
            color: #333;
            border-spacing: 0;
            border-collapse: collapse;
            font-size: 14px;
            border: 1px solid #cc;
            box-shadow: 0 0 4px rgba(0,0,0,0.10);
        }
        #vsTable::shadow .pagination table,
        #vsTable::shadow .pagination tr,
        #vsTable::shadow .pagination td{
            border: 0;
        }
        #vsTable::shadow th.sortable,
        #vsTable::shadow .rows td {
            white-space: nowrap;
            padding: 8px 5px;
            border: 1px solid #ccc;
        }
        #vsTable::shadow tbody tr.rows:hover,
        #vsTable::shadow .rows.selected {
            background-color: #f3f3f3;
            color: #1B8CCD;
        }
        #vsTable::shadow th.searchable input,
        #vsTable::shadow th.searchable select {
            width: 90%;
        }
        #vsTable::shadow th {
            border: 1px solid #ccc;
            background: #fff;
            color: #2e6e9e;
            font-weight: bold;
        }
        #vsTable::shadow sup {
            color:yellow;
        }
        #vsTable::shadow .search-head input {
            width: 40px;
            min-width: 90%;
        }
        #vsTable::shadow .paging {
            display: block;
            float: left;
        }
        #vsTable::shadow .pagesize {
            display: block;
            float: left;
        }
        #vsTable::shadow .summary {
            display: block;
            float: right;
        }
        #vsTable::shadow tfoot td {
            background-color: #eee;
        }
        #vsTable::shadow .search,
        #vsTable::shadow .nosearch {
            border: 0;
            display: inline-block;
            font-family: FontAwesome;
            font-style: normal;
            font-weight: normal;
            line-height: 1;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
            min-width: 15px;
            min-height: 20px;
        }
        #vsTable::shadow .search:before {
            content:"\f00e";
        }
        #vsTable::shadow .nosearch:before {
            content:"\f010";
        }
        #vsTable::shadow .aha-title-th{
         width: 300px;
        }
    </style>
    <template>

        <vs-table id="vsTable"
                  searchable


                   pagesize="100"
                   data-sizelist="[100,500,1000]"

                   pagesizetext="Page Size: "
                   summarytext="Viewing: "
                   pagetext="Page:"
                   pageoftext="of"
                   itemoftext="of"

                   searchtitle="Show/Hide Filters"
                   sorttitle="click to sort"
                   selecttitle="click to select"
                   selectalltitle="click to select/deselect all"
                   edittitle="click to edit"
                   copytitle="click to copy"
                   removetitle="click to remove"
                   movedowntitle="move down"
                   moveuptitle="move up"
                   searchtitle="click to show/hide search filters bar"
                   firsttitle="First"
                   previoustitle="Previous"
                   nexttitle="Next"
                   lasttitle="Last">
            <aha-column
                    name="title"
                        type="string"
                        sortable
                        searchable
                        required
                        searchplaceholder="${msg.searchLbl}"
                        default=""></aha-column>
            <aha-column name="date"
                        type="date"
                        searchable
                        sortable
                        required></aha-column>

            <aha-column name="type"
                        type="choice"
                        searchable
                        sortable
                        required
                        data-choices='{"":"", "private":"Private Event", "public":"Public Event"}'></aha-column>

            <aha-column name="content"
                        type="text"
                        searchable
                        required
                        hint="Keep it short"
                        placeholder="Event Content"
                        default=""></aha-column>

            <aha-column name="enabled"
                        type="boolean"

                        searchable
                        editable
                        sortable
                        required

                        data-choices='{"1":"Yes", "0":"No"}'
                        placeholder="Yes/No"
                        hint="Only enabled events will get alert"
                        default=""></aha-column>
            <aha-column name="html"
                        type="html"
                        required
                        placeholder=""
                        default=""></aha-column>
        </vs-table>
        <button id="create" on-click="{{addRow}}">Create</button>
        <div id="console"></div>
    </template>
    <script>
        Polymer({
            is:'test-table',
            ready: function() {
                var events = [
                    { title: "gym", date: new Date().toISOString().slice(0, 10), type: "private", content: "5*20 situps, 5 miles running, 5 minutes PLANKS", enabled: true, html: "<a href='http://www.google.com'>Search</a>" },
                    { title: "breakfast", date: new Date().toISOString().slice(0, 10), type: "private", enabled: true },
                    { title: "work", date: new Date().toISOString().slice(0, 10), type: "public", content: "implement vs-table plymer element", enabled: true },
                    { title: "lunch", date: new Date().toISOString().slice(0, 10), type: "public", content: null, enabled: false },
                    { title: "work", date: new Date().toISOString().slice(0, 10), type: "public", content: "implement vs-table plymer element" },
                    { title: "lunch", date: new Date().toISOString().slice(0, 10), type: "public", content: null },
                    { title: "work", date: new Date().toISOString().slice(0, 10), type: "public", content: "implement vs-table plymer element", enabled: true },
                    { title: "", date: new Date().toISOString().slice(0, 10), type: "public", content: null },
                    { title: "work", date: new Date().toISOString().slice(0, 10), type: "public", content: "implement vs-table plymer element", enabled: true },
                    { title: "", date: new Date().toISOString().slice(0, 10), type: "public", content: null },
                    { title: "meeting", date: new Date().toISOString().slice(0, 10), type: "public", content: "introduce web components to the team" },
                    { title: "dinner", date: new Date().toISOString().slice(0, 10), type: "private" }
                ];
                this.$.vsTable.data = JSON.parse(JSON.stringify(events));//deep copy so that they have independent data source.
                var table_log = function(msg) {
                    document.getElementById("console").innerHTML = msg;
                };
                /*this.$.vsTable.addEventListener('after-invalid', function(e) {
                    table_log('This field is required');
                    e.detail.row._editing=true;
                    e.detail.event.target.focus();
                });
                this.$.vsTable.addEventListener('after-create', function(e) {
                    table_log('created one record');
                });
                this.$.vsTable.addEventListener('after-remove', function(e) {
                    table_log('removed one record!');
                });
                this.$.vsTable.addEventListener('after-copy', function(e) {
                    table_log('copied one record!');
                });
                this.$.vsTable.addEventListener('after-save', function(e) {
                    table_log('saved one record!');
                });
                this.$.vsTable.addEventListener('after-move-down', function(e) {
                    table_log('moved one record down!');
                });
                this.$.vsTable.addEventListener('after-move-up', function(e) {
                    table_log('moved one record up!');
                });
                this.$.vsTable.customRenderer = function(datum, row, col, ahaTable) {
                    return datum.toUpperCase();
                }*/

            },
            addRow: function() {
                this.$.vsTable.create();
            }
        });
    </script>
</dom-module>
