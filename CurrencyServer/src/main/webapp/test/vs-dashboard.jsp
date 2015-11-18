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
                font-size: 8px;
            }
            rect {
                fill: #ddd;
            }
            .axis path,
            .axis line {
                fill: none;
                stroke: black;
                shape-rendering: crispEdges;
            }
            .axis text {
                font-size: 8px;
            }
            #tooltip {
                position: absolute;
                display: block;
                height: auto;
                padding: 10px;
                background-color: white;
                -webkit-border-radius: 10px;
                -moz-border-radius: 10px;
                border-radius: 10px;
                -webkit-box-shadow: 4px 4px 10px rgba(0, 0, 0, 0.4);
                -moz-box-shadow: 4px 4px 10px rgba(0, 0, 0, 0.4);
                box-shadow: 4px 4px 10px rgba(0, 0, 0, 0.4);
                pointer-events: none;
            }
            #tooltip.hidden {
                display: none;
            }
            #tooltip p {
                margin: 0;
                font-size: 16px;
                line-height: 20px;
            }
        </style>
        <iron-ajax auto="" id="ajax" url="{{url}}" handle-as="json" last-response="{{dashBoardDto}}" method="get"
                   content-type="application/json"></iron-ajax>
        <div id="transactionChart"></div>
        <div id="tooltip" class="hidden">
            <p><strong>{{transactionType}}</strong></p>
            <p>{{amount}}</p>
            <p>{{date}}</p>
        </div>
    </template>
    <script>
        Polymer({
            is:'vs-dashboard',
            properties: {
                dashBoardDto: {type:Object, observer:'dashBoardDtoChanged'},
                url:{type:String, value: "/CurrencyServer/rest/transactionVS/from/20151116_0000/to/20151118_0000"},
                width:{type:Number, value: 500},
                height:{type:Number, value: 350},
                rMin:{type:Number, value: 4},
                rMax:{type:Number, value: 20},
                margin:{type:Object, value: {top: 30, right: 20, bottom: 30, left: 50}}
            },
            ready: function() {
                this.rScale = d3.scale.linear().range([this.rMin, this.rMax]);
                this.colorScale = d3.scale.category10();
            },
            render:function(data) {
                console.log(this.tagName + " - render")
                this.rScale.domain(d3.extent(data, function (d){ return d.amount }));
                var xScale = d3.time.scale()
                        .domain(d3.extent(data, function(d) { return d.dateCreated; }))
                        .range([0, this.width]);
                var yScale = d3.scale.linear()
                        .domain([0, d3.max(data, function(d) { return d.amount; })])
                        .range([this.height, 0]);

                var xAxis = d3.svg.axis().scale(xScale)
                        .orient("bottom").ticks(5);
                var yAxis = d3.svg.axis().scale(yScale)
                        .orient("left").ticks(5);
                var zoom = d3.behavior.zoom()
                        .x(xScale)
                        .y(yScale)
                        .scaleExtent([-10, 10])
                        .on("zoom", zoomed);

                var svg = d3.select("#transactionChart")
                        .append("svg")
                        .attr("width", this.width + this.margin.left + this.margin.right)
                        .attr("height", this.height + this.margin.top + this.margin.bottom)
                        .append("g")
                        .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")")
                        .call(zoom);

                svg.append("rect")
                        .attr("width", this.width)
                        .attr("height", this.height)
                        .attr("class", "plot");

                var clip = svg.append("clipPath")
                        .attr("id", "clip")
                        .append("rect")
                        .attr("x", 0)
                        .attr("y", 0)
                        .attr("width", this.width)
                        .attr("height", this.height);

                var chartBody = svg.append("g")
                        .attr("clip-path", "url(#clip)");

                var circles = chartBody.selectAll("circle.node").data(data);
                circles.enter().append("circle");
                var hostElement = this
                var HTMLfixedTip = d3.select("#tooltip");
                circles.attr("class", "transaction")
                        .attr("cx",      function (d){ return   xScale(new Date(d.dateCreated))}.bind(this))
                        .attr("cy",      function (d){ return   yScale(d.amount) }.bind(this))
                        .attr("r",       function (d){
                            return this.rScale(d.amount) }.bind(this))
                        .attr("style",    function (d){ return   this.circleStyle(d) }.bind(this))
                        .on("mouseover", function(d) {
                            var matrix = this.getScreenCTM().translate(+this.getAttribute("cx"), +this.getAttribute("cy"));
                            //a fixed-position tooltip http://codepen.io/recursiev/pen/zpJxs
                            HTMLfixedTip.style("left", (matrix.e) + "px").style("top", (matrix.f + 3) + "px");
                            hostElement.transactionType = d.type
                            hostElement.amount = d.amount + " " + d.currencyCode
                            hostElement.date = new Date(d.dateCreated).formatWithTime()
                            d3.select("#tooltip").classed("hidden", false);
                        })
                        .on("mouseout", function() {
                            d3.select("#tooltip").classed("hidden", true);
                        })
                circles.exit().remove();
                svg.append("g")			// Add the X Axis
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + this.height + ")")
                        .call(xAxis);
                svg.append("g")			// Add the Y Axis
                        .attr("class", "y axis")
                        .call(yAxis);
                svg.append("text")
                        .attr("transform", "rotate(-90)")
                        .attr("y", 0 - this.margin.left)
                        .attr("x",(0 - (this.height / 2)))
                        .attr("dy", "1em")
                        .style("text-anchor", "middle")
                        .text("${msg.amountLbl}");
                svg.append("text")
                        .attr("x", (this.width / 2))
                        .attr("y", 0 - (this.margin.top / 2))
                        .attr("text-anchor", "middle")
                        .style("font-size", "16px")
                        .style("text-decoration", "underline")
                        .text("${msg.transactionsLbl}");
                //this is to apply polymer <style>
                Polymer.dom(this.$.transactionChart).appendChild(Polymer.dom(this.$.transactionChart).childNodes[0])
                function zoomed() {
                    svg.select(".x.axis").call(xAxis);
                    svg.select(".y.axis").call(yAxis);
                    circles.attr("cx",      function (d){ return   xScale(new Date(d.dateCreated))}.bind(this))
                            .attr("cy",      function (d){ return   yScale(d.amount) }.bind(this))
                }
            },
            circleStyle:function(d) {
                return "fill:" + this.colorScale(d.type) + "; stroke:" + d3.rgb(this.colorScale(d.type)).darker(1)
            },
            zoomed:function () {
                console.log(this.tagName + "- zoomed")
                if(!this.svg) return
                this.svg.select(".x.axis").call(this.xAxis);
                this.svg.select(".y.axis").call(this.yAxis);
            },
            reset:function () {
                /*d3.transition().duration(750).tween("zoom", function() {
                    var ix = d3.interpolate(this.xScale.domain(), [-this.width / 2, this.width / 2]),
                            iy = d3.interpolate(this.yScale.domain(), [-this.height / 2, this.height / 2]);
                    return function(t) {
                        zoom.x(this.xScale.domain(ix(t))).y(this.yScale.domain(iy(t)));
                        this.zoomed();
                    };
                });*/
            },
            dashBoardDtoChanged:function() {
                this.render(this.dashBoardDto.resultList)
            }
        });
    </script>
</dom-module>