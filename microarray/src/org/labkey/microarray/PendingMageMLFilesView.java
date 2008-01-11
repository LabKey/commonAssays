package org.labkey.microarray;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpDataTable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Jan 9, 2008
 */
public class PendingMageMLFilesView extends QueryView
{
    public PendingMageMLFilesView(ViewContext context)
    {
        super(new ExpSchema(context.getUser(), context.getContainer()), createSettings(context));
        setShowExportButtons(false);
        setShowRecordSelectors(true);
        setShowDetailsColumn(false);
        setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);
        setShowCustomizeViewLinkInButtonBar(true);
    }

    private static QuerySettings createSettings(ViewContext context)
    {
        QuerySettings result = new QuerySettings(context.getActionURL(), "PendingMageMLFiles");
        result.setAllowChooseQuery(false);
        result.setQueryName(ExpSchema.TableType.Datas.name());
        return result;
    }

    protected void populateButtonBar(DataView view, ButtonBar bar)
    {
        super.populateButtonBar(view, bar);

        List<ExpProtocol> protocols = AssayService.get().getAssayProtocols(getContainer());
        List<ExpProtocol> microarrayProtocols = new ArrayList<ExpProtocol>();
        for (ExpProtocol protocol : protocols)
        {
            if (AssayService.get().getProvider(protocol) instanceof MicroarrayAssayProvider)
            {
                microarrayProtocols.add(protocol);
            }
        }

        PipeRoot root;
        try
        {
            root = PipelineService.get().findPipelineRoot(getContainer());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        if (root == null)
        {
            SimpleTextDisplayElement element = new SimpleTextDisplayElement("Unable to upload, no pipeline root has been configured", true);
            element.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(element);
        }
        else if (microarrayProtocols.size() == 0)
        {
            SimpleTextDisplayElement element = new SimpleTextDisplayElement("Unable to upload, no microarray assay definitions found", false);
            element.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(element);
        }
        else if (microarrayProtocols.size() == 1)
        {
            ExpProtocol protocol = protocols.get(0);
            ActionURL url = MicroarrayController.getUploadRedirectAction(getContainer(), protocol);
            ActionButton button = new ActionButton(url, "Upload using " + protocol.getName());
            button.setDisplayPermission(ACL.PERM_INSERT);
            button.setActionType(ActionButton.Action.POST);
            bar.add(button);
        }
        else
        {
            MenuButton menu = new MenuButton("Upload selected using...");
            menu.setDisplayPermission(ACL.PERM_INSERT);
            bar.add(menu);
            for (ExpProtocol protocol : microarrayProtocols)
            {
                ActionURL url = MicroarrayController.getUploadRedirectAction(getContainer(), protocol);
                menu.addMenuItem("Upload using " + protocol.getName(), "javascript: if (verifySelected(document.forms[\"" + view.getDataRegion().getName() + "\"], \"" + url.getLocalURIString() + "\", \"POST\", \"files\")) { document.forms[\"" + view.getDataRegion().getName() + "\"].submit(); }");
            }
        }

        ActionButton deleteButton = new ActionButton("placeholder", "Delete selected");
        ActionURL deleteURL = ExperimentService.get().getDeleteDatasURL(view.getViewContext().getContainer(), view.getViewContext().getActionURL());
        deleteButton.setScript("if (verifySelected(document.forms[\"" + view.getDataRegion().getName() + "\"], \"" + deleteURL + "\", \"post\", \"MageML files\")) {document.forms[\"" + view.getDataRegion().getName() + "\"].submit();} return false;"); 
        deleteButton.setDisplayPermission(ACL.PERM_DELETE);
        bar.add(deleteButton);
    }

    protected TableInfo createTable()
    {
        ExpDataTable table = ExperimentService.get().createDataTable("pendingFile");
        table.setRun(null);
        table.setDataType(MicroarrayModule.MAGE_ML_DATA_TYPE);
        table.populate(getSchema());
        return table;
    }

    public ExpSchema getSchema()
    {
        return (ExpSchema)super.getSchema();
    }
}
