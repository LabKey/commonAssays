package org.labkey.ms2.protein;

import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.FormData;
import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.exp.*;
import org.labkey.api.query.*;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.common.tools.TabLoader;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class ProteinController extends ViewController
{

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward begin() throws Exception
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(ProteinManager.getTableInfoCustomAnnotationSet().getColumns("Name, Created, CreatedBy, CustomAnnotationSetId"));
        rgn.getDisplayColumn("Name").setURL("showAnnotationSet.view?CustomAnnotation.queryName=${Name}");
        rgn.getDisplayColumn("CustomAnnotationSetId").setVisible(false);
        GridView gridView = new GridView(rgn);
        rgn.setShowRecordSelectors(getContainer().hasPermission(getUser(), ACL.PERM_INSERT) || getContainer().hasPermission(getUser(), ACL.PERM_DELETE));
        gridView.setSort(new Sort("Name"));

        ButtonBar buttonBar = new ButtonBar();

        ActionButton addButton = new ActionButton("showUploadCustomProteinAnnotations.view", "Upload annotations");
        addButton.setDisplayPermission(ACL.PERM_INSERT);
        buttonBar.add(addButton);

        ActionButton deleteButton = new ActionButton("", "Delete selected");
        ViewURLHelper deleteURL = new ViewURLHelper("protein", "deleteCustomAnnotationSets.view", getContainer());
        deleteButton.setScript("return verifySelected(this.form, \"" + deleteURL.getLocalURIString() + "\", \"post\", \"Custom Annotation Sets\")");
        deleteButton.setActionType(ActionButton.Action.POST);
        deleteButton.setDisplayPermission(ACL.PERM_DELETE);
        buttonBar.add(deleteButton);

        rgn.setButtonBar(buttonBar);

        VBox box = new VBox();

        if (!getContainer().isProject())
        {
            ViewURLHelper link = cloneViewURLHelper();
            link.setExtraPath(getContainer().getProject().getPath());
            HtmlView noteView = new HtmlView("This list only shows annotation sets that have been loaded into this folder. When constructing queries, <a href=\"" + link + "\">annotations in the project</a> are visible from all the folders in that project.");
            box.addView(noteView);
        }
        box.addView(gridView);

        return renderInTemplate(box, getContainer(), "Custom Protein Annotations");
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward showAnnotationSet() throws Exception
    {
        return showSet(false);
    }

    private Forward showSet(boolean showSequences)
        throws Exception
    {
        UserSchema schema = new CustomAnnotationSchema(getUser(), getContainer(), showSequences);
        QuerySettings settings = new QuerySettings(getViewURLHelper(), getRequest(), "CustomAnnotation");

        ViewURLHelper url = cloneViewURLHelper();
        url.deleteParameters();
        if (showSequences)
        {
            url.setAction("showAnnotationSet.view");
            url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
        }
        else
        {
            url.setAction("showAnnotationSetWithSequences.view");
            url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
        }

        settings.getQueryDef(schema);
        settings.setAllowChooseQuery(true);
        settings.setAllowChooseView(true);

        QueryView queryView = new QueryView(getViewContext(), schema, settings);
        queryView.setShowExportButtons(true);
        queryView.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);

        HtmlView linkView = new HtmlView("[<a href=\"" + url.getLocalURIString() + "\">show with" + (showSequences ? "out" : "") + " proteins</a>]");
        
        VBox box = new VBox(linkView, queryView);

        return renderInTemplate(box, getContainer(), "Custom Annotation Set");
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward showAnnotationSetWithSequences() throws Exception
    {
        return showSet(true);
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_DELETE)
    protected Forward deleteCustomAnnotationSets() throws Exception
    {
        HttpServletRequest request = getRequest();
        String[] setIds = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
        for (String setId : setIds)
        {
            int id = Integer.parseInt(setId);
            CustomAnnotationSet set = ProteinManager.getCustomAnnotationSet(getContainer(), id, false);
            if (set != null)
            {
                ProteinManager.deleteCustomAnnotationSet(set);
            }
        }
        return new ViewForward("protein", "begin", getContainer());
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    protected Forward showUploadCustomProteinAnnotations(UploadAnnotationsForm form) throws Exception
    {
        JspView<UploadAnnotationsForm> view = new JspView<UploadAnnotationsForm>("/org/labkey/ms2/protein/uploadCustomProteinAnnotations.jsp", form);
        return renderInTemplate(view, getContainer(), "Upload Custom Protein Annotations");
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    protected Forward uploadCustomProteinAnnotations(UploadAnnotationsForm form) throws Exception
    {
        if (form.getName().length() == 0)
        {
            addError("You must enter a name for the annotation set.");
        }
        Map<String, CustomAnnotationSet> sets = ProteinManager.getCustomAnnotationSets(getContainer(), false);
        CaseInsensitiveHashSet names = new CaseInsensitiveHashSet(sets.keySet());
        if (names.contains(form.getName()))
        {
            addError("There is already an annotation set with the name '" + form.getName() + "' loaded.");
        }

        TabLoader tabLoader = new TabLoader(form.getAnnotationsText(), true);
        tabLoader.setScanAheadLineCount(200);

        Map<String, Object>[] rows = (Map<String, Object>[]) tabLoader.load();

        if (rows.length < 1)
        {
            addError("Your annotation set must have at least one annotation, plus the header line");
        }

        if (!PageFlowUtil.getActionErrors(getRequest(), true).isEmpty())
        {
            return showUploadCustomProteinAnnotations(form);
        }

        DbScope scope = ProteinManager.getSchema().getScope();
        Connection connection = scope.beginTransaction();
        CustomAnnotationType type = CustomAnnotationType.valueOf(form.getAnnotationType());
        try
        {
            CustomAnnotationSet annotationSet = new CustomAnnotationSet();
            Date modified = new Date();
            annotationSet.setModified(modified);
            annotationSet.setCreated(modified);
            User user = getUser();
            annotationSet.setCreatedBy(user.getUserId());
            annotationSet.setModifiedBy(user.getUserId());
            annotationSet.setContainer(getContainer().getId());
            annotationSet.setName(form.getName());
            annotationSet.setCustomAnnotationType(type.toString());

            annotationSet = Table.insert(getUser(), ProteinManager.getTableInfoCustomAnnotationSet(), annotationSet);
            annotationSet.setLsid(new Lsid(CustomAnnotationSet.TYPE, Integer.toString(annotationSet.getCustomAnnotationSetId())).toString());
            annotationSet = Table.update(getUser(), ProteinManager.getTableInfoCustomAnnotationSet(), annotationSet, annotationSet.getCustomAnnotationSetId(), null);

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ");
            sb.append(ProteinManager.getTableInfoCustomAnnotation());
            sb.append("(CustomAnnotationSetId, LookupString, ObjectURI) VALUES (?, ?, ?)");

            PreparedStatement stmt = connection.prepareStatement(sb.toString());
            stmt.setInt(1, annotationSet.getCustomAnnotationSetId());

            TabLoader.ColumnDescriptor[] columns = tabLoader.getColumns();
            String lookupStringColumnName = columns[0].name;

            List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();

            for (int i = 1; i < columns.length; i++)
            {
                TabLoader.ColumnDescriptor cd = columns[i];
                PropertyDescriptor pd = new PropertyDescriptor();
                DomainDescriptor dd = new DomainDescriptor();
                dd.setDomainURI(annotationSet.getLsid());
                dd.setContainer(getContainer());
                //todo :  name for domain?
                pd.setName(cd.name);
                String legalName = ColumnInfo.legalNameFromName(cd.name);
                String propertyURI = annotationSet.getLsid() + "#" + legalName;
                pd.setPropertyURI(propertyURI);
                pd.setRangeURI(PropertyType.getFromClass(cd.clazz).getTypeUri());
                pd.setContainer(getContainer());
                //Change name to be fully qualified string for property
                pd = OntologyManager.insertOrUpdatePropertyDescriptor(pd, dd);

                cd.name = pd.getPropertyURI();
                descriptors.add(pd);
            }

            int ownerObjectId = OntologyManager.ensureObject(getContainer().getId(), annotationSet.getLsid());
            OntologyManager.ImportHelper helper = new ImportHelper(stmt, connection, annotationSet.getLsid(), lookupStringColumnName);

            OntologyManager.insertTabDelimited(getContainer(), ownerObjectId, helper, descriptors.toArray(new PropertyDescriptor[0]), rows, false);

            stmt.executeBatch();
            connection.commit();

            scope.commitTransaction();
        }
        finally
        {
            scope.closeConnection();
        }

        return new ViewForward("protein", "begin", getContainer());
    }

    public static class UploadAnnotationsForm extends FormData
    {
        private String _name;
        private String _annotationsText;
        private String _annotationType;

        public String getName()
        {
            if (_name == null)
            {
                return "";
            }
            return _name;
        }

        public void setName(String name)
        {
            if (name != null)
            {
                name = name.trim();
            }
            _name = name;
        }

        public String getAnnotationsText()
        {
            if (_annotationsText == null)
            {
                return "";
            }
            return _annotationsText;
        }

        public void setAnnotationsText(String annotationsText)
        {
            if (annotationsText != null)
            {
                annotationsText = annotationsText.trim();
            }
            _annotationsText = annotationsText;
        }

        public String getAnnotationType()
        {
            return _annotationType;
        }

        public void setAnnotationType(String annotationType)
        {
            _annotationType = annotationType;
        }
    }

    class ImportHelper implements OntologyManager.ImportHelper
    {
        private final String _setLsid;
        private final String _lookupStringColumnName;
        private final PreparedStatement _stmt;
        private final Connection _conn;
        private Set<String> _lookupStrings = new HashSet<String>();
        private int _count;

        ImportHelper(PreparedStatement stmt, Connection conn, String lsid, String lookupStringColumnName)
        {
            _stmt = stmt;
            _conn = conn;
            _setLsid = lsid;
            _lookupStringColumnName = lookupStringColumnName;
        }

        public String beforeImportObject(Map map) throws SQLException
        {
            String lookupString = (String)map.get(_lookupStringColumnName);
            if (!_lookupStrings.add(lookupString))
            {
                throw new SQLException("The input contains multiple entries for the protein " + lookupString);
            }

            String lsid = new Lsid(_setLsid + "-" + lookupString).toString();

            _stmt.setString(2, lookupString);
            _stmt.setString(3, lsid);
            _stmt.addBatch();
            _count++;
            if (_count % 5000 == 0)
            {
                _stmt.executeBatch();
                _conn.commit();
            }

            return lsid;
        }

        public void afterImportObject(String lsid, ObjectProperty[] props) throws SQLException
        {
        }
    }
}
