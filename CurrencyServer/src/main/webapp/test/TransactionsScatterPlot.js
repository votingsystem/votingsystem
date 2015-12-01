function TransactionsScatterChart() {
    var margin,
        tooltipsDiv,
        transactionsStats,
        offsetTop, offsetLeft,
        width,
        height,
        rMin = 4,
        rMax = 20,
        xAxisLabelText,
        yAxisLabelText,
        resetButton;
        color = d3.scale.category10(),
        rScale = d3.scale.linear().range([rMin, rMax])

    var transactionFilter = []

    var x = d3.time.scale(),
        y = d3.scale.linear(),
        xAxis = d3.svg.axis().scale(x).orient('bottom').ticks(5),
        yAxis = d3.svg.axis().scale(y).orient('left').ticks(5),
        transactionsLegend = TransactionsLegend().height(30).width(500).color(color);

    function chart(selection) {
        selection.each(function(data) {
            width = width - margin.left - margin.right
            height = height - margin.top - margin.bottom


            data = data.filter(function(d) {
                var index = transactionFilter.indexOf(d.type);
                return index < 0 }).map(function(d) { return d});

            rScale.domain(d3.extent(data, function (d){ return d.amount }));
            x = x.domain(d3.extent(data, function(d) { return d.dateCreated; })).range([0, width]).nice();
            y = y.domain([0, d3.max(data, function(d) { return d.amount; })]).range([height, 0]).nice();
            var zoom = d3.behavior.zoom().x(x).y(y).scaleExtent([-10, 10]).on("zoom", zoomed)


            var svg = d3.select(this).append("g")
                .attr("transform", "translate(" + margin.left + " ," + margin.top + ")")
            var scatterWrap = svg.append("rect")
                .attr("width", width)
                .attr("height", height)
                .attr("class", "scatterRect").call(zoom);

            var clip = svg.append("clipPath")
                .attr("id", "clip")
                .append("rect")
                .attr("x", 0)
                .attr("y", 0)
                .attr("width", width)
                .attr("height", height);
            var chartBody = svg.append("g").attr("clip-path", "url(#clip)");

            resetButton.style.left = (50 + offsetLeft) + "px";
            resetButton.style.top = offsetTop + "px";
            resetButton.onclick= function(){
                zoom.translate([0, 0]).scale(1)
                zoomed()
            }

            var circles = chartBody.selectAll("circle.node").data(data);
            circles.enter().append("circle");
            circles.attr("class", "transaction")
                .attr("cx",      function (d){ return   x(new Date(d.dateCreated))})
                .attr("cy",      function (d){ return   y(d.amount) })
                .attr("r",       function (d){ return rScale(d.amount) })
                .attr("style",    function (d){ return   "fill:" + color(d.type) + "; stroke:" + d3.rgb(color(d.type)).darker(1) })
                .on("mouseover", function(d) {
                    tooltipsDiv.style("opacity", .9);
                    tooltipsDiv.html(
                            d.type + "<br/>" +
                            '<a href= "'+d.id+'" target="_blank">' + new Date(d.dateCreated).formatWithTime() + "</a>" +
                            "<br/>"  + d.amount + " " + d.currencyCode)
                        .style("left", (d3.event.pageX - 30) + "px")
                        .style("top", (d3.event.pageY ) + "px");
                })
                .on("mouseout", function() {
                    tooltipsDiv.transition().duration(1000).style("opacity", 0);
                });
            circles.exit().remove();

            transactionsLegend.dispatch.on('legendClick', function(d, i) {
                d.disabled = !d.disabled;
                console.log("----*----- legendClick: " + JSON.stringify(d))
                if(d.disabled) {
                    transactionFilter.push(d.type)
                } else {
                    var index = transactionFilter.indexOf(d.type);
                    if (index > -1) transactionFilter.splice(index, 1);
                }
                //selection.transition().call(chart)
            });


            transactionsLegend.dispatch.on('legendMouseover', function(d, i) {
                d.hover = true;
                selection.transition().call(chart)
            });

            transactionsLegend.dispatch.on('legendMouseout', function(d, i) {
                d.hover = false;
                selection.transition().call(chart)
            });

            svg.append('g').attr('class', 'legendWrap')
                .datum(transactionsStats.getTransactionTypesData())
                .attr('transform', 'translate(' + (width/2 - margin.left) + ',' + (-transactionsLegend.height()) +')')
                .call(transactionsLegend);

            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + (height) + ")")
                .call(xAxis)
                .append("text")
                .attr("class", "label")
                .attr("x", width)
                .attr("y", -6)
                .style("text-anchor", "end")
                .text(xAxisLabelText);

            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("class", "label")
                .attr("transform", "rotate(-90)")
                .attr("y", 6)
                .attr("dy", ".71em")
                .style("text-anchor", "end")
                .style("size", "end")
                .text(yAxisLabelText);


            function zoomed() {
                console.log("zoom.scale: " + zoom.scale() + " - x.domain: " + x.domain()[0])
                svg.select(".x.axis").call(xAxis);
                svg.select(".y.axis").call(yAxis);
                circles.attr("cx", function (d){ return x(new Date(d.dateCreated))})
                    .attr("cy", function (d){ return y(d.amount) })
            }
        });
        return chart;
    }

    chart.resetButton = function(_) {
        if (!arguments.length) return resetButton;
        resetButton = _;
        return chart;
    };

    chart.containerDiv = function(_) {
        //var w = window.innerWidth
        //var h = window.innerHeight
        width = _.offsetWidth;
        height = _.offsetHeight;
        offsetTop = _.offsetTop;
        offsetLeft = _.offsetLeft;
        tooltipsDiv = d3.select("#" + _.id).append("div").attr("class", "tooltip").style("opacity", 0);
        return chart;
    }

    chart.transactionsStats = function(_) {
        if (!arguments.length) return transactionsStats;
        transactionsStats = _;
        return chart;
    };

    chart.margin = function(_) {
        if (!arguments.length) return margin;
        margin = _;
        return chart;
    };

    chart.width = function(_) {
        if (!arguments.length) return width;
        width = _;
        return chart;
    };

    chart.height = function(_) {
        if (!arguments.length) return height;
        height = _;
        return chart;
    };

    chart.color = function(_) {
        if (!arguments.length) return color;
        color = _;
        return chart;
    };


    //TODO: consider directly exposing both axes
    //chart.xAxis = xAxis;

    //Expose the x-axis' tickFormat method.
    chart.xAxis = {};
    d3.rebind(chart.xAxis, xAxis, 'tickFormat');

    chart.xAxis.label = function(_) {
        if (!arguments.length) return xAxisLabelText;
        xAxisLabelText = _;
        return chart;
    }

    // Expose the y-axis' tickFormat method.
    //chart.yAxis = yAxis;

    chart.yAxis = {};
    d3.rebind(chart.yAxis, yAxis, 'tickFormat');

    chart.yAxis.label = function(_) {
        if (!arguments.length) return yAxisLabelText;
        yAxisLabelText = _;
        return chart;
    }

    return chart;
}
