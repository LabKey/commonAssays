/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var g_graphOptions = {
    subset : null,
    xAxis : null,
    yAxis : null,
    points : [],
    complexGate: false,
    intervalGate: false,
    dirty: false
};

function reloadGraph()
{
    var elGraph = document.getElementById("graph");
    if (!g_graphOptions.subset)
    {
        return;
    }
    if (!g_graphOptions.xAxis)
    {
        elGraph.src = "about:blank";
        return;
    }
    var src = g_urlGraphWindow +
              "&xaxis=" + urlEncode(g_graphOptions.xAxis) +
              "&subset=" + urlEncode(g_graphOptions.subset);
    if (g_graphOptions.yAxis)
    {
        src += "&yaxis=" + urlEncode(g_graphOptions.yAxis);
    }
    src += "&width=400&height=400";
    elGraph.src = src;
}

function parameterOptions(curParam, axis)
{
    var ret = [];
    if (axis == 1)
    {
        ret.push('<option value=""');
        if (!curParam || curParam.length == 0)
            ret.push(' selected');
        ret.push('>[[histogram]]</option>');
    }
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
    if (!axis || axis.length == 0)
        return "Count";
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
        var ret = ['<table><tr><td colspan="2">'];
        ret.push(g_graphOptions.subset);
        ret.push('</td></tr>');
        ret.push('<tr><th>X Axis</th><th>Y Axis</th></tr>')
        ret.push('<tr><td><select id="xAxis" onchange="setXAxis(this)">');
        ret.push(parameterOptions(g_graphOptions.xAxis, 0));
        ret.push('</select></td><td><select id="yAxis" onchange="setYAxis(this)">');
        ret.push(parameterOptions(g_graphOptions.yAxis, 1));
        ret.push('</select></td></tr>')
        if (g_graphOptions.complexGate)
        {
            ret.push('<tr><td colspan="2">Warning: this population already has a complex gate that cannot be edited with this tool.</td></tr>');
        }
        ret.push("<tr><td colspan=\"2\">To define a new gate, choose the X and Y parameters, and then click on the graph.</td></tr>");
        ret.push('</table>');
        return ret.join("");
    }
    var interval = g_graphOptions.intervalGate || !g_graphOptions.yAxis;
    function row(index)
    {
        var ret = BaseObj();
        if (interval)
        {
            ret.str = ['<tr id="row|index|" onclick="selectPoint(|index|)">',
                '<td>',
                index == 0 ? 'Min:' : 'Max:',
                '</td>',
                '<td><input type="hidden" name="ptX" value="|x|">|x|</td></tr>'].join('');
        }
        else
        {
            ret.str = ['<tr id="row|index|" onclick="selectPoint(|index|)">',
             '<td><input type="hidden" name="ptX" value="|x|">',
             '|x|</td>',
             '<td><input type="hidden" name="ptY" value="|y|">',
             '|y|</td></tr>'].join('');
        }
        ret.x = g_graphOptions.points[index].x;
        ret.y = g_graphOptions.points[index].y;
        ret.index = index;
        return ret;
    }
    var ret = BaseObj();
    var saveable = g_graphOptions.points.length >= 2 && g_graphOptions.dirty;
    if (interval)
    {
        ret.str = ['<form method="post" action="|formAction|">',
                '<input type="hidden" name="xaxis" value="|xAxis|">',
                '<input type="hidden" name="yaxis" value="|yAxis|">',
                '<input type="hidden" name="subset" value="|subset|">',
                '<table class="gateEditor" border="1">',
                '<tr><th colspan="2">|subset|</th></tr>',
                '<tr><td colspan="2">Interval gate on |xAxisLabel|</td></tr>',
                '<tr><td colspan="2">Plotted against: <select id="yAxis" onchange="setYAxis(this)">',
                parameterOptions(g_graphOptions.yAxis, 1),
                '</select></td></tr>',
                '|rows|',
                '<tr><td colspan="2"><input type="button" value="Clear All Points" onclick="setPoints([])"></td></tr>',
                saveable ? '<tr><td colspan="2"><input type="submit" value="Save Changes"></td></tr>' : '',
                '</table></form>'].join('');
    }
    else
    {
        ret.str = ['<form method="post" action="|formAction|">',
                '<input type="hidden" name="xaxis" value="|xAxis|">',
                '<input type="hidden" name="yaxis" value="|yAxis|">',
                '<input type="hidden" name="subset" value="|subset|">',
                '<table class="gateEditor" border="1">',
                '<tr><th colspan="2">|subset|</th></tr>',
                '<tr><th>|xAxisLabel|</th><th>|yAxisLabel|</th></tr>',
                '|rows|',
                '<tr><td colspan="2"><input type="button" value="Clear All Points" onclick="setPoints([])"></td></tr>',
                saveable ? '<tr><td colspan="2"><input type="submit" value="Save Changes"></td></tr>' : '',
                '</table></form>'].join('');
    }
    ret.formAction = g_formAction;
    ret.xAxis = g_graphOptions.xAxis;
    ret.yAxis = g_graphOptions.yAxis;
    ret.xAxisLabel = getLabel(g_graphOptions.xAxis);
    ret.yAxisLabel = getLabel(g_graphOptions.yAxis);
    ret.subset = g_graphOptions.subset;
    ret.rows = StringArray();
    
    for (var i = 0; i < g_graphOptions.points.length; i ++)
    {
        ret.rows.push(row(i));
    }
    return ret;
}

function updateGateEditor()
{
    if (!g_graphOptions.subset)
    {
        initGraph();
        return;
    }
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
    g_graphOptions.dirty = false;
    if (pop.gate)
    {
        g_graphOptions.xAxis = pop.gate.xAxis;
        g_graphOptions.yAxis = pop.gate.yAxis;
        g_graphOptions.points = pop.gate.points;
        g_graphOptions.intervalGate = pop.gate.intervalGate;
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
    g_graphOptions.dirty = true;
    updateGateEditor();
}

function setPoints(pts)
{
    g_graphOptions.points = pts;
    g_graphOptions.dirty = true;
    if (pts.length == 0)
    {
        g_graphOptions.intervalGate = false;
    }
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
    g_graphOptions.intervalGate = false;
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
        document.getElementById("polygon").innerHTML = '';
        document.getElementById("graph").src = g_urlInstructions;
    }
}

function setWell(id)
{
    g_urlGraphWindow = g_urlGraphWindow.replace(/wellId=[0-9]*/, "wellId=" + id);
    g_formAction = g_formAction.replace(/wellId=[0-9]*/, "wellId=" + id);
    reloadGraph();
}