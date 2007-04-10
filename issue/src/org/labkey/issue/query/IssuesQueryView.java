package org.labkey.issue.query;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.ACL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.common.util.Pair;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import java.sql.ResultSet;

public class IssuesQueryView extends QueryView
{
    private ViewContext _context;

    public IssuesQueryView(ViewContext context, UserSchema schema, QuerySettings settings)
    {
        super(context, schema, settings);
        _context = context;
        setShowDetailsColumn(false);
    }

    // MAB: I just want a resultset....
    public ResultSet getResultSet() throws SQLException, IOException
    {
        DataView view = createDataView();
        DataRegion rgn = view.getDataRegion();
        ResultSet rs = rgn.getResultSet(view.getRenderContext());
        return rs;
    }
    
    public UserSchema getSchema()
    {
        return super.getSchema();
    }

    protected DataView createDataView()
    {
        DataView view = super.createDataView();

        if (view.getDataRegion().getButtonBarPosition() != DataRegion.ButtonBarPosition.NONE)
        {
            ButtonBar bar = view.getDataRegion().getButtonBar(DataRegion.MODE_GRID);

            ActionButton adminButton = new ActionButton(_context.cloneViewURLHelper().setAction("admin.view").getEncodedLocalURIString(), "Admin", DataRegion.MODE_GRID, ActionButton.Action.LINK);
            adminButton.setDisplayPermission(ACL.PERM_ADMIN);
            bar.add(adminButton);

            ActionButton printButton = new ActionButton(_context.cloneViewURLHelper().addParameter("print", "1").getEncodedLocalURIString(), "Print", DataRegion.MODE_GRID, ActionButton.Action.LINK);
            bar.add(printButton);

            ActionButton prefsButton = new ActionButton(_context.cloneViewURLHelper().setAction("emailPrefs.view").getEncodedLocalURIString(), "Email Preferences", DataRegion.MODE_GRID, ActionButton.Action.LINK);
            bar.add(prefsButton);
            view.getDataRegion().setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        }
        view.getDataRegion().setShadeAlternatingRows(true);
        view.getDataRegion().setShowColumnSeparators(true);
        view.getRenderContext().setBaseSort(new Sort("-IssueId"));

        //ensureDefaultCustomViews();
        return view;
    }

    private static final String CUSTOM_VIEW_ALL = "all";
    private static final String CUSTOM_VIEW_OPEN = "open";
    private static final String CUSTOM_VIEW_RESOLVED = "resolved";

    private void ensureDefaultCustomViews()
    {
        UserSchema schema = QueryService.get().getUserSchema(null, _context.getContainer(), IssuesQuerySchema.SCHEMA_NAME);
        QueryDefinition qd = schema.getQueryDefForTable("Issues");
        Map<String, CustomView> views = qd.getCustomViews(null, _context.getRequest());

        if (!views.containsKey(CUSTOM_VIEW_ALL))
        {
            CustomView view = qd.createCustomView(null, CUSTOM_VIEW_ALL);
            List<FieldKey> columns = new ArrayList<FieldKey>();

            view.setIsHidden(true);
            columns.add(FieldKey.fromParts("IssueId"));
            columns.add(FieldKey.fromParts("Type"));
            columns.add(FieldKey.fromParts("Area"));
            columns.add(FieldKey.fromParts("Title"));
            columns.add(FieldKey.fromParts("AssignedTo"));
            columns.add(FieldKey.fromParts("Priority"));
            columns.add(FieldKey.fromParts("Status"));
            columns.add(FieldKey.fromParts("Milestone"));
            view.setColumns(columns);

            view.setFilterAndSortFromURL(new ViewURLHelper().addParameter("Issues.sort", "-Milestone,AssignedTo/DisplayName"), "Issues");
            view.save(null, null);
        }

        if (!views.containsKey(CUSTOM_VIEW_OPEN))
        {
            CustomView view = qd.createCustomView(null, CUSTOM_VIEW_OPEN);
            List<FieldKey> columns = new ArrayList<FieldKey>();

            view.setIsHidden(true);
            columns.add(FieldKey.fromParts("IssueId"));
            columns.add(FieldKey.fromParts("Type"));
            columns.add(FieldKey.fromParts("Area"));
            columns.add(FieldKey.fromParts("Title"));
            columns.add(FieldKey.fromParts("AssignedTo"));
            columns.add(FieldKey.fromParts("Priority"));
            columns.add(FieldKey.fromParts("Status"));
            columns.add(FieldKey.fromParts("Milestone"));
            view.setColumns(columns);

            view.setFilterAndSortFromURL(new ViewURLHelper().addParameter("Issues.sort", "-Milestone,AssignedTo/DisplayName").
                    addParameter("Issues.Status~eq", "open"), "Issues");
            view.save(null, null);
        }

        if (!views.containsKey(CUSTOM_VIEW_RESOLVED))
        {
            CustomView view = qd.createCustomView(null, CUSTOM_VIEW_RESOLVED);
            List<FieldKey> columns = new ArrayList<FieldKey>();

            view.setIsHidden(true);
            columns.add(FieldKey.fromParts("IssueId"));
            columns.add(FieldKey.fromParts("Type"));
            columns.add(FieldKey.fromParts("Area"));
            columns.add(FieldKey.fromParts("Title"));
            columns.add(FieldKey.fromParts("AssignedTo"));
            columns.add(FieldKey.fromParts("Priority"));
            columns.add(FieldKey.fromParts("Status"));
            columns.add(FieldKey.fromParts("Milestone"));
            view.setColumns(columns);

            view.setFilterAndSortFromURL(new ViewURLHelper().addParameter("Issues.sort", "-Milestone,AssignedTo/DisplayName").
                    addParameter("Issues.Status~eq", "resolved"), "Issues");
            view.save(null, null);
        }
    }

    public ViewURLHelper getCustomizeURL()
    {
        return urlFor(QueryAction.chooseColumns);
    }
    
    protected void renderQueryPicker(PrintWriter out)
    {
        // do nothing: we don't want a query picker for dataset views
    }

    protected void renderCustomizeLinks(PrintWriter out) throws Exception
    {
        // do nothing: we don't want a query picker for dataset views
    }
    
    protected void renderChangeViewPickers(PrintWriter out)
    {
        //do nothing: we render our own picker here...
    }

    protected ViewURLHelper urlFor(QueryAction action)
    {
        switch (action)
        {
            case exportRowsTsv:
                final ViewURLHelper url =  _context.cloneViewURLHelper().setAction("exportTsv.view");
                for (Pair<String, String> param : super.urlFor(action).getParameters())
                {
                    url.addParameter(param.getKey(), param.getValue());
                }
                return url;
        }
        return super.urlFor(action);
    }
}
