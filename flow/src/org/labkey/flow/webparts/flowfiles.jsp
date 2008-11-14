<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@page extends="org.labkey.api.jsp.JspBase" %>
<%
    ViewContext context = HttpView.currentContext();
    PipeRoot root = PipelineService.get().findPipelineRoot(context.getContainer());
    Container rootContainer = null;
    String path = null;
    if (root != null)
    {
        rootContainer = root.getContainer();
        path = rootContainer.getPath() + "/@pipeline/";
    }
%>
<script type="text/javascript">
   LABKEY.requiresClientAPI(true);
   LABKEY.requiresScript("ColumnTree.js",false);
   LABKEY.requiresScript("FileTree.js",false);
</script>
<script type="text/javascript">
Ext.onReady(function ()
{
   var tree = new LABKEY.ext.FileTree({
     id : 'tree',
     path: <%=PageFlowUtil.jsString(path)%>,
     renderTo : 'fileTree',
     title : "Files",
       autoHeight:false,
     height:400,
     width:600,
//     inputId : "???",
     dirsSelectable : false,
     browsePipeline : true,
     relativeToRoot : true,
//     fileFilter : /^.*\.xml/,
     listeners : {
//         dblclick : function (node, e) {
//             if (node.isLeaf() && !node.disabled)
//                 document.forms["importAnalysis"].submit();
//         }
     }
   });
   tree.render();
   tree.root.expand();

    // second tabs built from JS
    var tabs = new Ext.TabPanel({
        renderTo: 'fileProperties',
        activeTab: 0,
        width:300,
        height:400,
        plain:true,
        defaults:{autoScroll: true},
        items:[{
                title: 'Properties',
                html: "My content was added during construction."
            },{
                title: 'Audit',
                autoLoad:'ajax1.htm'
            },{
                title: 'Analyze',
                autoLoad: {url: 'ajax2.htm', params: 'foo=bar&wtf=1'}
            }
        ]
    });
    tabs.render();
});
</script>

<table><tr>
    <td valign="top"><div id='fileTree' class='extContainer'></div></td>
    <td valign="top"><div id="fileProperties" class='extContainer'></div></td>
</tr></table>

