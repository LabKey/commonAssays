var g_graphOptions = {
    subset : null,
    xAxis : null,
    yAxis : null,
    points : [],
    complexGate: false
};


function reloadGraph()
{
    var elGraph = document.getElementById("graph");
    if (!g_graphOptions.subset || !g_graphOptions.xAxis || !g_graphOptions.yAxis)
    {
        elGraph.src = "about:blank";
        return;
    }
    var src = g_urlGraphWindow +
              "&x=" + urlEncode(g_graphOptions.xAxis) +
              "&y=" + urlEncode(g_graphOptions.yAxis) +
              "&subset=" + urlEncode(g_graphOptions.subset);
    src += "&width=400&height=400";
    elGraph.src = src;
}

function parameterOptions(curParam)
{
    var ret = [];
    for (var i = 0; i < parameters.length; i ++)
    {
        ret.push('<option value="' + parameters[i].name + '"');
        if (curParam == parameters[i].name)
        {
            ret.push(' selected');
        }
        ret.push('>' + parameters[i].label + '</option>');
    }
    return ret.join("");
}

function setXAxis(el)
{
    g_graphOptions.xAxis = getValue(el);
    updateAll();
}
function setYAxis(el)
{
    g_graphOptions.yAxis = getValue(el);
    updateAll();
}

function getLabel(axis)
{
    for (var i = 0; i < parameters.length; i ++)
    {
        if (parameters[i].name == axis)
            return parameters[i].label;
    }
    return axis;
}

function gatePointEditor()
{
    if (!g_graphOptions.points || g_graphOptions.points.length == 0)
    {
        var ret = ['<table class="normal"><tr><td colspan="2">'];
        ret.push(g_graphOptions.subset);
        ret.push('</td></tr>');
        ret.push('<tr><td><select id="xAxis" onchange="setXAxis(this)">');
        ret.push(parameterOptions(g_graphOptions.xAxis));
        ret.push('</select></td><td><select id="yAxis" onchange="setYAxis(this)">');
        ret.push(parameterOptions(g_graphOptions.yAxis));
        ret.push('</select></td></tr>')
        if (g_graphOptions.complexGate)
        {
            ret.push('<tr><td colspan="2">Warning: this population already has a complex gate that cannot be edited with this tool.</td></tr>');
        }
        ret.push("<tr><td colspan=\"2\">To define a new gate, choose the X and Y parameters, and then click on the graph.</td></tr>");
        ret.push('</table>');
        return ret.join("");
    }
    function row(index)
    {
        var ret = BaseObj();
        ret.str = ['<tr id="row|index|" onclick="selectPoint(|index|)">',
             '<td><input type="hidden" name="ptX[|index|]" value="|x|">',
             '|x|</td>',
             '<td><input type="hidden" name="ptY[|index|]" value="|y|">',
             '|y|</td></tr>'].join('');
        ret.x = g_graphOptions.points[index].x;
        ret.y = g_graphOptions.points[index].y;
        ret.index = index;
        return ret;
    }
    var ret = BaseObj();
    ret.str = ['<form class="normal" method="post" action="|formAction|">',
            '<input type="hidden" name="xaxis" value="|xAxis|">',
            '<input type="hidden" name="yaxis" value="|yAxis|">',
            '<input type="hidden" name="subset" value="|subset|">',
            '<table class="gateEditor" border="1">',
            '<tr><th colspan="2">|subset|</th></tr>',
            '<th>|xAxisLabel|</th><th>|yAxisLabel|</th></tr>',
            '|rows|',
            '<tr><td colspan="2"><input type="button" value="Clear All Points" onclick="setPoints([])"></td></tr>',
            '<tr><td colspan="2"><input type="submit" value="Save Changes"></td></tr>',
            '</table></form>'].join('');
    ret.formAction = g_formAction;
    ret.xAxis = g_graphOptions.xAxis;
    ret.yAxis = g_graphOptions.yAxis;
    ret.xAxisLabel = getLabel(g_graphOptions.xAxis);
    ret.yAxisLabel = getLabel(g_graphOptions.yAxis);
    ret.subset = g_graphOptions.subset;
    ret.rows = StringArray();
    if (g_graphOptions.points.length)
    {
        for (var i = 0; i < g_graphOptions.points.length; i ++)
        {
            ret.rows.push(row(i));
        }
    }
    return ret;
}

function updateGateEditor()
{
    document.getElementById("polygon").innerHTML = gatePointEditor().toString();
    if (window.frames.graph.updateImage)
    {
        window.frames.graph.updateImage();
    }
}

function updateAll()
{
    reloadGraph();
    updateGateEditor();
}

function setPopulation(name)
{
    var pop = populations[name];
    g_graphOptions.subset = name;
    if (pop.gate)
    {
        g_graphOptions.xAxis = pop.gate.xAxis;
        g_graphOptions.yAxis = pop.gate.yAxis;
        g_graphOptions.points = pop.gate.points;
    }
    else
    {
        g_graphOptions.xAxis = g_graphOptions.yAxis = g_graphOptions.points = null;
    }

    g_graphOptions.complexGate = pop.complexGate;
    updateAll();
    if (subsetWellMap[name])
    {
        setWell(subsetWellMap[name]);
        setValue(document.getElementById("wells"), subsetWellMap[name]);
    }
}

function setPoint(index, pt)
{
    g_graphOptions.points[index] = pt;
    updateGateEditor();
}

function setPoints(pts)
{
    g_graphOptions.points = pts;
    updateGateEditor();
}

function getPoints()
{
    return g_graphOptions.points;
}

function trackPoint(pt)
{
    window.status = Math.round(pt.x) + "," + Math.round(pt.y);
}

function createNewPopulation()
{
    var name = window.prompt("What do you want to call this new population?", "subset");
    if (!name)
        return;
    var parent = getValue(document.getElementById("subset"));
    var fullName;
    if (parent)
    {
        fullName = parent + "/" + name;
    }
    else
    {
        fullName = name;
    }
    if (populations[fullName])
    {
        window.alert("There is already a population " + fullName);
        return;
    }
    g_graphOptions.subset = fullName;
    g_graphOptions.xAxis = null;
    g_graphOptions.yAxis = null;
    g_graphOptions.points = [];
    g_graphOptions.complexGate = false;
    updateAll();
}

function initGraph(subset, xAxis, yAxis)
{
    if (subset)
    {
        setPopulation(subset);
    }
    else
    {
        document.getElementById("polygon").innerHTML =
            "To begin using the gate editor, either select a population from the dropdown, or click the 'new' button to create a new population."
    }
}

function setWell(id)
{
    g_urlGraphWindow = g_urlGraphWindow.replace(/wellId=[0-9]*/, "wellId=" + id);
    g_formAction = g_formAction.replace(/wellId=[0-9]*/, "wellId=" + id);
    reloadGraph();
}