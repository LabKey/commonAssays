package org.labkey.flow.webparts;

import org.labkey.flow.view.FlowQueryView;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowQuerySettings;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.util.PFUtil;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.security.ACL;

import java.util.List;
import java.util.ArrayList;

public class AnalysisScriptsWebPart extends FlowQueryView
{
    static public final WebPartFactory FACTORY = new WebPartFactory("Flow Analysis Scripts")
    {
        public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart wp) throws Exception
        {
            FlowSchema schema = new FlowSchema(portalCtx.getUser(), portalCtx.getContainer());
            FlowQuerySettings settings = schema.getSettings(wp, portalCtx.getViewURLHelper());
            settings.setAllowChooseQuery(false);
            settings.setAllowChooseView(false);
            settings.setQueryName(FlowTableType.AnalysisScripts.toString());
            return new AnalysisScriptsWebPart(new ViewContext(portalCtx), schema, settings);
        }
    };

    public AnalysisScriptsWebPart(ViewContext context, FlowSchema schema, FlowQuerySettings settings)
    {
        super(context, schema, settings);
        setTitle("Flow Analysis Scripts");
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
    }

    public List<DisplayColumn> getDisplayColumns()
    {
        List<DisplayColumn> ret = new ArrayList();
        TableInfo table = getTable();
        List<ColumnInfo> columns = getQueryDef().getColumns(null, table);
        for (ColumnInfo col : columns)
        {
            ret.add(col.getRenderer());
        }
        ColumnInfo colScriptType = new AliasedColumn("Type", table.getColumn("RowId"));
        ret.add(new AnalysisScriptTypeColumn(colScriptType));
        return ret;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        if (!getViewContext().hasPermission(ACL.PERM_UPDATE))
            return;
        ActionButton btnNewScript = new ActionButton("Create New Analysis Script", PFUtil.urlFor(ScriptController.Action.newProtocol, getContainer()));
        bar.add(btnNewScript);
        return;
    }

    protected DataRegion createDataRegion()
    {
        DataRegion ret = super.createDataRegion();
        ret.setFixedWidthColumns(false);
        return ret;
    }
}
