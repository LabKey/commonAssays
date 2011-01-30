<%
/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.ObjectProperty"%>
<%@ page import="org.labkey.api.exp.property.DomainProperty"%>
<%@ page import="org.labkey.api.study.PlateTemplate" %>
<%@ page import="org.labkey.api.study.Position" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.elispot.ElispotController" %>
<%@ page import="org.labkey.elispot.ElispotDataHandler" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.elispot.ElispotAssayProvider" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<ElispotController.PlateSummaryBean> me = (JspView<ElispotController.PlateSummaryBean>)HttpView.currentView();
    ElispotController.PlateSummaryBean bean = me.getModelBean();
    PlateTemplate template = bean.getTemplate();

    // map of sample group names to cell id's
    Map<String, String> sampleMap = new java.util.TreeMap<String, String>();
    Map<String, String> antigenMap = new java.util.TreeMap<String, String>();
    
    for (int row=0; row < template.getRows(); row++)
    {
        for (int col=0; col < template.getColumns(); col++)
        {
            Position pos = template.getPosition(row, col);
            ElispotController.WellInfo info = bean.getWellInfoMap().get(pos);

            ObjectProperty prop = info.getWellProperties().get(ElispotDataHandler.WELLGROUP_PROPERTY_NAME);
            if (prop != null)
            {
                String key = prop.getStringValue();
                if (!sampleMap.containsKey(key))
                {
                    sampleMap.put(key, getSampleClass(prop));
                }
            }

            ObjectProperty antigenGroup = info.getWellProperties().get(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME);
            ObjectProperty antigen = info.getWellProperties().get(org.labkey.elispot.ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME);
            if (antigenGroup != null)
            {
                StringBuilder sb = new StringBuilder();

                sb.append(antigenGroup.getStringValue());
                if (antigen != null && antigen.value() != null)
                {
                    sb.append(" (").append(antigen.getStringValue()).append(")");
                }
                if (!antigenMap.containsKey(sb.toString()))
                {
                    antigenMap.put(sb.toString(), getAntigenClass(antigenGroup));
                }
            }
        }
    }
%>

<script type="text/javascript">

    var sampleGroups = [];
    var antigenGroups = [];
<%
    for (Map.Entry<String, String> entry : sampleMap.entrySet()) {
%>
        sampleGroups.push({
            name: '<%=entry.getKey()%>',
            cls: '<%=entry.getValue()%>'
        });
<%
    }

    for (Map.Entry<String, String> entry : antigenMap.entrySet()) {
%>
        antigenGroups.push({
            name: '<%=entry.getKey()%>',
            cls: '<%=entry.getValue()%>'
        });
<%
    }
%>
    Ext.onReady(function()
    {
        var showSample = function(cls, hilight){
            var sample = Ext.select('.' + cls, true);
            if (sample)
            {
                sample.applyStyles({backgroundColor: hilight ? '#126495' : '#AAAAAA'});
                sample.repaint();                
            }
        };

        var items = [];

        var fieldLabel = 'Sample Well Groups';
        for (i=0; i < sampleGroups.length; i++)
        {
            var group = sampleGroups[i];

            items.push({xtype:'radio', name:'sampleGroup',
                boxLabel: group.name,
                fieldLabel: fieldLabel,
                sampleCls: group.cls,
                handler: function(cmp, checked){showSample(cmp.initialConfig.sampleCls, checked);},
                scope: this
            });
            fieldLabel = '';
        }

        fieldLabel = 'Antigen Well Groups';
        for (var i=0; i < antigenGroups.length; i++)
        {
            group = antigenGroups[i];

            items.push({xtype:'radio', name:'sampleGroup',
                fieldLabel: fieldLabel,
                boxLabel: group.name,
                antigenCls: group.cls,
                handler: function(cmp, checked){showSample(cmp.initialConfig.antigenCls, checked);},
                scope: this
            });
            fieldLabel = '';
        }

        var form = new Ext.form.FormPanel({
            border: false,
            labelWidth: 125,
            items: items
        });

        var panel = new Ext.Panel({
            border: false,
            padding: 20,
            renderTo: 'wellGroup-form',
            items:[
                {html:'<span>Click on a button to highlight the wells in a particular well group.<br>Hover over an individual well ' +
                        'to display a tooltip with additional details.</span>', border: false},
                {html:'&nbsp;', border:false},
                form
            ]
        });

        var rowLabel = ['A','B','C','D','E','F','G','H'];
        // set up the tooltips
        for (var row=0; row < 8; row++)
        {
            for (var col=0; col < 12; col++)
            {
                var target = 'well_' + row + '_' + col;
                var contentId = 'wellInfo_' + row + '_' + col;
                var title = 'Well Detail : (' + rowLabel[row] + ', ' + (col+1) + ')';

                new Ext.ToolTip({
                        title: title,
                        target: target,
                        anchor: 'left',
                        html: null,
                        width: 425,
                        closable: true,
                        contentEl: contentId
                });
            }
        }
    });
</script>

<table>
<%--
    <tr class="labkey-wp-header"><th colspan=50 align=center>Plate Summary Information</th></tr>
--%>
    <tr><td class="labkey-announcement-title" colspan="2"><span>Plate Summary Information</span></td></tr>
    <tr><td class="labkey-title-area-line" colspan="2"></td></tr>
    <tr><td>
        <table>
        <%
            out.print("<tr>");
            out.print("<td><div style=\"width:45px; height:35px; text-align:center;\"></div></td>");
            for (int col=0; col < template.getColumns(); col++)
            {
                out.print("<td><div style=\"width:45px; height:35px; text-align:center;\">" + (col + 1) + "</div></td>");
            }
            out.print("</tr>");

            char rowLabel = 'A';
            for (int row=0; row < template.getRows(); row++){
        %>
            <tr>
            <%
                out.println("<td><div style=\"width:35px; height:25px; text-align:center;\">" + rowLabel++ + "</div></td>");
                for (int col=0; col < template.getColumns(); col++)
                {
                    Position pos = template.getPosition(row, col);
                    ElispotController.WellInfo info = bean.getWellInfoMap().get(pos);
                    ObjectProperty prop = info.getWellProperties().get("WellgroupName");
                    ObjectProperty antigen = info.getWellProperties().get(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME);
            %>
                    <td><div class="<%=getSampleClass(prop)%> <%=getAntigenClass(antigen)%>" style="border:1px solid gray;
                            width:45px; height:35px; vertical-align:middle; text-align:center; background-color:#AAAAAA;"><br/>
                            <a id="<%=getId("well_", pos)%>" style="color: white;" href="javascript:void(0);"><%=info.getTitle()%></a>
                        </div>
                    </td>
            <%
                }
            %>
            </tr>
        <%
            }
        %>
        </table>
    </td><td><div id="wellGroup-form"></div></td>
    </tr>
</table>

<div id="wellDetailsDiv" style="display:none;">
<%
    for (int row=0; row < template.getRows(); row++)
    {
        for (int col=0; col < template.getColumns(); col++)
        {
            Position pos = template.getPosition(row, col);
            ElispotController.WellInfo info = bean.getWellInfoMap().get(pos);
            String id = getId("wellInfo_", pos);
            %>
            <table id="<%=id%>">
            <%
            for (ObjectProperty op : info.getWellProperties().values())
            {
                if (ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY.equalsIgnoreCase(op.getName()))
                    continue;
            %>
                <tr><td><%=op.getName()%></td><td><%=String.valueOf(op.value())%></td></tr>
            <%
            }
            for (Map.Entry<DomainProperty, String> entry : info.getSpecimenProperties().entrySet())
            {
            %>
<%--
                <tr><td><%=entry.getKey().getName()%></td><td><%=entry.getValue()%></td></tr>
--%>
            <%
            }
%>
            </table>
<%
        }
    }
%>
</div>

<%!
    String getId(String prefix, Position pos)
    {
        return (prefix + pos.getRow() + "_" + pos.getColumn());
    }

    String getSampleClass(ObjectProperty prop)
    {
        if (prop != null)
            return "lk_sample_" + prop.getStringValue().replaceAll("\\s", "_");
        return "";
    }

    String getAntigenClass(ObjectProperty prop)
    {
        if (prop != null)
            return "lk_antigen_" + prop.getStringValue().replaceAll("\\s", "_");
        return "";
    }
%>
