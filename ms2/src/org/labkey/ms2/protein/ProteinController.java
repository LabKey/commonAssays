/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.*;
import org.labkey.api.exp.*;
import org.labkey.api.query.*;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.*;
import org.labkey.api.security.User;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

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

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new CustomProteinListView(getViewContext(), true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Custom Protein Lists");
        }
    }

    @RequiresPermission(ReadPermission.class)
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
            QuerySettings settings = schema.getSettings(getViewContext(), "CustomAnnotation");
            settings.getQueryDef(schema);
            settings.setAllowChooseQuery(true);
            settings.setAllowChooseView(true);
            _setName = settings.getQueryName();

            QueryView queryView = new QueryView(schema, settings, errors)
            {
                public DataView createDataView()
                {
                    DataView result = super.createDataView();
                    result.getRenderContext().setBaseSort(new Sort("LookupString"));
                    return result;
                }

                public MenuButton createQueryPickerButton(String label)
                {
                    return super.createQueryPickerButton("Custom Protein List");
                }
            };

            queryView.setShowExportButtons(true);
            queryView.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);

            ActionURL url;

            String header;
            if (_showSequences)
            {
                url = new ActionURL(ShowAnnotationSetAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header = "This view shows your protein list with all the proteins that match. If more than one sequence matches you will get multiple rows. " + PageFlowUtil.textLink("show without proteins", url.getLocalURIString());
            }
            else
            {
                url = new ActionURL(ShowAnnotationSetWithSequencesAction.class, getContainer());
                url.addParameter("CustomAnnotation.queryName", settings.getQueryName());
                header = "This view shows just the data uploaded as part of the list. " + PageFlowUtil.textLink("show with matching proteins loaded into this server", url.getLocalURIString());
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

    @RequiresPermission(ReadPermission.class)
    public class ShowAnnotationSetWithSequencesAction extends ShowSetAction
    {
        public ShowAnnotationSetWithSequencesAction()
        {
            super(true);
        }
    }

    @RequiresPermission(DeletePermission.class)
    public class DeleteCustomAnnotationSetsAction extends FormHandlerAction
    {
        public void validateCommand(Object target, Errors errors)
        {
        }

        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            Set<Integer> setIds = DataRegionSelection.getSelectedIntegers(getViewContext(), true);
            for (Integer id : setIds)
            {
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

    @RequiresPermission(InsertPermission.class)
    public class UploadCustomProteinAnnotations extends FormViewAction<UploadAnnotationsForm>
    {
        public void validateCommand(UploadAnnotationsForm target, Errors errors)
        {
        }

        public ModelAndView getView(UploadAnnotationsForm form, boolean reshow, BindException errors) throws Exception
        {
            return new JspView<>("/org/labkey/ms2/protein/uploadCustomProteinAnnotations.jsp", form, errors);
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

            List<Map<String, Object>> rows = tabLoader.load();
            ColumnDescriptor[] columns = tabLoader.getColumns();
            String lookupStringColumnName = null;

            CustomAnnotationType type = CustomAnnotationType.valueOf(form.getAnnotationType());

            if (rows.size() < 1)
            {
                errors.addError(new ObjectError("main", null, null, "Your protein list must have at least one protein, plus the header line"));
            }
            else
            {
                Set<String> columnNames = new CaseInsensitiveHashSet();
                for (ColumnDescriptor column : columns)
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

            try (DbScope.Transaction transaction = scope.ensureTransaction())
            {
                Connection connection = transaction.getConnection();
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
                annotationSet = Table.update(getUser(), ProteinManager.getTableInfoCustomAnnotationSet(), annotationSet, annotationSet.getCustomAnnotationSetId());

                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ");
                sb.append(ProteinManager.getTableInfoCustomAnnotation());
                sb.append("(CustomAnnotationSetId, LookupString, ObjectURI) VALUES (?, ?, ?)");

                PreparedStatement stmt = connection.prepareStatement(sb.toString());
                stmt.setInt(1, annotationSet.getCustomAnnotationSetId());

                List<PropertyDescriptor> descriptors = new ArrayList<>();

                for (int i = 1; i < columns.length; i++)
                {
                    ColumnDescriptor cd = columns[i];
                    PropertyDescriptor pd = new PropertyDescriptor();
                    DomainDescriptor dd = new DomainDescriptor.Builder(annotationSet.getLsid(), getContainer()).build();

                    //todo :  name for domain?
                    pd.setName(cd.name);
                    String legalName = ColumnInfo.legalNameFromName(cd.name);
                    String propertyURI = annotationSet.getLsid() + "#" + legalName;
                    pd.setPropertyURI(propertyURI);
                    pd.setRangeURI(PropertyType.getFromClass(cd.clazz).getTypeUri());
                    pd.setContainer(getContainer());
                    //Change name to be fully qualified string for property
                    pd = OntologyManager.insertOrUpdatePropertyDescriptor(pd, dd, i - 1);

                    cd.name = pd.getPropertyURI();
                    descriptors.add(pd);
                }

                rows = tabLoader.load();

                int ownerObjectId = OntologyManager.ensureObject(getContainer(), annotationSet.getLsid());
                OntologyManager.ImportHelper helper = new CustomAnnotationImportHelper(stmt, connection, annotationSet.getLsid(), lookupStringColumnName);

                OntologyManager.insertTabDelimited(getContainer(), getUser(), ownerObjectId, helper, descriptors, rows, false);

                stmt.executeBatch();
                connection.commit();

                transaction.commit();
            }
            catch (ValidationException ve)
            {
                for (ValidationError error : ve.getErrors())
                    errors.reject(SpringActionController.ERROR_MSG, PageFlowUtil.filter(error.getMessage()));
                return false;
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
