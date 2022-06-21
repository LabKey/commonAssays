if (!LABKEY.ms2) {
    LABKEY.ms2 = {};
}

if (!LABKEY.ms2.PeptideCharacteristicLegend) {
    LABKEY.ms2.PeptideCharacteristicLegend = {

        addHeatMap: function (peptideCharacteristics, colors) {

            const len = peptideCharacteristics.length;
            const min = peptideCharacteristics[0];
            const max = peptideCharacteristics[len-1];
            const y_axis = len/2;
            const x_axis = 0;

            var rectWidth = 25;
            var count = 0;

            var svgContainer = d3.select(".heatmap").append("svg")
                    .attr("height", rectWidth * len + 20)
                    .attr("width", 200);

            var rect = svgContainer.selectAll(".rect")
                    .data(peptideCharacteristics)
                    .enter()
                    .append("rect");
            rect.attr("y", (d,i) => y_axis + (rectWidth*i))
                    .attr("x", function (d, i) { return x_axis; })
                    .attr("height", rectWidth)
                    .attr("width", 20)
                    .style("fill", (d) => colors[d]);
            svgContainer.selectAll('.text')
                    .data(peptideCharacteristics)
                    .enter().append('text')
                    .text((d) => {
                        // display every 5th value
                        var str = "";
                        if (d.toString() === min.toString() || d.toString() === max.toString() || count === 4) {
                           str = d.toPrecision(3).toString();
                        }
                        if (count === 4) {
                            count = 0;
                        }
                        else {
                            count++;
                        }
                        return str;
                    })
                    .attr("y", (d,i) => y_axis + (rectWidth*i) + 20)
                    .attr("x", x_axis + 35);

        },

        changeView: function (settingName, elementId) {
            var viewBy = $("#" + elementId).val();
            var currentUrl = new URL(window.location.href);
            currentUrl.searchParams.set(settingName, viewBy)
            window.location = currentUrl.toString();
        }
    }

}