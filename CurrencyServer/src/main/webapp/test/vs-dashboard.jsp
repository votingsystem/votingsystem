<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/iron-ajax/iron-ajax.html" rel="import"/>

<dom-module name="vs-dashboard">
    <template>
        <style>
            .transaction {
                stroke-width: 2;
                opacity: 0.8;
            }
            .transaction:hover {
                cursor: pointer;
                opacity: 1.0;
            }
        </style>
        <iron-ajax auto="" id="ajax" url="{{url}}" handle-as="json" last-response="{{dashBoardDto}}" method="get"
                   content-type="application/json"></iron-ajax>
        <div id="transactionChart"></div>
    </template>
    <script>
        Polymer({
            is:'vs-dashboard',
            properties: {
                dashBoardDto: {type:Object, observer:'dashBoardDtoChanged'},
                url:{type:String, value: "/CurrencyServer/rest/transactionVS"},
                outerWidth:{type:Number, value: 500},
                outerHeight:{type:Number, value: 350}
            },
            ready: function() {
                this.innerWidth  = this.outerWidth  - 50 - 50;
                this.innerHeight = this.outerHeight - 50 - 30;

                this.rMin = 5; // "r" stands for radius
                this.rMax = 20;

                this.svg = d3.select("#transactionChart").append("svg")
                        .attr("width", this.outerWidth)
                        .attr("height", this.outerHeight);
                this.xScale = d3.scale.linear().range([50, this.innerWidth]);
                this.yScale = d3.scale.linear().range([this.innerHeight, 50]);
                this.rScale = d3.scale.linear().range([this.rMin, this.rMax]);
                this.colorScale = d3.scale.category10();

            },
            render:function(data) {
                this.xScale.domain(d3.extent(data, function (d){
                    return new Date(d.dateCreated).getMinutes() }));
                this.yScale.domain(d3.extent(data, function (d){ return d.amount }));
                this.rScale.domain(d3.extent(data, function (d){ return d.amount }));

                var circles = this.svg.selectAll("circle").data(data);
                circles.enter().append("circle");
                circles.attr("class", "transaction")
                        .attr("cx",      function (d){ return   this.xScale(new Date(d.dateCreated).getMinutes())}.bind(this))
                        .attr("cy",      function (d){ return   this.yScale(d.amount) }.bind(this))
                        .attr("r",       function (d){ return   this.rScale(d.amount) }.bind(this))
                        .attr("style",    function (d){ return   this.circleStyle(d) }.bind(this))
                circles.exit().remove();
                //this is to force the load of inner styles
                Polymer.dom(this.$.transactionChart).appendChild(Polymer.dom(this.$.transactionChart).childNodes[0])
            },
            circleStyle:function(d) {
                return "fill:" + this.colorScale(d.type) + "; stroke:" + d3.rgb(this.colorScale(d.type)).darker(1)
            },
            dashBoardDtoChanged:function() {
                this.render(this.dashBoardDto.resultList)
            }
        });
    </script>
</dom-module>