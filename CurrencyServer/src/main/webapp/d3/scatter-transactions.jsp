<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/d3.html" rel="import"/>
<link href="../resources/bower_components/vs-checkbox-selector/vs-checkbox-selector.html" rel="import"/>

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
        .icontext {
            text-anchor: middle;
            pointer-events: none;
            font-family: FontAwesome;
            fill: #fff;
        }
    </style>
    <template>
        <div class="horizontal layout" style="margin: 4px;">
            <div>
                <button id="resetButton" style="">reset</button>
            </div>
            <div class="horizontal layout center center-justified" style="border: 1px solid #888; margin: 0 10px 0 10px; font-size: 0.9em;">
                <input type="checkbox" value="timeLimited" on-click="timeCheckboxSelected" checked="true"><i class="fa fa-clock-o"></i> ${msg.timeLimitedLbl}
                <input type="checkbox" value="timeFree" on-click="timeCheckboxSelected" style="margin:0 0 0 10px;" checked="true">${msg.timeFreeLbl}
            </div>

            <vs-checkbox-selector id="typeSelector" width="300px" caption="${msg.typeLbl}" on-checkbox-selected="typeCheckboxSelected"></vs-checkbox-selector>
            <vs-checkbox-selector id="tagSelector" width="200px" caption="${msg.tagLbl}" on-checkbox-selected="tagCheckboxSelected"></vs-checkbox-selector>

        </div>

        <div id="scatterPlotDiv" style="width: 100%; height: 100%; min-height: 200px;"></div>
        <div id="tooltip" class="tooltip" style="">
            <div><strong>{{selectedTransaction.type}}</strong></div>
            <div>{{getDate(selectedTransaction.dateCreated)}}</div>
            <div><a href="{{selectedTransaction.messageSMIMEURL}}" target="_blank">
                {{selectedTransaction.amount}} {{selectedTransaction.currencyCode}}</a>
                <i hidden={{!selectedTransaction.timeLimited}} class="fa fa-clock-o" style="margin:0px 0px 0px 20px;" title="${msg.timeLimitedLbl}"></i></div>
        </div>
    </template>
    <script>
        (function() {
            Polymer({
                is: "scatter-transactions",
                properties: {
                    transactionTypeFilter:{type:Array, value:[]},
                    transactionTimeFilter:{type:Array, value:[]},
                    transactionTagFilter:{type:Array, value:[]},
                    margin:{type:Object, value:{top: 5, right: 10, bottom: 100, left: 50}},
                    chartData:{type:Object}
                },
                ready: function() {
                    console.log(this.tagName + " ready")

                    this.transactionsStats = new TransactionsStats()
                    var rMin = 5, rMax = 20;
                    this.color = d3.scale.category10(),
                            rScale = d3.scale.linear().range([rMin, rMax])

                    this.$.typeSelector.init(Object.keys(transactionsMap), function(item) {
                            return 'font-weight: bold; color: ' + this.color(item) + ';'
                        }.bind(this), function(item) {
                            return transactionsMap[item].lbl
                        })

                    window.addEventListener('resize', function(event){
                        this.chart(this.chartData)
                    }.bind(this));
                    d3.json("/CurrencyServer/rest/transactionVS/from/20151116_0000/to/20151210_0000", function (json) {
                        this.chartData = json.resultList
                        this.chartData.forEach(function(transactionvs) {
                            this.transactionsStats.pushTransaction(transactionvs)
                        }.bind(this))
                        this.$.tagSelector.init(this.transactionsStats.tags, null, null)
                        this.chart(this.chartData)
                    }.bind(this))
                },
                tagCheckboxSelected: function(e) {
                    console.log("typeCheckboxSelected: " + e.detail.value + " - checked: " + e.detail.checked)
                    if(e.detail.checked) {
                        var index = this.transactionTagFilter.indexOf(e.detail.value)
                        this.transactionTagFilter.splice(index, 1)
                    } else {
                        this.transactionTagFilter.push(e.detail.value)
                    }
                    this.filterChart()
                },
                typeCheckboxSelected:function (e) {
                    console.log("typeCheckboxSelected: " + e.detail.value + " - checked: " + e.detail.checked)
                    if(e.detail.checked) {
                        var index = this.transactionTypeFilter.indexOf(e.detail.value)
                        this.transactionTypeFilter.splice(index, 1)
                    } else {
                        this.transactionTypeFilter.push(e.detail.value)
                    }
                    this.filterChart()
                },
                timeCheckboxSelected:function (e) {
                    console.log("timeCheckboxSelected: " + e.target.value + " - checked: " + e.target.checked)
                    if(e.target.checked) {
                        var index = this.transactionTimeFilter.indexOf(e.target.value)
                        this.transactionTimeFilter.splice(index, 1)
                    } else {
                        this.transactionTimeFilter.push(e.target.value)
                    }
                    this.filterChart()
                },
                filterChart:function () {
                    d3.select(this).selectAll(".transaction").attr("display", function(d) {
                        var timeType = d.timeLimited ? 'timeLimited':'timeFree'
                        if(this.transactionTimeFilter.indexOf(timeType) > -1) return 'none'
                        else if(this.transactionTypeFilter.indexOf(d.type) > -1) return 'none'
                        else if(this.transactionTagFilter.indexOf(d.tags[0]) > -1) return 'none'
                        else return 'inline'
                    }.bind(this));
                },
                getDate:function (date) {
                    return new Date(date).formatWithTime()
                },
                chart:function (data) {
                    console.log(this.tagName + " - chart")
                    while (this.$.scatterPlotDiv.hasChildNodes()) {
                        this.$.scatterPlotDiv.removeChild(this.$.scatterPlotDiv.lastChild);
                    }
                    this.width = this.$.scatterPlotDiv.offsetWidth;
                    this.height = this.$.scatterPlotDiv.offsetHeight
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
                    rScale.domain(d3.extent(data, function (d){ return d.amount }));
                    x = x.domain(d3.extent(data, function(d) { return d.dateCreated; })).range([0, this.width]).nice();
                    y = y.domain([0, d3.max(data, function(d) {
                        return d.amount; })]).range([this.height, 0]).nice();
                    var zoom = d3.behavior.zoom().x(x).y(y).scaleExtent([-100, 100]).on("zoom", zoomed)
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
                    var nodes = chartBody.selectAll(".transaction").data(data).enter().append("g").attr("class", "transaction")
                            .attr("transform", function (d){ return 'translate(' + x(new Date(d.dateCreated)) + ' ' + y(d.amount) +')'})
                    var hostElement = this
                    var circles = nodes.append("circle")
                            .attr("r",       function (d){ return rScale(d.amount) })
                            .attr("style",    function (d){ return   "fill:" +
                                    this.color(d.type) + "; stroke:" + d3.rgb(this.color(d.type)).darker(1)}.bind(this))
                            .on("mouseover", function(d) {
                                hostElement.$.tooltip.style.display = 'block'
                                hostElement.$.tooltip.style.top = (d3.event.pageY ) + "px"
                                hostElement.$.tooltip.style.left = (d3.event.pageX - 30) + "px"
                                hostElement.selectedTransaction = d

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
                                svg.append("g")
                                        .attr("class", "guide")
                                        .append("line")
                                        .attr("x1", circle.attr("cx"))
                                        .attr("x2", 0)
                                        .attr("y1", circle.attr("cy"))
                                        .attr("y2", circle.attr("cy"))
                                        .style("stroke", circle.style("fill"))
                            })
                            .on("mouseout", function(d) {
                                var circle = d3.select(this);
                                circle.transition().duration(800).style("opacity", .5).attr("r", rScale(d.amount)).ease("elastic");
                                d3.selectAll(".guide").transition().duration(100).styleTween("opacity", function() { return d3.interpolate(.5, 0); }).remove()
                            })

                    nodes.append("text")
                            .attr("class","FontAwesome icontext")
                            .style("font-size", function(d) {
                                var r = rScale(d.amount)
                                return Math.min(2 * r, (2 * r - 8) / this.getComputedTextLength() * 24) + "px"; })
                            .attr("dy", ".35em")
                            .text(function (d){ if(d.timeLimited) return "\uf017" });

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
                    function zoomed() {
                        //console.log("zoom.scale: " + zoom.scale() + " - x.domain: " + x.domain()[0])
                        svg.select(".x.axis").call(xAxis);
                        svg.select(".y.axis").call(yAxis);
                        nodes.attr("transform", function (d){ return 'translate(' + x(new Date(d.dateCreated)) + ' ' + y(d.amount) +')'})
                    }
                    //hack to refresh styles, I haven't found other way
                    Polymer.dom(this.$.scatterPlotDiv).appendChild(Polymer.dom(this.$.scatterPlotDiv).childNodes[0])
                }
            });
        })();
    </script>
</dom-module>