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
     renderTo : 'tree',
     title : "Files",
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
});
</script>

<div id='tree' class='extContainer'></div>
