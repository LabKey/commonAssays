if (!LABKEY.ms2) {
    LABKEY.ms2 = {};
}

if (!LABKEY.ms2.PeptideIntensityHeatMap) {
    LABKEY.ms2.PeptideIntensityHeatMap = {

        addHeatMap: function (peptideCharacteristics, colors) {

            const y_axis = peptideCharacteristics.length * 10;
            const x_axis = 0;

            var rectWidth = 30;

            var svgContainer = d3.select(".heatmap").append("svg")
                    .attr("height", rectWidth * peptideCharacteristics.length + y_axis)
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
                    .text((d) => d.toString())
                    .attr("y", (d,i) => y_axis + (rectWidth*i) + 25)
                    .attr("x", x_axis + 35);

        },

        changeView: function () {
            var viewBy = $("#peptide-setting-select").val();
            var currentUrl = new URL(window.location.href);
            currentUrl.searchParams.set('viewBy', viewBy)
            window.location = currentUrl.toString();
        }
    }

}