<%@ page contentType="text/html; charset=UTF-8" %>

<script type="text/javascript" src="TransactionStats.js"></script>

<dom-module id="scatter-transactions">
    <link rel="import" type="css" href="transactions-scatter.css">
    <style>
        :host {
            display: block;
            width:100%;
            height: 100%;
            position: relative;
        }
        .tooltip {
            position: absolute;
            text-align: center;
            font-size: 0.8em;
            color: #fff;
            padding: 2px;
            background: #888;
            border: 0px;
            border-radius: 8px;
        }
    </style>
    <template>
        <button id="resetButton" style="position: absolute; left: 50px; top: 0px;">reset</button>
        <div id="scatterPlotDiv" style="width: 100%; height: 100%; min-height: 200px;"></div>
        <div id="tooltip" class="tooltip" style="">
            <div><strong>{{transactionType}}</strong></div>
            <div><a href="{{receiptURL}}">{{amount}}</a></div>
            <div>{{date}}</div>
        </div>
    </template>
    <script>
        (function() {
            Polymer({
                is: "scatter-transactions",
                properties: {
                    legend:{type:Object, value:{height:30, width:500, margin: {top: 15, right: 0, bottom: 0, left: 300}}},
                    margin:{type:Object, value:{top: 40, right: 10, bottom: 100, left: 50}},
                    chartData:{type:Object, value:{top: 40, right: 10, bottom: 100, left: 50}}
                },
                ready: function() {
                    console.log(this.tagName + " ready")
                    this.transactionsStats = new TransactionsStats()
                    this.legendDispatch = d3.dispatch('legendClick', 'legendMouseover', 'legendMouseout');
                    var rMin = 4, rMax = 20;
                    this.color = d3.scale.category10(),
                            rScale = d3.scale.linear().range([rMin, rMax])
                    window.addEventListener('resize', function(event){
                        this.chart(this.chartData)
                    }.bind(this));
                    d3.json("/CurrencyServer/rest/transactionVS/from/20151116_0000/to/20151118_0000", function (json) {
                        this.chartData = json.resultList
                        this.chartData.forEach(function(transactionvs) {
                            this.transactionsStats.pushTransaction(transactionvs)
                        }.bind(this))
                        this.chart(this.chartData)
                    }.bind(this))
                },
                chart:function (data) {
                    while (this.$.scatterPlotDiv.hasChildNodes()) {
                        this.$.scatterPlotDiv.removeChild(this.$.scatterPlotDiv.lastChild);
                    }
                    this.width = this.$.scatterPlotDiv.offsetWidth;
                    this.height = this.$.scatterPlotDiv.offsetHeight
                    alert(this.width + ", " + this.height)
                    var svg = d3.select("#scatterPlotDiv").append("svg")
                            .attr('width', this.width)
                            .attr('height', this.height).datum(data).append("g")
                            .attr("transform", "translate(" + this.margin.left + " ," + this.margin.top + ")")
                            .on("click", function() {
                                this.$.tooltip.style.display = 'none'
                            }.bind(this));

                    this.height = this.height - this.margin.top - this.margin.bottom;
                    var x = d3.time.scale(),
                            y = d3.scale.linear(),
                            xAxis = d3.svg.axis().scale(x).orient('bottom').ticks(5),
                            yAxis = d3.svg.axis().scale(y).orient('left').ticks(5)
                    this.width = this.width - this.margin.left - this.margin.right
                    console.log("TransactionScatterPlot.js - printing chart")
                    rScale.domain(d3.extent(data, function (d){ return d.amount }));
                    x = x.domain(d3.extent(data, function(d) { return d.dateCreated; })).range([0, this.width]).nice();
                    y = y.domain([0, d3.max(data, function(d) { return d.amount; })]).range([this.height, 0]).nice();
                    var zoom = d3.behavior.zoom().x(x).y(y).scaleExtent([-10, 10]).on("zoom", zoomed)
                    var scatterWrap = svg.append("rect")
                            .attr("width", this.width)
                            .attr("height", this.height)
                            .attr("class", "scatterRect").call(zoom);

                    var clip = svg.append("clipPath")
                            .attr("id", "clip")
                            .append("rect")
                            .attr("x", 0)
                            .attr("y", 0)
                            .attr("width", this.width)
                            .attr("height", this.height)
                    var chartBody = svg.append("g").attr("clip-path", "url(#clip)");

                    resetButton.onclick= function(){
                        zoom.translate([0, 0]).scale(1)
                        zoomed()
                    }
                    var circles = chartBody.selectAll(".transaction").data(data);
                    var hostElement = this
                    circles.enter().append("circle");
                    circles.attr("class", "transaction")
                            .attr("cx",      function (d){ return   x(new Date(d.dateCreated))})
                            .attr("cy",      function (d){ return   y(d.amount) })
                            .attr("r",       function (d){ return rScale(d.amount) })
                            .attr("style",    function (d){ return   "fill:" +
                                    this.color(d.type) + "; stroke:" + d3.rgb(this.color(d.type)).darker(1)}.bind(this))
                            .on("mouseover", function(d) {
                                hostElement.$.tooltip.style.display = 'block'
                                hostElement.$.tooltip.style.top = (d3.event.pageY ) + "px"
                                hostElement.$.tooltip.style.left = (d3.event.pageX - 30) + "px"
                                hostElement.transactionType = d.amount
                                hostElement.receiptURL = d.id
                                hostElement.amount = d.amount + " " + d.currencyCode
                                hostElement.date = new Date(d.dateCreated).formatWithTime()

                                var circle = d3.select(this);
                                circle.transition()
                                        .duration(800).style("opacity", 1)
                                        .attr("r", rScale(d.amount) + 3).ease("elastic");
                                svg.append("g")
                                        .attr("class", "guide")
                                        .append("line")
                                        .attr("x1", circle.attr("cx"))
                                        .attr("x2", circle.attr("cx"))
                                        .attr("y1", circle.attr("cy"))
                                        .attr("y2", hostElement.height)
                                        .style("stroke", circle.style("fill"))
                                        .transition().delay(200).duration(400).styleTween("opacity", function() { return d3.interpolate(0, .5); })
                                svg.append("g")
                                        .attr("class", "guide")
                                        .append("line")
                                        .attr("x1", circle.attr("cx"))
                                        .attr("x2", 0)
                                        .attr("y1", circle.attr("cy"))
                                        .attr("y2", circle.attr("cy"))
                                        .style("stroke", circle.style("fill"))
                                        .transition().delay(200).duration(400).styleTween("opacity",
                                        function() { return d3.interpolate(0, .5); })
                            })
                            .on("mouseout", function(d) {
                                var circle = d3.select(this);
                                circle.transition().duration(800).style("opacity", .5).attr("r", rScale(d.amount)).ease("elastic");
                                d3.selectAll(".guide").transition().duration(100).styleTween("opacity", function() { return d3.interpolate(.5, 0); }).remove()
                            })

                    circles.exit().remove();
                    this.legendChart()
                    svg.append("g")
                            .attr("class", "x axis")
                            .attr("transform", "translate(0," + (this.height) + ")")
                            .call(xAxis)
                            .append("text")
                            .attr("class", "label")
                            .attr("x", this.width)
                            .attr("y", -6)
                            .style("text-anchor", "end");
                    svg.append("g")
                            .attr("class", "y axis")
                            .call(yAxis)
                            .append("text")
                            .attr("class", "label")
                            .attr("transform", "rotate(-90)")
                            .attr("y", 6)
                            .attr("x", 0)
                            .attr("dy", ".71em")
                            .style("text-anchor", "end")
                            .style("size", "end")
                            .text("${msg.amountLbl}");

                    var transactionFilter = []
                    this.legendDispatch.on('legendClick', function(d, i) {
                        var index
                        if((index = transactionFilter.indexOf(d.type)) > -1) {
                            transactionFilter.splice(index, 1);
                        } else {
                            transactionFilter.push(d.type)
                        }
                        chartBody.selectAll(".transaction").attr("display", function(d) {
                            if(transactionFilter.indexOf(d.type) > -1) return 'none'
                            else return 'inline'
                        });
                    });
                    function zoomed() {
                        //console.log("zoom.scale: " + zoom.scale() + " - x.domain: " + x.domain()[0])
                        svg.select(".x.axis").call(xAxis);
                        svg.select(".y.axis").call(yAxis);
                        circles.attr("cx", function (d){ return x(new Date(d.dateCreated))})
                                .attr("cy", function (d){ return y(d.amount) })
                    }
                    Polymer.dom(this.$.scatterPlotDiv).appendChild(Polymer.dom(this.$.scatterPlotDiv).childNodes[0])
                },
                legendChart:function () {
                    var wrap = d3.select("#scatterPlotDiv svg").append('g').attr('class', 'legendWrap');
                    var series = wrap.selectAll('.series').data(this.transactionsStats.getTransactionTypesData());
                    var seriesEnter = series.enter().append('g').attr('class', 'series disabled')
                            .on('click', function(d, i) {
                                d.enabled = !d.enabled
                                series.classed('disabled', function(d1) {
                                    console.log(d1.type + "------- disabled: " + ((d1.type === d.type) &&  d.enabled))
                                    return ((d1.type === d.type) &&  d.enabled)});
                                console.log("seriesEnterseriesEnterseriesEnter" + JSON.stringify(d))
                                this.legendDispatch.legendClick(d, i);
                            })
                            .on('mouseover', function(d, i) {
                                //this.legendDispatch.legendMouseover(d, i);
                            })
                            .on('mouseout', function(d, i) {
                                //this.legendDispatch.legendMouseout(d, i);
                            });

                    seriesEnter.append('circle')
                            .style('fill', function(d, i){ return this.color(d.type)}.bind(this))
                            .style('stroke', function(d, i){ this.color(d.type)}.bind(this))
                            .attr('r', 5);
                    seriesEnter.append('text')
                            .text(function(d) { return d.type})
                            .attr('text-anchor', 'start')
                            .attr('dy', '.32em')
                            .attr('dx', '8');
                    series.exit().remove();

                    var ypos = 5,
                            newxpos = 5,
                            maxwidth = 0,
                            xpos;
                    series.attr('transform', function(d, i) {
                        var length = d3.select(this).select('text').node().getComputedTextLength() + 28;
                        xpos = newxpos;
                        //TODO: 1) Make sure dot + text of every series fits horizontally, or clip text to fix
                        //      2) Consider making columns in line so dots line up
                        //         --all labels same width? or just all in the same column?
                        //         --optional, or forced always?
                        if (this.width < this.margin.left + this.margin.right + xpos + length) {
                            newxpos = xpos = 5;
                            ypos += 20;
                        }
                        newxpos += length;
                        if (newxpos > maxwidth) maxwidth = newxpos;
                        return 'translate(' + xpos + ',' + ypos + ')';
                    }.bind(this));
                    this.legend.height = this.legend.margin.top + this.legend.margin.bottom + ypos + 15;
                    d3.select(".legendWrap").attr('transform', 'translate(' + (this.legend.margin.left) + ',' +
                            (this.legend.margin.top) + ' )')
            }
            });
        })();
    </script>
</dom-module>