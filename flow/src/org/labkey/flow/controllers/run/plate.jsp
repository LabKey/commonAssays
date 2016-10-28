<%
/*
 * Copyright (c) 2008-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page import="org.labkey.flow.query.FlowPropertySet" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<style type="text/css">
    .well {height:24px; width:24px; font-size:8pt; text-align:center; vertical-align:middle; color:#000000;}
    .wellEmpty {height:24px; width:24px; font-size:8pt; text-align:center; vertical-align:middle; color:#997777; background-color:#ffdddd;}
</style>
<script type="text/javascript">

var queryParams = {};
function parseParameters()
{
	var s = window.location.search;
	if (s.charAt(0) == '?')
		s = s.substring(1);
	var a = s.split('&');
	for (var i=0 ; i<a.length ; i++)
	{
		var kv = a[i].split('=');
		queryParams[kv[0]] = kv[1];
	}
}
parseParameters();

var options = {};
options.RUNID = 0;
options.STATISTIC = 'Statistic/Count';
options.PLATENAME = "FCSFile/Keyword/PLATE NAME";
options.PLATEID = "FCSFile/Keyword/PLATE ID";
options.WELLID = "FCSFile/Keyword/WELL ID";
options.STIM = "FCSFile/Keyword/stim";
options.CONCENTRATION= "FCSFile/Keyword/concentration";
if (queryParams['runId'])
	options.RUNID = queryParams['runId'];

var h = Ext4.util.Format.htmlEncode;
function _id(s) {return document.getElementById(s);}
function _div(node) { var div = document.createElement("DIV"); if (node) div.appendChild(node); return div; }
function _tbody() { return document.createElement("TBODY"); }
function _tr() { return document.createElement("TR"); }
function _td(node) { var td = document.createElement("TD"); if (node) td.appendChild(node); return td;}
function _text(s) {return document.createTextNode(s)}
function removeChildren(e)
{
    while (e && e.firstChild)
        e.removeChild(e.firstChild);
}

Color = function(r,g,b)
{
    this.r = r;
    this.g = g;
    this.b = b;
};

function _fix(c)
{
    if (c > 255) return 255;
    if (c < 0) return 0;
    return Math.round(c);
}

Color.prototype.fix = function()
{
    this.r = _fix(this.r);
    this.g = _fix(this.g);
    this.b = _fix(this.b);
    return this;
};

Color.prototype.toCSS = function()
{
    this.fix();
    var c = this.r * (0x10000) + this.g * (0x100) + this.b * (0x1);
    var color = c.toString(16);
    return "#" + "000000".substr(color.length) + color;
};

Color.prototype.isDark = function()
{
    var a = this.r + this.g + this.b;
    return a < 255;
};

Color.interpolate = function(a,b,x)
{
    return new Color(
        a.r * (1-x) + b.r * x,
        a.g * (1-x) + b.g * x,
        a.b * (1-x) + b.b * x
    );
};

// functions that take value [0-1.0] and map to color(0-255,0-255,0-255)
var HEATMAPS =
{
blackToWhite: function (x)
    {
        var c = Math.floor(x * 255);
        return new Color(c,c,c);
    },

whiteToBlack: function(x)
    {
        var c = Math.floor((1.0-x) * 255);
        return new Color(c,c,c);
    },

_interpolate: function(h, x)
    {
        var intervals = h.length-1;
        x = x * intervals;
        var i = Math.floor(x);
        if (i == intervals)
            return h[intervals];
        x = x - i;
        var a = h[i], b = h[i+1];
        return Color.interpolate(a,b,x);
    },

_heat: [new Color(0,0,0), new Color(105,0,0),new Color(192,23,0),new Color(255,150,38),new Color(255,255,240)],
heat: function(x)
    {
        return HEATMAPS._interpolate(HEATMAPS._heat, x);
    },

_response: [new Color(255,255,204), new Color(255,236,159),new Color(253,217,118),new Color(253,177,75),new Color(252,141,59),new Color(251,78,42),new Color(226,26,28),new Color(188,0,37),new Color(128,0,37)],
response: function(x)
    {
        return HEATMAPS._interpolate(HEATMAPS._response, x);
    }
};

var heatmap = HEATMAPS.response;


function failureHandler(responseObj)
{
    alert("Failure: " + responseObj.exception);
}

function Runs_Handler(responseObj)
{
}

function FCSAnalyses_Handler(responseObj)
{
    var i, j, r;

    // debug grid display
    if (_id("result"))
	{
	    var s = "";
	    var fieldCount = responseObj.metaData.fields.length;
	    s += "<table><tr>";
		for (j=0 ; j<fieldCount ; j++)
		{
	    	s += "<th>" + h(responseObj.metaData.fields[j].name) + "</th>";
		}
	    for (i=0 ; i< responseObj.rowCount ; i++)
	    {
	    	s += "<tr>";
	    	r = responseObj.rows[i];
	    	for (j=0 ; j<fieldCount ; j++)
	    	{
	    		s += "<td>" + h(r[responseObj.metaData.fields[j].name]) + "</td>";
	    	}
	    	s += "</tr>";
	    }
	    s += "</table>";
        _id("result").innerHTML = s;
	}

	var map = {};
    var plateSet = {};
    for (i=0 ; i< responseObj.rowCount ; i++)
    {
    	r = responseObj.rows[i];
    	var well = {};
        well.plateid = r[options.PLATEID];
        well.wellid = r[options.WELLID];
    	well.statistic = options.STATISTIC;
    	well.value = options.STATISTIC in r ? r[options.STATISTIC] : null;
    	well.stim = r[options.STIM];
        well.platename = r[options.PLATENAME];
        if (!well.platename)
            well.platename = well.plateid;
        well.concentration = r[options.CONCENTRATION];
        var v = well.value == null ? "#" : (Math.round(well.value*100.0)/100.0);
        well.title = well.stim + " " + well.concentration + "  \n" + v;

        if (well.value == null != null)
        {
            if (!well.plateid)
                well.plateid = 'default';
            plateSet[well.plateid] = {name:well.platename, id:well.plateid};
        }

        well.key = well.plateid + "-" + well.wellid;
        map[well.key] = well;
    }

    generateTables(plateSet);
    updateHeapMap(plateSet, map);
}


function Statistic_onChange()
{
    var e = Ext4.get("statistic");
    if (e.dom.selectedIndex == 0)
        return;
    var stat =  e.getValue();
    if (stat && options.STATISTIC != stat)
    {
        options.STATISTIC = stat;
        getData();
    } 
}

function Statistics_Handler(responseObj)
{
    var rows = responseObj.rows;
    var stats = [];
    for (var i=0 ; i< responseObj.rowCount ; i++)
    {
        stats.push(rows[i]['Name'])
    }
    populateStatistics(stats);
}

function populateStatistics(stats)
{
    var select = Ext4.get("statistic").dom;
    for (var i=0 ; i<stats.length ; i++)
    {
        var name = stats[i];
        select.options[select.options.length] = new Option(name, 'Statistic/' + encodeFieldKey(name));
    }
}

function getData()
{
    Ext4.get("plateDisplay").update("<img src='<%=request.getContextPath()%>/<%=PageFlowUtil.extJsRoot()%>/resources/images/default/shared/blue-loading.gif'>");
    LABKEY.Query.selectRows(
        {
            schemaName:'flow',
            queryName:'FCSAnalyses',
            successCallback:FCSAnalyses_Handler,
            errorCallback:failureHandler,
            filterArray:[ LABKEY.Filter.create('run', options.RUNID)],
            columns:options.WELLID+","+options.STATISTIC+","+options.PLATEID+","+options.STIM+","+options.CONCENTRATION+","+options.PLATENAME
        });
}

function getStatistics()
{
    if (statistics)
    {
        populateStatistics(statistics);
    }
    else
    {
        LABKEY.Query.selectRows(
        {
            schemaName:'flow',
            queryName:'Statistics',
            successCallback:Statistics_Handler,
            errorCallback:failureHandler
        });
    }
}


var down = ["A","B","C","D","E","F","G","H"];
var across = ["01","02","03","04","05","06","07","08","09","10","11","12"];
var wellTDs = {};

function tdForWell(id)
{
    var td = wellTDs[id];
    if (!td)
    {
        td = document.getElementById(id);
        wellTDs[id] = td;
    }
    return td;
}

function generateTables(plates)
{
    var ret = [];
    var id;

    var foundAll = true;
    for (plate in plates)
    {
        if (!Ext4.get("T" + plate))
            foundAll = false;
    }
    if (foundAll)
        return;

    for (plate in plates)
    {
        ret.push(plates[plate].name);
        ret.push("<table id='T" + plate + "' class='normal' style='border:solid 1px black;'");
        for (var x=0 ; x<8 ; x++)
        {
            ret.push("<tr>");
            for (var y=0 ; y<12 ; y++)
            {
                id = down[x] + across[y];
                ret.push("<td class='wellEmpty' id='" + plate + "-" + id + "'>" + id + "</td>");
            }
            ret.push("</tr>");
        }
        ret.push("</table>");
    }
    Ext4.get("plateDisplay").update(ret.join(""),false);
    wellTDs = {};
}


function updateHeapMap(plates, map)
{
	var maxValue=0;
	var minValue=10000000;
	var well, value, scaled;
    var key, plate;

    for (key in map)
    {
        well = map[key];
        value = well.value;
        if (value > maxValue) maxValue = value;
        if (value < minValue) minValue = value;
    }
    // range fiddling
    if (minValue < maxValue*0.1)
        minValue = 0;
    if (minValue > maxValue*0.8)
        minValue = maxValue*0.8;

    minValue = 10*Math.floor(minValue/10);
    maxValue = 10*Math.ceil(maxValue/10);

    for (plate in plates)
    {
        for (var x=0 ; x<8 ; x++)
        {
            for (var y=0 ; y<12 ; y++)
            {
                var wellid = plate + "-" + down[x] + across[y];
                var td = tdForWell(wellid);
                td.className="wellEmpty";
                td.style.backgroundColor = null;
                td.style.color = null;
                td.title = "";

                well = map[wellid];
                if (!well)
                    continue;

                value = well.value;
                scaled = null;
                
                if (value)
                {
                    if (maxValue > minValue)
                        scaled = (value - minValue) / (maxValue-minValue);
                    else
                        scaled = 0.5;
                }

                if (scaled)
                {
                    try
                    {
                        var c = heatmap(scaled);
                        td.className = "well";
                        td.style.backgroundColor = c.toCSS();
                        if (c.isDark())
                            td.style.color = "#ffffff";
                        td.title = well.title;
                    }
                    catch (ex)
                    {
                        window.alert(ex + " " + scaled + " : " + td.style.backgroundColor);
                    }
                }
            }
        }
    }
    
    showHeatMap(minValue, maxValue);
}

function showHeatMap(min,max)
{
    if (Ext4.get("legendMax"))
    {
        Ext4.get("legendMax").update(max);
        Ext4.get("legendMin").update(min);
        return;
    }
    var height=180;
    var html = [];
    html.push("&nbsp;<br><span id='legendMax'>" + max + "</span><br>");

    var img = "<img src='" + LABKEY.contextPath + "/_.gif' width=20 height=1 border=0 style='background-color:COLOR;'><br>";

    for (var i=height ; i>=0 ; i--)
    {
        var c = heatmap(i/height);
        html.push(img.replace("COLOR", c.toCSS()));
    }
    html.push("<span id='legendMin'>" + min + "</span><br>");
    Ext4.get("showMap").update(html.join(""));
}


function encodeFieldKey(k)
{
    return k.replace(/\//g, String.fromCharCode(36)+'S').replace(/&/g, String.fromCharCode(36)+'A');
}


Ext4.onReady(function()
{
    getStatistics();
    //getData();
});

var statistics = [<%
String comma = "";
FlowPropertySet ps = new FlowPropertySet(getContainer());
for (AttributeCache.StatisticEntry s : ps.getStatistics())
{
    %><%=comma%><%=PageFlowUtil.jsString(s.getAttribute().toString())%><%
    comma = ",";
}
%>];
</script>

<select id="statistic" onchange="Statistic_onChange();" style="width:340px;">
    <option>--choose statistic--</option>
</select>

<table><tr>
    <td><div id="plateDisplay" class="extContainer" style="height:210px; width:314px"></div></td>
    <td><div id="showMap" class="extContainer" style="height:210px;"></div></td>
</tr></table>

</div>

<!--<div id="result"></div>-->
