package org.labkey.ms2.protein;

import org.labkey.api.view.*;
import org.labkey.api.data.*;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.exp.*;
import org.labkey.api.query.*;
import org.labkey.api.util.CaseInsensitiveHashSet;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.common.tools.TabLoader;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.labkey.ms2.MS2Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public class ProteinController extends SpringActionController
{
    private static DefaultActionResolver _actionResolver = new DefaultActionResolver(ProteinController.class);
    
    public ProteinController()
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            DataRegion rgn = new DataRegion();
            rgn.setColumns(ProteinManager.getTableInfoCustomAnnotationSet().getColumns("Name, Created, CreatedBy, CustomAnnotationSetId"));
            rgn.getDisplayColumn("Name").setURL("showAnnotationSet.view?CustomAnnotation.queryName=${Name}");
            rgn.getDisplayColumn("CustomAnnotationSetId").setVisible(false);
            GridView gridView = new GridView(rgn);
            rgn.setShowRecordSelectors(getContainer().hasPermission(getUser(), ACL.PERM_INSERT) || getContainer().hasPermission(getUser(), ACL.PERM_DELETE));
            gridView.setSort(new Sort("Name"));

            ButtonBar buttonBar = new ButtonBar();

            ActionButton deleteButton = new ActionButton("", "Delete");
            ActionURL deleteURL = new ActionURL(DeleteCustomAnnotationSetsAction.class, getContainer());
            deleteButton.setScript("return verifySelected(this.form, \"" + deleteURL.getLocalURIString() + "\", \"post\", \"Custom Protein Lists\")");
            deleteButton.setActionType(ActionButton.Action.POST);
            deleteButton.setDisplayPermission(ACL.PERM_DELETE);
            buttonBar.add(deleteButton);

            ActionButton addButton = new ActionButton(new ActionURL(UploadCustomProteinAnnotations.class, getContainer()), "Import Custom Protein List");
            addButton.setDisplayPermission(ACL.PERM_INSERT);
            addButton.setActionType(ActionButton.Action.LINK);
            buttonBar.add(addButton);

            rgn.setButtonBar(buttonBar);

            VBox box = new VBox();

            if (!getContainer().isProject())
            {
                ActionURL link = getBeginURL(getContainer().getProject());
                HtmlView noteView = new HtmlView("This list only shows protein lists that have been loaded into this folder. When constructing queries, <a href=\"" + link + "\">annotations in the project</a> are visible from all the folders in that project.");
                box.addView(noteView);
            }
            box.addView(gridView);
            return box;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Custom Protein Lists");
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowAnnotationSetAction extends ShowSetAction
    {
        public ShowAnnotationSetAction()
        {
            super(false);
        }
    }

    public abstract class ShowSetAction extends SimpleViewAction
    {
        private final boolean _showSequences;
        private String _setName;

        protected ShowSetAction(boolean showSequences)
        {
            _showSequences = showSequences;
        }

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            UserSchema schema = new CustomAnnotationSchema(getUser(), getContainer(), _showSequences);
            QuerySettings settings = new QuerySettings(getViewContext().getActionURL(), "CustomAnnotation");
            settings.setSchemaName(schema.getSchemaName());
            settings.getQueryDef(schema);
            settings.setAllowChooseQuery(true);
            settings.setAllowChooseView(true);
            _setName = settings.getQueryName();

            QueryView queryView = new QueryView(schema, settings)
            {
                protected DataView createDataView()
                {
                    DataView result = super.createDataView();
                    result.getRenderContext().setBaseSort(new Sort("LookupString"));
                    return result;
                }

                protected List<QueryPicker> getQueryPickers()
                {
                    List<QueryPicker> result = super.getQueryPickers();
                    for (QueryPicker picker : result)
                    {
                        picker.setLabel("Custom Protein List: ");
                    }
                    return result;
                }
            };

            queryView.setShowExportButtons(true);
            queryView.setShowCustomizeViewLinkInButtonBar(true);
            queryView.setButtonBarPosition(DataRegion.ButtonBarPosition.BOTTOM);

            ActionURL url;

            String header;
            if (_showSequences)
            {
                url = new ActionURL(ShowAnnotationSetAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header = "This view shows your protein list with all the proteins that match. If more than one sequence matches you will get multiple rows. [<a href=\"" + url.getLocalURIString() + "\">show without proteins</a>]";
            }
            else
            {
                url = new ActionURL(ShowAnnotationSetWithSequencesAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header = "This view shows just the data uploaded as part of the list. [<a href=\"" + url.getLocalURIString() + "\">show with matching proteins loaded into this server</a>]";
            }

            HtmlView linkView = new HtmlView(header);

            return new VBox(linkView, queryView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("MS2", MS2Controller.getBeginURL(getContainer()));
            root.addChild("Custom Protein Lists", getBeginURL(getContainer()));
            root.addChild("Custom Protein List: " + _setName);
            return root;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowAnnotationSetWithSequencesAction extends ShowSetAction
    {
        public ShowAnnotationSetWithSequencesAction()
        {
            super(true);
        }
    }

    @RequiresPermission(ACL.PERM_DELETE)
    public class DeleteCustomAnnotationSetsAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            Set<String> setIds = DataRegionSelection.getSelected(getViewContext(), true);
            for (String setId : setIds)
            {
                int id = Integer.parseInt(setId);
                CustomAnnotationSet set = ProteinManager.getCustomAnnotationSet(getContainer(), id, false);
                if (set != null)
                {
                    ProteinManager.deleteCustomAnnotationSet(set);
                }
            }
            return true;
        }

        public ActionURL getSuccessURL(Object o)
        {
            return getBeginURL(getContainer());
        }
    }

    public static ActionURL getBeginURL(Container c)
    {
        return new ActionURL(BeginAction.class, c);
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class UploadCustomProteinAnnotations extends FormViewAction<UploadAnnotationsForm>
    {
        public void validateCommand(UploadAnnotationsForm target, Errors errors)
        {
        }

        public ModelAndView getView(UploadAnnotationsForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<UploadAnnotationsForm>("/org/labkey/ms2/protein/uploadCustomProteinAnnotations.jsp", form, errors);
        }

        public boolean handlePost(UploadAnnotationsForm form, BindException errors) throws Exception
        {
            if (form.getName().length() == 0)
            {
                errors.addError(new ObjectError("main", null, null, "You must enter a name for the protein list."));
            }
            Map<String, CustomAnnotationSet> sets = ProteinManager.getCustomAnnotationSets(getContainer(), false);
            CaseInsensitiveHashSet names = new CaseInsensitiveHashSet(sets.keySet());
            if (names.contains(form.getName()))
            {
                errors.addError(new ObjectError("main", null, null, "There is already an protein list with the name '" + form.getName() + "' loaded."));
            }

            TabLoader tabLoader = new TabLoader(form.getAnnotationsText(), true);
            tabLoader.setScanAheadLineCount(200);

            Map<String, Object>[] rows = (Map<String, Object>[]) tabLoader.load();
            TabLoader.ColumnDescriptor[] columns = tabLoader.getColumns();
            String lookupStringColumnName = null;

            CustomAnnotationType type = CustomAnnotationType.valueOf(form.getAnnotationType());

            if (rows.length < 1)
            {
                errors.addError(new ObjectError("main", null, null, "Your protein list must have at least one protein, plus the header line"));
            }
            else
            {
                Set<String> columnNames = new CaseInsensitiveHashSet();
                for (TabLoader.ColumnDescriptor column : columns)
                {
                    if (!columnNames.add(column.name))
                    {
                        errors.addError(new ObjectError("main", null, null, "Duplicate column name: " + column.name));
                    }
                }

                lookupStringColumnName = columns[0].name;

                Set<String> lookupStrings = new CaseInsensitiveHashSet();
                for (Map<String, Object> row : rows)
                {
                    String lookupString = CustomAnnotationImportHelper.convertLookup(row.get(lookupStringColumnName));
                    if (lookupString == null || lookupString.length() == 0)
                    {
                        errors.addError(new ObjectError("main", null, null, "All rows must contain a protein identifier."));
                        break;
                    }

                    String error = type.validateUserLookupString(lookupString);
                    if (error != null)
                    {
                        errors.addError(new ObjectError("main", null, null, error));
                        break;
                    }

                    if (!lookupStrings.add(lookupString))
                    {
                        errors.addError(new ObjectError("main", null, null, "The input contains multiple entries for the protein " + lookupString));
                        break;
                    }
                    row.put(lookupStringColumnName, lookupString);
                    for (Object o : row.values())
                    {
                        if (o != null && o.toString().length() >= 4000)
                        {
                            errors.addError(new ObjectError("main", null, null, "The input contains a value that is more than 4000 characters long, which is the limit for a single value"));
                            break;
                        }
                    }
                }
            }

            if (errors.getErrorCount() > 0)
            {
                return false;
            }

            DbScope scope = ProteinManager.getSchema().getScope();
            Connection connection = scope.beginTransaction();
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

                rows = (Map<String, Object>[])tabLoader.load();

                int ownerObjectId = OntologyManager.ensureObject(getContainer().getId(), annotationSet.getLsid());
                OntologyManager.ImportHelper helper = new CustomAnnotationImportHelper(stmt, connection, annotationSet.getLsid(), lookupStringColumnName);

                OntologyManager.insertTabDelimited(getContainer(), ownerObjectId, helper, descriptors.toArray(new PropertyDescriptor[0]), rows, false);

                stmt.executeBatch();
                connection.commit();

                scope.commitTransaction();
            }
            finally
            {
                scope.closeConnection();
            }
            return true;
        }

        public ActionURL getSuccessURL(UploadAnnotationsForm uploadAnnotationsForm)
        {
            return getBeginURL(getContainer());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            root.addChild("MS2", MS2Controller.getBeginURL(getContainer()));
            root.addChild("Custom Protein Lists", getBeginURL(getContainer()));
            root.addChild("Upload Custom Protein List");
            return root;
        }
    }

    public static class UploadAnnotationsForm
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

}
