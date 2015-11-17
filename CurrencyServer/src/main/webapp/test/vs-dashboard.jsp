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
                font-family: sans-serif;
                font-size: 16px;
                line-height: 20px;
            }
        </style>
        <iron-ajax auto="" id="ajax" url="{{url}}" handle-as="json" last-response="{{dashBoardDto}}" method="get"
                   content-type="application/json"></iron-ajax>
        <div id="transactionChart"></div>
        <div id="tooltip" class="hidden">
            <p><strong>{{transactionType}}</strong></p>
            <p><span id="value">{{amount}}</span></p>
            <p>{{date}}</p>
        </div>
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
                document.onmousemove =  function (event) {
                    var dot, eventDoc, doc, body, pageX, pageY;

                    event = event || window.event; // IE-ism

                    // If pageX/Y aren't available and clientX/Y are,
                    // calculate pageX/Y - logic taken from jQuery.
                    // (This is to support old IE)
                    if (event.pageX == null && event.clientX != null) {
                        eventDoc = (event.target && event.target.ownerDocument) || document;
                        doc = eventDoc.documentElement;
                        body = eventDoc.body;

                        event.pageX = event.clientX +
                                (doc && doc.scrollLeft || body && body.scrollLeft || 0) -
                                (doc && doc.clientLeft || body && body.clientLeft || 0);
                        event.pageY = event.clientY +
                                (doc && doc.scrollTop  || body && body.scrollTop  || 0) -
                                (doc && doc.clientTop  || body && body.clientTop  || 0 );
                    }

                    // Use event.pageX / event.pageY here
                }
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
                        .attr("transform", "translate(0," + (this.width - this.padding) + ")")
                        .call(this.xAxis);

                //Create Y axis
                this.svg.append("g")
                        .attr("class", "y axis")
                        .attr("transform", "translate(" + this.padding + ",0)")
                        .call(this.yAxis);

                var circles = this.svg.selectAll("circle").data(data);
                circles.enter().append("circle");
                var parentElement = this
                //http://codepen.io/recursiev/pen/zpJxs
                var HTMLfixedTip = d3.select("#tooltip");

                circles.attr("class", "transaction")
                        .attr("cx",      function (d){ return   this.xScale(new Date(d.dateCreated).getMinutes())}.bind(this))
                        .attr("cy",      function (d){ return   this.yScale(d.amount) }.bind(this))
                        .attr("r",       function (d){ return   this.rScale(d.amount) }.bind(this))
                        .attr("style",    function (d){ return   this.circleStyle(d) }.bind(this))
                        .on("mouseover", function(d) {
                            var matrix = this.getScreenCTM()
                                    .translate(+this.getAttribute("cx"),
                                            +this.getAttribute("cy"));
                            //You can use screen coordinates directly to position
                            //a fixed-position tooltip
                            HTMLfixedTip.style("left", (matrix.e) + "px")
                                    .style("top", (matrix.f + 3) + "px");

                            parentElement.transactionType = d.type
                            parentElement.date = new Date(d.dateCreated)
                            d3.select("#tooltip").classed("hidden", false);

                        })
                        .on("mouseout", function() {
                            d3.select("#tooltip").classed("hidden", true);
                        })
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