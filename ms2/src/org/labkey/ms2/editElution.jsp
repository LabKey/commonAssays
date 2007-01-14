<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.ms2.Quantitation"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="org.labkey.api.view.ViewURLHelper"%>
<%@ page import="org.labkey.ms2.MS2Peptide"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%
    JspView<MS2Controller.EditElutionGraphContext> me = (JspView<MS2Controller.EditElutionGraphContext>) HttpView.currentView();
    MS2Controller.EditElutionGraphContext ctx = me.getModel();
    String contextPath = request.getContextPath();
    MS2Peptide p = ctx.getPeptide();
    Quantitation quant = ctx.getQuantitation();
    DecimalFormat format = new DecimalFormat();
    DecimalFormat decimalRatioFormat = new DecimalFormat();
    decimalRatioFormat.setMaximumFractionDigits(2);
    ViewURLHelper saveUrl = ctx.getUrl().clone();
    saveUrl.setAction("saveElutionProfile.post");
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%= PageFlowUtil.getStandardIncludes() %>
<title>Edit Elution Profile - <%=p%></title>
</head>
<body onload="updateRange('light'); updateRange('heavy');">
<b><%=p.getPeptide()%></b>

<form name="elutionForm" method="post" action="<%= saveUrl %>">

<table>
<tr><td colspan="2" align="center"><strong>Light</strong></td></tr>
<tr>
    <td>
        <table>
            <tr>
                <td>Selected area:</td>
                <td><div id="lightArea"><%= format.format(quant.getLightArea()) %></div></td>
            </tr>
            <tr>
                <td>Scans:</td>
                <td>
                    <input type="text" onkeyup="updateRange('light');" name="lightFirstScan" value="<%= quant.getLightFirstScan() %>" size="4"/> -
                    <input type="text" onkeyup="updateRange('light');" name="lightLastScan" value="<%= quant.getLightLastScan() %>" size="4"/>
                </td>
            </tr>
            <tr>
                <td>Click to set:</td>
                <td>
                    <input type="radio" name="lightBoundary" value="first" checked="true"/>First scan
                </td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input type="radio" name="lightBoundary" value="last"/>Last scan<br/>
                </td>
            </tr>
        </table>
    </td>
    <td height="250" valign="bottom">
<%
for (int i = quant.getMinDisplayScan(); i <= quant.getMaxDisplayScan(); i++)
{
    if (ctx.getLightValue(i) != null)
    {
        if (i >= quant.getLightFirstScan() && i <= quant.getLightLastScan())
        {
            %><img style="border: 1px #000000 solid;" onclick="setRange('light', <%= i %>)" name="lightImgScan<%= i %>" src="<%= contextPath %>/_images/red.gif" height="<%= (int)(ctx.getLightValue(i).floatValue() / ctx.getMaxLightIntensity() * 250)%>" width="5" alt="Scan <%= i %>"/><%
        }
        else
        {
            %><img style="border: 1px #000000 solid;" onclick="setRange('light', <%= i %>)" name="lightImgScan<%= i %>" src="<%= contextPath %>/_images/gray.gif" height="<%= (int)(ctx.getLightValue(i).floatValue() / ctx.getMaxLightIntensity() * 250)%>" width="5" alt="Scan <%= i %>"/><%
        }
    }
    else
    {
        %><img src="<%= contextPath%>/_.gif" height="0" width="5" style="border: 1px #000000 solid;" style="border: 1px #000000 solid;" alt="Scan <%= i %>"/><%
    }
}
%>
</td>
    <td><table height="250"><tr><td valign="top" height="100%"><%= format.format(ctx.getMaxLightIntensity()) %></td></tr><tr><td valign="bottom">0</td></tr></table></td>
</tr>
<tr><td></td><td><table width="100%"><tr><td><%= quant.getMinDisplayScan() %></td><td align="center"><%= ( quant.getMinDisplayScan() + quant.getMaxDisplayScan() ) / 2 %></td><td align="right"><%= quant.getMaxDisplayScan() %></td></tr></table></td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td colspan="2" align="center">
    <table>
        <tr>
            <td>Light to heavy ratio:</td>
            <td><strong><div id="ratio"><%= quant.getRatio() %></div></strong></td>
        </tr>
        <tr>
            <td>Heavy to light ratio:</td>
            <td><strong><div id="heavy2LightRatio"><%= quant.getHeavy2LightRatio() %></div></strong></td>
        </tr>
        <tr>
            <td>Decimal ratio:</td>
            <td><strong><div id="decimalRatio"><%= decimalRatioFormat.format(quant.getDecimalRatio()) %></div></strong></td>
        </tr>
    </table>
</td></tr>
<tr><td>&nbsp;</td></tr>
<tr><td colspan="2" align="center"><strong>Heavy</strong></td></tr>
<tr>
    <td>
        <table>
            <tr>
                <td>Selected area:</td>
                <td><div id="heavyArea"><%= format.format(quant.getHeavyArea()) %></div></td>
            </tr>
            <tr>
                <td>Scans:</td>
                <td>
                    <input type="text" onkeyup="updateRange('heavy');" name="heavyFirstScan" value="<%= quant.getHeavyFirstScan() %>" size="4"/> -
                    <input type="text" onkeyup="updateRange('heavy');" name="heavyLastScan" value="<%= quant.getHeavyLastScan() %>" size="4"/>
                </td>
            </tr>
            <tr>
                <td>Click to set:</td>
                <td>
                    <input type="radio" name="heavyBoundary" value="first" checked="true"/>First scan
                </td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input type="radio" name="heavyBoundary" value="last"/>Last scan<br/>
                </td>
            </tr>
        </table>
    </td>
    <td height="250" valign="bottom">
<%
for (int i = quant.getMinDisplayScan(); i <= quant.getMaxDisplayScan(); i++)
{
    if (ctx.getHeavyValue(i) != null)
    {
        if (i >= quant.getHeavyFirstScan() && i <= quant.getHeavyLastScan())
        {
            %><img style="border: 1px #000000 solid;" onclick="setRange('heavy', <%= i %>)" name="heavyImgScan<%= i %>" src="<%= contextPath %>/_images/red.gif" height="<%= (int)(ctx.getHeavyValue(i).floatValue() / ctx.getMaxHeavyIntensity() * 250)%>" width="5" alt="Scan <%= i %>"/><%
        }
        else
        {
            %><img style="border: 1px #000000 solid;" onclick="setRange('heavy', <%= i %>)" name="heavyImgScan<%= i %>" src="<%= contextPath %>/_images/gray.gif" height="<%= (int)(ctx.getHeavyValue(i).floatValue() / ctx.getMaxHeavyIntensity() * 250)%>" width="5" alt="Scan <%= i %>"/><%
        }
    }
    else
    {
        %><img src="<%= contextPath%>/_.gif" height="0" width="5" style="border: 1px #000000 solid;" alt="Scan <%= i %>"/><%
    }
}
%>
</td>
<td><table height="250"><tr><td valign="top" height="100%"><%= format.format(ctx.getMaxHeavyIntensity()) %></td></tr><tr><td valign="bottom">0</td></tr></table></td>
</tr>
<tr><td></td><td><table width="100%"><tr><td><%= quant.getMinDisplayScan() %></td><td align="center"><%= ( quant.getMinDisplayScan() + quant.getMaxDisplayScan() ) / 2 %></td><td align="right"><%= quant.getMaxDisplayScan() %></td></tr></table></td></tr>
</table>

<cpas:button text="Save Elution Profiles" onclick="var valuesOK = (document.elutionForm.lightFirstScan.value <= document.elutionForm.lightLastScan.value && document.elutionForm.heavyFirstScan.value <= document.elutionForm.heavyLastScan.value); if (!valuesOK) { alert('The first scan must come before the last scan.'); } return valuesOK;" />


</form>

<script type="text/javascript">
var areas = new Object();
areas.light = <%= quant.getLightArea() %>;
areas.heavy = <%= quant.getHeavyArea() %>;
var intensities = new Object();
intensities.light = new Object();
<%
    for (Quantitation.ScanInfo scanInfo : ctx.getLightElutionProfile())
    {
    %>
        intensities.light[<%= scanInfo.getScan() %>] = <%= scanInfo.getIntensity() %>;
    <%
    }
%>
intensities.heavy = new Object();
<%
    for (Quantitation.ScanInfo scanInfo : ctx.getHeavyElutionProfile())
    {
    %>
        intensities.heavy[<%= scanInfo.getScan() %>] = <%= scanInfo.getIntensity() %>;
    <%
    }
%>

function setRange(prefix, scan)
{
    if (document.elutionForm[prefix + "Boundary"][0].checked)
    {
        document.elutionForm[prefix + "FirstScan"].value = scan;
    }
    else
    {
        document.elutionForm[prefix + "LastScan"].value = scan;
    }
    updateRange(prefix);
}

function addCommas(nStr)
{
	nStr += '';
	x = nStr.split('.');
	x1 = x[0];
	x2 = x.length > 1 ? '.' + x[1] : '';
	var rgx = /(\d+)(\d{3})/;
	while (rgx.test(x1)) {
		x1 = x1.replace(rgx, '$1' + ',' + '$2');
	}
	return x1 + x2;
}

function updateRange(prefix)
{
    var firstSelected = document.elutionForm[prefix + "FirstScan"].value;
    var lastSelected = document.elutionForm[prefix + "LastScan"].value;
    var area = 0;
    for (i = <%= quant.getMinDisplayScan() %>; i <= <%= quant.getMaxDisplayScan() %>; i++)
    {
        if (document[prefix + "ImgScan" + i] != null)
        {
            if (i >= firstSelected && i <= lastSelected)
            {
                document[prefix + "ImgScan" + i].src = "<%= contextPath%>" + "/_images/red.gif";
                area += intensities[prefix][i];
            }
            else
            {
                document[prefix + "ImgScan" + i].src = "<%= contextPath%>" + "/_images/gray.gif";
            }
        }
    }

    if (area > 0)
    {
        var l = Math.pow(10, Math.round(Math.log(area) / Math.log(10)) - 4);
        area = Math.round(area / l) * l;
    }
    document.getElementById(prefix + 'Area').innerHTML = addCommas(area);
    areas[prefix] = area;
    if (areas.light > areas.heavy)
    {
        var ratioNumber = areas.heavy / areas.light;
        var heavyToLightNumber = areas.light / areas.heavy;
        document.getElementById("ratio").innerHTML = "1:" + ratioNumber.toFixed(2);
        document.getElementById("heavy2LightRatio").innerHTML = "1:" + heavyToLightNumber.toFixed(2);
    }
    else
    {
        var ratioNumber = areas.light / areas.heavy;
        var heavyToLightNumber = areas.heavy / areas.light;
        document.getElementById("ratio").innerHTML = ratioNumber.toFixed(2) + ":1";
        document.getElementById("heavy2LightRatio").innerHTML = heavyToLightNumber.toFixed(2) + ":1";
    }
    var decimalRatio = areas.light / areas.heavy;
    document.getElementById("decimalRatio").innerHTML = decimalRatio.toFixed(2);
}
</script>
</body>
</html>
