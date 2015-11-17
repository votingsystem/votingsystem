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
            .axis {
                z-index: -20;
            }

            .axis path,
            .axis line {
                fill: none;
                stroke: black;
                shape-rendering: crispEdges;
            }

            .axis text {
                font-family: sans-serif;
                font-size: 11px;
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
                width:{type:Number, value: 500},
                height:{type:Number, value: 350},
                padding:{type:Number, value: 50}
            },
            ready: function() {
                this.rMin = 5; // "r" stands for radius
                this.rMax = 20;

                this.svg = d3.select("#transactionChart").append("svg")
                        .attr("width", this.width)
                        .attr("height", this.height);

                this.xScale = d3.scale.linear().range([this.padding, this.width - this.padding * 2]);
                this.yScale = d3.scale.linear().range([this.height - this.padding, this.padding]);
                this.rScale = d3.scale.linear().range([this.rMin, this.rMax]);
                this.colorScale = d3.scale.category10();
            },
            render:function(data) {
                this.xScale.domain(d3.extent(data, function (d){
                    return new Date(d.dateCreated).getMinutes() }));
                this.yScale.domain(d3.extent(data, function (d){ return d.amount }));
                this.rScale.domain(d3.extent(data, function (d){ return d.amount }));

                //Define X axis
                this.xAxis = d3.svg.axis()
                        .scale(this.xScale)
                        .orient("bottom")
                        .ticks(5);
                //Define Y axis
                this.yAxis = d3.svg.axis()
                        .scale(this.yScale)
                        .orient("left")
                        .ticks(5);
                //Create X axis
                this.svg.append("g")
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + (this.height - this.padding) + ")")
                        .call(this.xAxis);

                //Create Y axis
                this.svg.append("g")
                        .attr("class", "y axis")
                        .attr("transform", "translate(" + this.padding + ",0)")
                        .call(this.yAxis);

                var circles = this.svg.selectAll("circle").data(data);
                circles.enter().append("circle");
                circles.attr("class", "transaction")
                        .attr("cx",      function (d){ return   this.xScale(new Date(d.dateCreated).getMinutes())}.bind(this))
                        .attr("cy",      function (d){ return   this.yScale(d.amount) }.bind(this))
                        .attr("r",       function (d){ return   this.rScale(d.amount) }.bind(this))
                        .attr("style",    function (d){ return   this.circleStyle(d) }.bind(this))
                circles.exit().remove();

                this.svg.append("g")
                        .attr("class", "x axis")    // <-- Note x added here
                        .attr("transform", "translate(0," + (this.height - this.padding) + ")")
                        .call(this.xAxis);

                this.svg.append("g")
                        .attr("class", "y axis")    // <-- Note y added here
                        .attr("transform", "translate(" + this.padding + ",0)")
                        .call(this.yAxis);

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