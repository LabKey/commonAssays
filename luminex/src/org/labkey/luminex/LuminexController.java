/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.luminex;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TSVWriter;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.form.DeleteForm;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.AbstractQueryImportAction;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.actions.BaseAssayAction;
import org.labkey.api.study.actions.ProtocolIdForm;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayView;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.permissions.DesignAssayPermission;
import org.labkey.api.util.FileStream;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.HttpView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.WebPartView;
import org.labkey.luminex.model.GuideSet;
import org.labkey.luminex.query.LuminexProtocolSchema;
import org.labkey.luminex.AnalyteDefaultValueService.AnalyteDefaultTransformer;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Actions for Luminex specific features (Levey-Jennings, QC Report, Excluded Data)
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class LuminexController extends SpringActionController
{
    private static final Logger _log = Logger.getLogger(LuminexController.class);

    private static final DefaultActionResolver _resolver = new DefaultActionResolver(LuminexController.class,
            LuminexUploadWizardAction.class
    );

    public LuminexController()
    {
        super();
        setActionResolver(_resolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ExcludedDataAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();
            AssayProvider provider = form.getProvider();
            if (!(provider instanceof LuminexAssayProvider))
                throw new ProtocolIdForm.ProviderNotFoundException("Luminex assay provider not found", _protocol);

            AssayView result = new AssayView();
            LuminexProtocolSchema schema = new LuminexProtocolSchema(getUser(), getContainer(), (LuminexAssayProvider)provider, _protocol, null);

            String queryName = LuminexProtocolSchema.RUN_EXCLUSION_TABLE_NAME;
            result.setupViews(getExcludedQueryView(schema, queryName, "Analyte", errors), false, form.getProvider(), form.getProtocol());

            queryName = LuminexProtocolSchema.TITRATION_EXCLUSION_TABLE_NAME;
            result.addView(getExcludedQueryView(schema, queryName, "Titration", errors));

            queryName = LuminexProtocolSchema.SINGLEPOINT_UNKNOWN_EXCLUSION_TABLE_NAME;
            result.addView(getExcludedQueryView(schema, queryName, "Singlepoint Unknown", errors));

            queryName = LuminexProtocolSchema.WELL_EXCLUSION_TABLE_NAME;
            result.addView(getExcludedQueryView(schema, queryName, "Well", errors));

            setHelpTopic(new HelpTopic("excludeAnalytes"));

            return result;
        }

        private QueryView getExcludedQueryView(LuminexProtocolSchema schema, String queryName, String noun, BindException errors)
        {
            QuerySettings settings = new QuerySettings(getViewContext(), queryName, queryName);
            QueryView view = createQueryView(settings, schema, errors);
            view.setTitle("Excluded " + noun + "s");
            String helpText = "Shows all of the " + noun.toLowerCase() + "s that have been marked as excluded in "
                    + "individual runs in this folder. Data may be marked as excluded from the results views.";
            view.setTitlePopupHelp("Excluded " + noun + "s", helpText);
            return view;
        }

        private QueryView createQueryView(QuerySettings settings, UserSchema schema, BindException errors)
        {
            QueryView result = new QueryView(schema, settings, errors);
            result.setShadeAlternatingRows(true);
            result.setShowBorders(true);
            result.setShowInsertNewButton(false);
            result.setShowImportDataButton(false);
            result.setShowDeleteButton(false);
            result.setShowUpdateColumn(false);
            result.setFrame(WebPartView.FrameType.PORTAL);
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = super.appendNavTrail(root);
            return result.addChild(_protocol.getName() + " Excluded Data");
        }
    }

    private class GraphLinkQueryView extends QueryView
    {
        private String _tableName;
        private String _controlType;
        private ExpProtocol _protocol;

        public GraphLinkQueryView(String tableName, String controlType, ExpProtocol protocol, UserSchema schema, QuerySettings settings, @Nullable Errors errors)
        {
            super(schema, settings, errors);
            _tableName = tableName;
            _controlType = controlType;
            _protocol = protocol;
            setShowUpdateColumn(false);
            setFrame(WebPartView.FrameType.NONE);
        }

        @Override
        protected void setupDataView(DataView ret)
        {
            super.setupDataView(ret);

            String tablePrefix = _tableName != null ? _tableName + "/Run/" : "";
            ActionURL graph = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getViewContext().getContainer(), _protocol, LuminexController.LeveyJenningsReportAction.class);
            graph.addParameter("controlName", _tableName != null ? "${" + _tableName + "/Name}" : "${ControlName}");
            graph.addParameter("controlType", _controlType != null ? _controlType : "${ControlType}");
            graph.addParameter("analyte", _controlType != null ? "${Analyte/Name}" : "${AnalyteName}");
            graph.addParameter("isotype", "${" + tablePrefix + "Isotype}");
            graph.addParameter("conjugate", "${" + tablePrefix + "Conjugate}");
            SimpleDisplayColumn graphDetails = new UrlColumn(StringExpressionFactory.createURL(graph), "graph");
            ret.getDataRegion().addDisplayColumn(0, graphDetails);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class TitrationQcReportAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AssayView result = new AssayView();
            AssaySchema schema = form.getProvider().createProtocolSchema(getUser(), getContainer(), form.getProtocol(), null);
            QuerySettings settings = new QuerySettings(getViewContext(), LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME, LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME);
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("Titration", "IncludeInQcReport"), true));
            setHelpTopic(new HelpTopic("trackLuminexAnalytes"));

            GraphLinkQueryView view = new GraphLinkQueryView("Titration", "Titration", _protocol, schema, settings, errors);
            result.setupViews(view, false, form.getProvider(), form.getProtocol());

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_protocol.getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol));
            return result.addChild("Titration QC Report");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class SinglePointControlQcReportAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AssayView result = new AssayView();
            AssaySchema schema = form.getProvider().createProtocolSchema(getUser(), getContainer(), form.getProtocol(), null);
            QuerySettings settings = new QuerySettings(getViewContext(), LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME, LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);
            setHelpTopic(new HelpTopic("trackLuminexAnalytes"));

            GraphLinkQueryView view = new GraphLinkQueryView("SinglePointControl", "SinglePoint", _protocol, schema, settings, errors);
            result.setupViews(view, false, form.getProvider(), form.getProtocol());

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_protocol.getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol));
            return result.addChild("Single Point Control QC Report");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class LeveyJenningsReportAction extends SimpleViewAction<LeveyJenningsForm>
    {
        private LeveyJenningsForm _form;

        @Override
        public ModelAndView getView(LeveyJenningsForm form, BindException errors) throws Exception
        {
            _form = form;

            if (form.getControlName() == null)
            {
                throw new NotFoundException("No control name specified");
            }
            VBox result = new VBox();
            result.addView(new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null));
            result.addView(new JspView<>("/org/labkey/luminex/view/leveyJenningsReport.jsp", form));
            setHelpTopic(new HelpTopic("trackLuminexAnalytes"));
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            setHelpTopic("luminexSinglePoint");
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_form.getProtocol().getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _form.getProtocol()));
            result.addChild("Levey-Jennings Reports", new ActionURL(LeveyJenningsMenuAction.class, getContainer()).addParameter("rowId", _form.getProtocol().getRowId()));
            return result.addChild(_form.getControlName());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class LeveyJenningsMenuAction extends SimpleViewAction<ProtocolIdForm>
    {
        private ProtocolIdForm _form;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _form = form;
            VBox result = new VBox();
            result.addView(new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null));
            result.addView(new LeveyJenningsMenuView(form.getProtocol()));
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            setHelpTopic("trackLuminexAnalytes");
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_form.getProtocol().getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _form.getProtocol()));
            return result.addChild("Levey-Jennings Reports");
        }
    }

    @RequiresPermission(DesignAssayPermission.class)
    public class SetAnalyteDefaultValuesAction extends FormViewAction<DefaultValuesForm>
    {
        @Override
        public void validateCommand(DefaultValuesForm form, Errors errors)
        {
            AnalyteDefaultTransformer adt = new AnalyteDefaultTransformer(form.getAnalytes(), form.getPositivityThresholds(), form.getNegativeBeads());
            BatchValidationException e = validateDefaultValues(adt);
            for(ValidationException validationErrors: e.getRowErrors())
            {
                errors.reject(ERROR_MSG, validationErrors.getMessage());
            }
        }

        @Override
        public ModelAndView getView(DefaultValuesForm form, boolean reshow, BindException errors) throws Exception
        {
            ExpProtocol protocol = form.getProtocol();

            if (!reshow)
            {
                List<String> analytes = AnalyteDefaultValueService.getAnalyteNames(protocol, getContainer());
                List<String> positivityThresholds = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
                List<String> negativeBeads = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);

                form.setAnalytes(analytes);
                form.setPositivityThresholds(positivityThresholds);
                form.setNegativeBeads(negativeBeads);
            }

            return new JspView<>("/org/labkey/luminex/view/defaultValues.jsp", form, errors);
        }

        @Override
        public boolean handlePost(DefaultValuesForm form, BindException errors) throws Exception
        {
            ExpProtocol protocol = form.getProtocol();

            List<String> analytes = form.getAnalytes();
            List<String> positivityThresholds = form.getPositivityThresholds();
            List<String> negativeBeads = form.getNegativeBeads();

            // TODO: consider using transformer here...
            //AnalyteDefaultTransformer adt = AnalyteDefaultTransformer()

            AnalyteDefaultValueService.setAnalyteDefaultValues(analytes, positivityThresholds, negativeBeads, getContainer(), protocol);

            return true;
        }

        @Override
        public URLHelper getSuccessURL(DefaultValuesForm form)
        {
            return form.getReturnURLHelper();
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Set Default Values");
        }
    }

    private BatchValidationException validateDefaultValues(AnalyteDefaultTransformer adt)
    {
        BatchValidationException errors = new BatchValidationException();

        // TODO: consider placing this inside importData() (as it only effects this).
        // Issue 21413: NPE when importing analyte default values that are missing expected columns
        boolean only_analytes = true;

        // check sizes are a match (e.g. that all analyte names are unique)
        if (adt.getAnalyteMap().keySet().size() != adt.getAnalytes().size())
            errors.addRowError(new ValidationException("The analyte names are not unique."));

        for (Map<String, String> analyteProperities : adt.getAnalyteMap().values())
        {
            if(analyteProperities.size() > 0) only_analytes = false;

            String positivityThreshold = analyteProperities.get(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
            try {
                if (StringUtils.trimToNull(positivityThreshold) != null)
                    Integer.parseInt(positivityThreshold);
            }
            catch (NumberFormatException e)
            {
                errors.addRowError(new ValidationException("The Positivity Threshold '" + positivityThreshold + "' does not appear to be an integer."));
            }
        }

        if(only_analytes && adt.getAnalytes().size() != 0)
            errors.addRowError(new ValidationException("The uploaded file only contains a column of analyte names without any analyte properities."));

        return errors;
    }

    public static class DefaultValuesForm extends ProtocolIdForm
    {
        private List<String> analytes;
        private List<String> positivityThresholds;
        private List<String> negativeBeads;

        public List<String> getAnalytes()
        {
            return analytes;
        }

        public void setAnalytes(List<String> analytes)
        {
            this.analytes = analytes;
        }

        public List<String> getPositivityThresholds()
        {
            return positivityThresholds;
        }

        public void setPositivityThresholds(List<String> positivityThresholds)
        {
            this.positivityThresholds = positivityThresholds;
        }

        public List<String> getNegativeBeads()
        {
            return negativeBeads;
        }

        public void setNegativeBeads(List<String> negativeBeads)
        {
            this.negativeBeads = negativeBeads;
        }
    }

    @RequiresPermission(DesignAssayPermission.class)
    public class ImportDefaultValuesAction extends AbstractQueryImportAction<ProtocolIdForm>
    {
        ExpProtocol _protocol;

        public ImportDefaultValuesAction()
        {
            super(ProtocolIdForm.class);
        }

        @Override
        protected void initRequest(ProtocolIdForm form) throws ServletException
        {
            _protocol = form.getProtocol();
            setNoTableInfo();
            setImportMessage("Import default values for standard analyte properties. " +
                    "Column headers should include: Analyte, " + LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME +
                    ", and " + LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME + ".");
        }

        @Override
        protected int importData(DataLoader dl, FileStream file, String originalName, BatchValidationException errors) throws IOException
        {
            // NOTE: consider being smarter here and intersecting the list of desired columns with dl.getColumns()
            // NOTE: consider making case-insentive
            ColumnDescriptor[] columns = dl.getColumns();
            Boolean err = true;

            List<String> analytes = new ArrayList<>();
            List<String> positivityThresholds = new ArrayList<>();
            List<String> negativeBeads = new ArrayList<>();

            if (columns.length > 0)
            {
                for(ColumnDescriptor cd : columns)
                {
                    if(cd.getColumnName().equals("Analyte"))
                    {
                        for (Map<String, Object> row : dl)
                        {
                            if (row.get("Analyte") != null)
                            {
                                String analyte = row.get("Analyte").toString();
                                analytes.add(analyte);

                                Object positivityThreshold = row.get(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
                                positivityThresholds.add((positivityThreshold !=null) ? positivityThreshold.toString() : null);

                                Object negativeBead = row.get(LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
                                negativeBeads.add((negativeBead != null) ? negativeBead.toString() : null);
                            }
                        }
                        err = false;
                        break;
                    }
                }

                if (err)
                {
                    errors.addRowError(new ValidationException("The uploaded data doesn't appear to have an 'Analyte' column and cannot be parsed"));
                    return -1;
                }
            }

            if (analytes.size() == 0)
            {
                errors.addRowError(new ValidationException("The uploaded data doesn't appear to have any analyte properities to parse"));
                return -1;
            }

            AnalyteDefaultTransformer adt = new AnalyteDefaultTransformer(analytes, positivityThresholds, negativeBeads);
            // NOTE: Watch out! "Only row errors are copied over with the call to addAllErrors"
            List<ValidationException> rowErrors = validateDefaultValues(adt).getRowErrors();
            if (rowErrors.size() > 0)
            {
                for (ValidationException validationErrors : rowErrors)
                    errors.addRowError(validationErrors);
                // NOTE: consider pushing back failure types
                return -1;
            }

            AnalyteDefaultValueService.setAnalyteDefaultValues(adt.getAnalyteMap(), getContainer(), _protocol);

            return adt.size();
        }

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            initRequest(form);
            return getDefaultImportView(form, errors);
        }

        @Override
        protected void validatePermission(User user, BindException errors)
        {
            checkPermissions();
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Import Default Values");
        }
    }

    @RequiresPermission(DesignAssayPermission.class)
    public class ExportDefaultValuesAction extends ExportAction<ProtocolIdForm>
    {
        @Override
        public void export(ProtocolIdForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            ExpProtocol protocol = form.getProtocol();

            final List<String> analytes = AnalyteDefaultValueService.getAnalyteNames(protocol, getContainer());
            final List<String> positivityThresholds = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
            final List<String> negativeBeads = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);

            try (TSVWriter writer = new TSVWriter(){
                @Override
                protected void write()
                {
                    _pw.println("Analyte\t" + LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME + "\t" + LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
                    for (int i=0; i<analytes.size(); i++)
                    {
                        _pw.println( String.format("%s\t%s\t%s", analytes.get(i), positivityThresholds.get(i), negativeBeads.get(i)));
                    }
                }
            })
            {
                writer.setFilenamePrefix("LuminexDefaultValues");
                writer.write(response);
            }
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class SaveExclusionAction extends ApiAction<LuminexSaveExclusionsForm>
    {
        @Override
        public void validateForm(LuminexSaveExclusionsForm form, Errors errors)
        {
            // verify the assayName provided is valid and of type LuminexAssayProvider
            if (form.getProtocol(getContainer()) == null)
            {
                errors.reject(ERROR_MSG, "Luminex assay protocol not found: " + form.getAssayId());
            }

            // verify that the runId is valid and matches an existing run
            if (form.getRunId() == null || ExperimentService.get().getExpRun(form.getRunId()) == null)
            {
                errors.reject(ERROR_MSG, "No run found for id " + form.getRunId());
            }

            ExpRun run = ExperimentService.get().getExpRun(form.getRunId());
            if (!getContainer().equals(run.getContainer()))
            {
                errors.reject(ERROR_MSG, "The run for id " + form.getRunId() + " does not exist in the current container");
            }

            form.validate(errors);
        }

        @Override
        public Object execute(LuminexSaveExclusionsForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
            PipelineJob job = new LuminexExclusionPipelineJob(getViewBackgroundInfo(), root, form);
            PipelineService.get().queueJob(job);

            response.put("success", true);
            response.put("returnUrl", PageFlowUtil.urlProvider(AssayUrls.class).getShowUploadJobsURL(getContainer(), form.getProtocol(getContainer()), ContainerFilter.CURRENT));
            return response;
        }
    }

    // NOTE: following example of AbstractDeleteAction in ExperimentController
    @RequiresPermission(DeletePermission.class)
    public class DeleteGuideSetAction extends FormViewAction<DeleteForm>
    {

        @Override
        public ActionURL getSuccessURL(DeleteForm form)
        {
            return form.getReturnActionURL();
        }

        protected void deleteObjects(DeleteForm form) throws SQLException, ExperimentException, ServletException, InvalidKeyException, BatchValidationException, QueryUpdateServiceException
        {
            List<Map<String, Object>> keys = new ArrayList<>();
            Set<Integer> selections = DataRegionSelection.getSelectedIntegers(getViewContext(), true);

            for (Integer selection : selections)
            {
                Map<String, Object> entry = new HashMap<>();
                entry.put("RowId", selection);
                keys.add(entry);
            }

            LuminexAssayProvider provider = new LuminexAssayProvider();
            LuminexProtocolSchema schema = new LuminexProtocolSchema(getUser(), getContainer(), provider, form.getProtocol(), getContainer());
            QueryUpdateService queryUpdateService = schema.getTable(LuminexProtocolSchema.GUIDE_SET_TABLE_NAME).getUpdateService();
            queryUpdateService.deleteRows(getUser(), getContainer(), keys, null, null);
        }

        @Override
        public void validateCommand(DeleteForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(DeleteForm form, boolean reshow, BindException errors) throws Exception
        {
            return new GuideSetConfirmDeleteView(form);
        }

        @Override
        public boolean handlePost(DeleteForm deleteForm, BindException errors) throws Exception
        {
            if (!deleteForm.isForceDelete())
            {
                return false;
            }
            else
            {
                deleteObjects(deleteForm);
                return true;
            }
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Confirm Deletion");
        }
    }

    public class GuideSetConfirmDeleteView extends JspView<GuideSetsDeleteBean>
    {
        public GuideSetConfirmDeleteView(DeleteForm form)
        {
            super("/org/labkey/luminex/view/guideSetConfirmDelete.jsp");

            GuideSetsDeleteBean bean = new GuideSetsDeleteBean(form.getReturnUrl(), form.getDataRegionSelectionKey(), form.getProtocol().getRowId(), getContainer(), form.getProtocol().getName());

            Set<Integer> selections = DataRegionSelection.getSelectedIntegers(getViewContext(), false);

            SimpleFilter filter = new SimpleFilter();
            filter.addInClause(FieldKey.fromParts("RowId"), selections);

            List<GuideSet> guideSets = new TableSelector(LuminexProtocolSchema.getTableInfoGuideSet(), filter, null).getArrayList(GuideSet.class);
            for (GuideSet gs : guideSets)
            {
                int rowId = gs.getRowId();

                SQLFragment sql = new SQLFragment("SELECT r.name, a.includeInGuideSetCalculation FROM ");
                sql.append(ExperimentService.get().getTinfoExperimentRun(), "r ");
                sql.append("JOIN ");
                if (gs.getIsTitration())
                {
                    sql.append(LuminexProtocolSchema.getTableInfoTitration(), "t ");
                    sql.append("ON r.RowId = t.RunId ");
                    sql.append("JOIN ");
                    sql.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "a ");
                    sql.append("ON t.RowId = a.TitrationId ");
                    sql.append("WHERE a.GuideSetId = ?");
                    sql.add(rowId);
                }
                else
                {
                    sql.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "spc ");
                    sql.append("ON r.RowId = spc.RunId ");
                    sql.append("JOIN ");
                    sql.append(LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), "a ");
                    sql.append("ON spc.RowId = a.SinglePointControlId ");
                    sql.append("WHERE a.GuideSetId = ?");
                    sql.add(rowId);
                }

                List<String> memberRuns = new ArrayList<>();
                List<String> userRuns = new ArrayList<>();
                try (ResultSet rs = new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getResultSet())
                {
                    while (rs.next())
                    {
                        if (rs.getBoolean("includeInGuideSetCalculation") == true)
                            memberRuns.add(rs.getString("name"));
                        else
                            userRuns.add(rs.getString("name"));
                    }
                }
                catch (SQLException e)
                {
                    _log.error("Failed to get ResultSet for analytes", e);

                }

                bean.add(new GuideSetsDeleteBean.GuideSet(rowId, gs.getComment(), gs.isCurrentGuideSet(), memberRuns, userRuns, gs.isValueBased()));
            }

            setModelBean(bean);
        }
    }

    public static class GuideSetsDeleteBean extends DeleteForm
    {
        public static class GuideSet
        {
            private int _guideSetId;
            private String _comment;
            private boolean _current;
            private List<String> _memberRuns;
            private List<String> _userRuns;
            private boolean _valueBased;

            public GuideSet(int guideSetId, String comment, Boolean current, List<String> memberRuns, List<String> userRuns, Boolean valueBased)
            {
                _guideSetId = guideSetId;
                _comment = comment;
                _current = current;
                _valueBased = valueBased;
                _memberRuns = memberRuns;
                _userRuns = userRuns;
            }

            public boolean isValueBased()
            {
                return _valueBased;
            }

            public void setValueBased(boolean valueBased)
            {
                _valueBased = valueBased;
            }

            public List<String> getMemberRuns()
            {
                return _memberRuns;
            }

            public void setMemberRuns(List<String> memberRuns)
            {
                _memberRuns = memberRuns;
            }

            public List<String> getUserRuns()
            {
                return _userRuns;
            }

            public void setUserRuns(List<String> userRuns)
            {
                _userRuns = userRuns;
            }

            public int getGuideSetId()
            {
                return _guideSetId;
            }

            public void setGuideSetId(int guideSetId)
            {
                _guideSetId = guideSetId;
            }

            public String getComment()
            {
                return _comment;
            }

            public void setComment(String comment)
            {
                _comment = comment;
            }

            public Boolean getCurrent()
            {
                return _current;
            }

            public void setCurrent(boolean current)
            {
                _current = current;
            }
        }

        private List<GuideSet> _guideSets;

        public GuideSetsDeleteBean(String returnUrl, String selectionKey, int protocolId, Container container, String assayName)
        {
            _guideSets = new ArrayList<>();
            setReturnUrl(returnUrl);
            setDataRegionSelectionKey(selectionKey);
            setRowId(protocolId); // fixes getProtocol in view
            setContainer(container);
            setAssayName(assayName);
        }

        public void add(GuideSet gs)
        {
            _guideSets.add(gs);
        }

        public List<GuideSet> getGuideSets()
        {
            return _guideSets;
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class ManageGuideSetAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AssayView result = new AssayView();
            final AssaySchema schema = form.getProvider().createProtocolSchema(getUser(), getContainer(), form.getProtocol(), null);
            QuerySettings settings = new QuerySettings(getViewContext(), LuminexProtocolSchema.GUIDE_SET_TABLE_NAME, LuminexProtocolSchema.GUIDE_SET_TABLE_NAME);
            settings.setBaseSort(new Sort("-RowId")); // Issue 22935
            setHelpTopic(new HelpTopic("applyGuideSets"));

            final int protocolId = _protocol.getRowId();
            GraphLinkQueryView view = new GraphLinkQueryView(null, null, _protocol, schema, settings, errors)
            {
                @Override
                public ActionButton createDeleteButton()
                {
                    ActionURL urlDelete = new ActionURL(LuminexController.DeleteGuideSetAction.class, getContainer());
                    urlDelete.addReturnURL(getReturnURL());
                    urlDelete.addParameter("rowId", protocolId);
                    ActionButton btnDelete = new ActionButton(urlDelete, "Delete");
                    btnDelete.setIconCls("trash");
                    btnDelete.setActionType(ActionButton.Action.POST);
                    btnDelete.setDisplayPermission(DeletePermission.class);
                    btnDelete.setRequiresSelection(true);
                    return btnDelete;
                }
            };
            view.setShowInsertNewButton(false);
            view.setShowImportDataButton(false);
            result.setupViews(view, false, form.getProvider(), form.getProtocol());

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_protocol.getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol));
            return result.addChild("Manage Guide Sets");
        }
    }
}
