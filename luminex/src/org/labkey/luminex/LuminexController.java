/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TSVWriter;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.AbstractQueryImportAction;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.User;
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
import org.labkey.luminex.query.LuminexProtocolSchema;
import org.labkey.luminex.AnalyteDefaultValueService.AnalyteDefaultTransformer;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Actions for Luminex specific features (Levey-Jennings, QC Report, Excluded Data)
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class LuminexController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(LuminexController.class,
            LuminexUploadWizardAction.class
    );

    public LuminexController()
    {
        super();
        setActionResolver(_resolver);
    }

    @RequiresPermissionClass(ReadPermission.class)
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

    @RequiresPermissionClass(ReadPermission.class)
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
            QuerySettings runsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.RUN_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.RUN_EXCLUSION_TABLE_NAME);
            QueryView runsView = createQueryView(runsSetting, schema, errors);
            runsView.setTitle("Excluded Analytes");
            runsView.setTitlePopupHelp("Excluded Analytes", "Shows all of the analytes that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.setupViews(runsView, false, form.getProvider(), form.getProtocol());

            QuerySettings titrationsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.TITRATION_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.TITRATION_EXCLUSION_TABLE_NAME);
            QueryView titrationsView = createQueryView(titrationsSetting, schema, errors);
            titrationsView.setTitle("Excluded Titrations");
            titrationsView.setTitlePopupHelp("Excluded Titrations", "Shows all of the titrations that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.addView(titrationsView);

            QuerySettings wellsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.WELL_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.WELL_EXCLUSION_TABLE_NAME);
            QueryView wellsView = createQueryView(wellsSetting, schema, errors);
            wellsView.setTitle("Excluded Wells");
            wellsView.setTitlePopupHelp("Excluded Wells", "Shows all of the wells that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.addView(wellsView);

            setHelpTopic(new HelpTopic("excludeAnalytes"));

            return result;
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

    @RequiresPermissionClass(ReadPermission.class)
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
            setHelpTopic(new HelpTopic("applyGuideSets"));
            QueryView view = new QueryView(schema, settings, errors)
            {
                @Override
                protected void setupDataView(DataView ret)
                {
                    super.setupDataView(ret);

                    ActionURL graph = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getViewContext().getContainer(), _protocol, LuminexController.LeveyJenningsReportAction.class);
                    graph.addParameter("titration", "${Titration/Name}");
                    graph.addParameter("analyte", "${Analyte/Name}");
                    graph.addParameter("isotype", "${Titration/Run/Isotype}");
                    graph.addParameter("conjugate", "${Titration/Run/Conjugate}");
                    SimpleDisplayColumn graphDetails = new UrlColumn(StringExpressionFactory.createURL(graph), "graph");
                    ret.getDataRegion().addDisplayColumn(0, graphDetails);
                }
            };
            view.setShadeAlternatingRows(true);
            view.setShowBorders(true);
            view.setShowUpdateColumn(false);
            view.setFrame(WebPartView.FrameType.NONE);
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

    @RequiresPermissionClass(ReadPermission.class)
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
            setHelpTopic(new HelpTopic("applyGuideSets"));
            QueryView view = new QueryView(schema, settings, errors)
            {
                @Override
                protected void setupDataView(DataView ret)
                {
                    super.setupDataView(ret);

                    ActionURL graph = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getViewContext().getContainer(), _protocol, LuminexController.LeveyJenningsReportAction.class);
                    graph.addParameter("controlName", "${SinglePointControl/Name}");
                    graph.addParameter("controlType", "SinglePoint");
                    graph.addParameter("analyte", "${Analyte/Name}");
                    graph.addParameter("isotype", "${SinglePointControl/Run/Isotype}");
                    graph.addParameter("conjugate", "${SinglePointControl/Run/Conjugate}");
                    SimpleDisplayColumn graphDetails = new UrlColumn(StringExpressionFactory.createURL(graph), "graph");
                    ret.getDataRegion().addDisplayColumn(0, graphDetails);
                }
            };
            view.setShadeAlternatingRows(true);
            view.setShowBorders(true);
            view.setShowUpdateColumn(false);
            view.setFrame(WebPartView.FrameType.NONE);
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

    @RequiresPermissionClass(ReadPermission.class)
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
            AssayHeaderView header = new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null);
            result.addView(header);
            JspView report = new JspView<>("/org/labkey/luminex/view/leveyJenningsReport.jsp", form);
            result.addView(report);
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

    @RequiresPermissionClass(ReadPermission.class)
    public class LeveyJenningsMenuAction extends SimpleViewAction<ProtocolIdForm>
    {
        private ProtocolIdForm _form;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _form = form;
            VBox result = new VBox();
            AssayHeaderView header = new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null);
            result.addView(header);
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

    @RequiresPermissionClass(DesignAssayPermission.class)
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

    @RequiresPermissionClass(DesignAssayPermission.class)
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

    @RequiresPermissionClass(DesignAssayPermission.class)
    public class ExportDefaultValuesAction extends ExportAction<ProtocolIdForm>
    {
        @Override
        public void export(ProtocolIdForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            ExpProtocol protocol = form.getProtocol();

            final List<String> analytes = AnalyteDefaultValueService.getAnalyteNames(protocol, getContainer());
            final List<String> positivityThresholds = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
            final List<String> negativeBeads = AnalyteDefaultValueService.getAnalyteProperty(analytes, getContainer(), protocol, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);

            TSVWriter writer = new TSVWriter(){
                @Override
                protected void write()
                {
                    _pw.println("Analyte\t" + LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME + "\t" + LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
                    for (int i=0; i<analytes.size(); i++)
                    {
                        _pw.println( String.format("%s\t%s\t%s", analytes.get(i), positivityThresholds.get(i), negativeBeads.get(i)));
                    }
                }
            };
            writer.setFilenamePrefix("LuminexDefaultValues");
            writer.write(response);
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class SaveExclusionAction extends ApiAction<LuminexSaveExclusionsForm>
    {
        @Override
        public void validateForm(LuminexSaveExclusionsForm form, Errors errors)
        {
            // verify the assayName provided is valid and of type LuminexAssayProvider
            if (form.getProtocol(getContainer()) == null)
            {
                errors.reject(ERROR_MSG, "Luminex assay protocol not found: " + form.getAssayName());
            }

            // verify that the runId is valid and matches an existing run
            if (form.getRunId() == null || ExperimentService.get().getExpRun(form.getRunId()) == null)
            {
                errors.reject(ERROR_MSG, "No run found for id " + form.getRunId());
            }

            form.validate(errors);
        }

        @Override
        public Object execute(LuminexSaveExclusionsForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            try
            {
                PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());
                PipelineJob job = new LuminexExclusionPipelineJob(getViewBackgroundInfo(), root, form);
                PipelineService.get().queueJob(job);

                response.put("success", true);
                response.put("returnUrl", PageFlowUtil.urlProvider(AssayUrls.class).getShowUploadJobsURL(getContainer(), form.getProtocol(getContainer()), ContainerFilter.CURRENT));
            }
            catch (PipelineValidationException e)
            {
                throw new IOException(e);
            }
            return response;
        }
    }
}
