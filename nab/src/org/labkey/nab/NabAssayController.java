/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.nab;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiJsonForm;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssaySchema;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.AssayUrls;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.dilution.DilutionDataRow;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.dilution.WellDataRow;
import org.labkey.api.assay.nab.Luc5Assay;
import org.labkey.api.assay.nab.NabUrls;
import org.labkey.api.assay.nab.RenderAssayBean;
import org.labkey.api.assay.nab.RenderAssayForm;
import org.labkey.api.assay.nab.view.DilutionGraphAction;
import org.labkey.api.assay.nab.view.GraphSelectedAction;
import org.labkey.api.assay.nab.view.GraphSelectedBean;
import org.labkey.api.assay.nab.view.GraphSelectedForm;
import org.labkey.api.assay.nab.view.MultiGraphAction;
import org.labkey.api.assay.nab.view.RunDetailOptions;
import org.labkey.api.assay.nab.view.RunDetailsAction;
import org.labkey.api.assay.nab.view.RunDetailsHeaderView;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateSampleFilePropertyHelper;
import org.labkey.api.assay.plate.Position;
import org.labkey.api.assay.plate.PositionImpl;
import org.labkey.api.assay.plate.WellData;
import org.labkey.api.assay.plate.WellGroup;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.FitFailedException;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.defaults.DefaultValueService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.ElevatedUser;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.roles.ReaderRole;
import org.labkey.api.study.assay.RunDatasetContextualRoles;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.JsonUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.qc.NabWellQCFlag;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class NabAssayController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(NabAssayController.class,
        NabUploadWizardAction.class,
        GetNabRunsAction.class,
        GetStudyNabGraphURLAction.class,
        StudyNabGraphAction.class,
        GetStudyNabRunsAction.class
    );

    public NabAssayController()
    {
        setActionResolver(_resolver);
    }

    public static class NabUrlsImpl implements NabUrls
    {
        @Override
        public ActionURL getSampleXLSTemplateURL(Container container, ExpProtocol protocol)
        {
            ActionURL url = new ActionURL(SampleSpreadsheetTemplateAction.class, container);
            url.addParameter("protocol", protocol.getRowId());
            return url;
        }

        @Override
        public ActionURL urlGraph(Container container)
        {
            return new ActionURL(GraphAction.class, container);
        }

        @Override
        public ActionURL urlDownloadDatafile(Container container)
        {
            return new ActionURL(DownloadDatafileAction.class, container);
        }

        @Override
        public ActionURL urlDeleteRun(Container container)
        {
            return new ActionURL(DeleteRunAction.class, container);
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        @Override
        public ModelAndView getView(Object o, BindException errors)
        {
            return HttpView.redirect(urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    protected DilutionAssayProvider getProvider(ExpRun run)
    {
        AssayProvider provider = AssayService.get().getProvider(run.getProtocol());
        if (!(provider instanceof DilutionAssayProvider))
            throw new NotFoundException("Run " + run.getRowId() + " is not a NAb run.");
        return (DilutionAssayProvider) provider;
    }

    protected DilutionDataHandler getDataHandler(ExpRun run)
    {
        return getProvider(run).getDataHandler();
    }

    @RequiresPermission(ReadPermission.class)
    public class DownloadDatafileAction extends SimpleViewAction<RenderAssayForm>
    {
        @Override
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            if (form.getRowId() < 0)
            {
                throw new NotFoundException("No run specified");
            }
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }
            File file = getDataHandler(run).getDataFile(run);
            if (file == null)
            {
                throw new NotFoundException("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
            }
            PageFlowUtil.streamFile(getViewContext().getResponse(), file, true);
            return null;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException("Not Yet Implemented");
        }
    }

    private Date getCustomViewModifiedDate(ExpRun run)
    {
        ExpProtocol protocol = run.getProtocol();
        // There are two potential places the view could be saved
        CustomView runView1 = QueryService.get().getCustomView(getUser(), getContainer(), getUser(),
                AssaySchema.NAME, AssaySchema.getLegacyProtocolTableName(protocol, AssayProtocolSchema.RUNS_TABLE_NAME), NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);
        CustomView runView2 = QueryService.get().getCustomView(getUser(), getContainer(), getUser(),
                getProvider(run).createProtocolSchema(getUser(), getContainer(), protocol, null).getSchemaName(), AssayProtocolSchema.RUNS_TABLE_NAME, NabAssayProvider.CUSTOM_DETAILS_VIEW_NAME);

        // Find the newest view and return its modified date
        Date d1 = runView1 == null ? null : runView1.getModified();
        Date d2 = runView2 == null ? null : runView2.getModified();
        if (d1 == null)
        {
            // If there's no first date, the second must be fine (even if it's null)
            return d2;
        }
        if (d2 == null)
        {
            // If there's not second date, the first must be fine (even if it's null)
            return d1;
        }
        // Otherwise, return the more recent of the two
        return d1.compareTo(d2) > 0 ? d1 : d2;
    }

    static Cache<String,NAbRunWrapper> ASSAY_CACHE = CacheManager.getCache(10, TimeUnit.MINUTES.toMillis(5), "NabAssayCache");

    private DilutionAssayRun getCachedRun(ExpRun run)
    {
        NAbRunWrapper cache = ASSAY_CACHE.get(getViewContext().getSession().getId());
        if (cache == null || cache.getRun() == null)
            return null;
        DilutionAssayRun assay = cache.getRun();
        Date customViewModified = getCustomViewModifiedDate(run);
        // There's no custom view, so it can't have been modified since we cached the run:
        if (customViewModified == null)
            return assay;
        // Check the view modification time against the time we cached the run.  If the view has been changed since
        // the run was cached, return null.
        Date cachedDate = cache.getDate();
        if (cachedDate.after(customViewModified))
            return assay;
        return null;
    }

    private void putCachedRun(DilutionAssayRun assay)
    {
        if (PageFlowUtil.isRobotUserAgent(getViewContext().getRequest().getHeader("User-Agent")))
            return;
        ASSAY_CACHE.put(getViewContext().getSession().getId(), new NAbRunWrapper(assay, new Date()));
    }

    private void clearCachedRun()
    {
        ASSAY_CACHE.remove(getViewContext().getSession().getId());
    }

    private DilutionAssayRun _getNabAssayRun(ExpRun run, StatsService.CurveFitType fit, User elevatedUser) throws ExperimentException
    {
        // cache last NAb assay run in session.  This speeds up the case where users bring up details view and
        // then immediately hit the 'print' button.
        DilutionAssayRun assay = getCachedRun(run);
        if (fit != null || assay == null ||
                (assay.getRunRowId() != null && run.getRowId() != assay.getRunRowId().intValue()) ||
                (assay.getRun() != null && run.getRowId() != assay.getRun().getRowId()))
        {
            try
            {
                assay = getDataHandler(run).getAssayResults(run, elevatedUser, fit);
                if (assay != null && fit == null)
                    putCachedRun(assay);
            }
            catch (DilutionDataHandler.MissingDataFileException e)
            {
                throw new NotFoundException(e.getMessage());
            }
        }
        return assay;
    }

    private static final String LAST_NAB_RUN_KEY = NabAssayController.class.getName() + "/LastNAbRun";

    @RequiresPermission(ReadPermission.class)
    @ContextualRoles(RunDatasetContextualRoles.class)
    public class DetailsAction extends RunDetailsAction<RenderAssayBean>
    {
        @Override
        protected DilutionAssayRun getNabAssayRun(ExpRun run, StatsService.CurveFitType fit, User user) throws ExperimentException
        {
            return _getNabAssayRun(run, fit, user);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            if (null != _protocol)
            {
                ActionURL runDataURL = urlProvider(AssayUrls.class).getAssayResultsURL(getContainer(), _protocol, _runRowId);
                root.addChild(_protocol.getName() + " Data", runDataURL);
                root.addChild("Run " + _runRowId + " Details");
            }
        }
    }

    public static class NabGraphSelectedBean extends GraphSelectedBean
    {
        public NabGraphSelectedBean(ViewContext context, ExpProtocol protocol, int[] cutoffs, int[] dataObjectIds, String captionColumn, String chartTitle)
        {
            super(context, protocol, cutoffs, dataObjectIds, captionColumn, chartTitle);
        }

        @Override
        public QueryView createQueryView()
        {
            AssayProvider provider = AssayService.get().getProvider(_protocol);
            AssayProtocolSchema schema = provider.createProtocolSchema(_context.getUser(), _context.getContainer(), _protocol, null);
            QuerySettings settings = schema.getSettings(_context, AssayProtocolSchema.DATA_TABLE_NAME, AssayProtocolSchema.DATA_TABLE_NAME);
            QueryView dataView = new NabProtocolSchema.NabResultsQueryView(_protocol, _context, settings)
            {
                @Override
                public DataView createDataView()
                {
                    DataView view = super.createDataView();
                    SimpleFilter filter = new SimpleFilter();
                    SimpleFilter existingFilter = (SimpleFilter) view.getRenderContext().getBaseFilter();
                    if (existingFilter != null)
                        filter.addAllClauses(existingFilter);
                    List<Integer> objectIds = new ArrayList<>(_dataObjectIds.length);
                    for (int dataObjectId : _dataObjectIds)
                        objectIds.add(Integer.valueOf(dataObjectId));

                    filter.addInClause(FieldKey.fromString("RowId"), objectIds);
                    view.getDataRegion().setRecordSelectorValueColumns("RowId");
                    view.getRenderContext().setBaseFilter(filter);
                    return view;
                }
            };
            return dataView;
        }

        @Override
        public ActionURL getGraphRenderURL()
        {
            return new ActionURL(NabMultiGraphAction.class, _context.getContainer());
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class NabGraphSelectedAction extends GraphSelectedAction<GraphSelectedForm>
    {
        @Override
        protected GraphSelectedBean createSelectionBean(ViewContext context, ExpProtocol protocol, int[] cutoffs, int[] dataObjectIds, String caption, String title)
        {
            return new NabGraphSelectedBean(context, protocol, cutoffs, dataObjectIds, caption, title);
        }
    }

    public static class DeleteRunForm
    {
        private int _rowId;
        private boolean _reupload;

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }

        public boolean isReupload()
        {
            return _reupload;
        }

        public void setReupload(boolean reupload)
        {
            _reupload = reupload;
        }
    }

    @RequiresPermission(DeletePermission.class)
    public class DeleteRunAction extends FormHandlerAction<DeleteRunForm>
    {
        private ExpRun _run;
        private File _file;

        @Override
        public void validateCommand(DeleteRunForm form, Errors errors)
        {
            if (form.getRowId() == 0)
            {
                throw new NotFoundException("No run specified");
            }
            _run = ExperimentService.get().getExpRun(form.getRowId());
            if (_run == null)
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");

            if (form.isReupload())
            {
                _file = getDataHandler(_run).getDataFile(_run);
                if (_file == null)
                {
                    throw new NotFoundException("Data file for run " + _run.getName() + " was not found.  Deleted from the file system?");
                }
            }
        }

        @Override
        public boolean handlePost(DeleteRunForm form, BindException errors) throws Exception
        {
            _run.delete(getUser());
            return true;
        }

        @Override
        public URLHelper getSuccessURL(DeleteRunForm form)
        {
            if (form.isReupload())
            {
                ActionURL reuploadURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                reuploadURL.addParameter("dataFile", _file.getPath());

                return reuploadURL;
            }
            return urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _run.getProtocol());
        }
    }

    @RequiresPermission(ReadPermission.class)
    @ContextualRoles(RunDatasetContextualRoles.class)
    public class NabMultiGraphAction extends MultiGraphAction<GraphSelectedForm>
    {
    }

    @RequiresPermission(ReadPermission.class)
    @ContextualRoles(RunDatasetContextualRoles.class)
    public class GraphAction extends DilutionGraphAction
    {
        @Override
        protected User getGraphUser()
        {
            // See comment in DetailsAction about the elevatedUser
            return ElevatedUser.ensureContextualRoles(getContainer(), getUser(), Pair.of(ReadPermission.class, ReaderRole.class));
        }

        @Override
        protected DilutionAssayRun getAssayRun(ExpRun run, StatsService.CurveFitType fit, User user) throws ExperimentException
        {
            return _getNabAssayRun(run, fit, user);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class SampleSpreadsheetForm
    {
        private int _protocol;

        public int getProtocol()
        {
            return _protocol;
        }

        public void setProtocol(int protocol)
        {
            _protocol = protocol;
        }
    }

    private static class SampleTemplateWriter extends ExcelWriter
    {
        private final Container _container;
        private final Domain _sampleDomain;
        private final List<WellGroup> _sampleGroups;
        private final Domain _virusDomain;
        private final List<WellGroup> _virusGroups;

        public SampleTemplateWriter(Container container, Domain sampleDomain, List<WellGroup> sampleGroups, Domain virusDomain, List<WellGroup> virusGroups)
        {
            _sampleDomain = sampleDomain;
            _sampleGroups = sampleGroups;
            _virusDomain = virusDomain;
            _virusGroups = virusGroups;
            _container = container;
        }

        @Override
        protected void renderSheet(Workbook workbook, int sheetNumber)
        {
            Sheet sheet = workbook.createSheet(getSheetName(sheetNumber));
            sheet.getPrintSetup().setPaperSize(PrintSetup.LETTER_PAPERSIZE);

            // Render the header row and collect default values for the sample/virus property columns:
            List<String> headers = new ArrayList<>();
            headers.add(PlateSampleFilePropertyHelper.SAMPLE_WELLGROUP_COLUMN);
            headers.add(PlateSampleFilePropertyHelper.PLATELOCATION_COLUMN);

            Map<DomainProperty, Object> defaultValues = DefaultValueService.get().getDefaultValues(_container, _sampleDomain);
            Map<String, Object> columnToDefaultValue = new HashMap<>();
            for (DomainProperty property : _sampleDomain.getProperties())
            {
                columnToDefaultValue.put(property.getName(), defaultValues.get(property));
                headers.add(property.getName());
            }

            if (_virusGroups.size() > 0)
            {
                headers.add(NabVirusFilePropertyHelper.VIRUS_WELLGROUP_COLUMN);
                if (_virusDomain != null)
                {
                    defaultValues = DefaultValueService.get().getDefaultValues(_container, _virusDomain);
                    for (DomainProperty property : _virusDomain.getProperties())
                    {
                        columnToDefaultValue.put(property.getName(), defaultValues.get(property));
                        headers.add(property.getName());
                    }
                }
            }

            Row firstRow = sheet.createRow(0);
            for (int column = 0; column < headers.size(); column++)
            {
                String header = headers.get(column);
                Cell cell = firstRow.getCell(column, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(header);
                cell.setCellStyle(getBoldFormat(sheet.getWorkbook()));
            }

            // Render the rows (i.e. well group names, virus group names, and default property values):
            int rowNum = 1;
            for (WellGroup sampleGroup : _sampleGroups)
            {
                if (_virusGroups.size() > 0)
                {
                    for (WellGroup virusGroup : _virusGroups)
                        renderRow(sheet, headers, columnToDefaultValue, rowNum++, sampleGroup, virusGroup);
                }
                else
                    renderRow(sheet, headers, columnToDefaultValue, rowNum++, sampleGroup, null);
            }
        }

        private void renderRow(Sheet sheet, List<String> headers, Map<String, Object> columnToDefaultValue, int rowNum, WellGroup sample, @Nullable WellGroup virus)
        {
            Row row = sheet.createRow(rowNum);
            for (int column = 0; column < headers.size(); column++)
            {
                Object defaultValue = columnToDefaultValue.get(headers.get(column));
                if (PlateSampleFilePropertyHelper.SAMPLE_WELLGROUP_COLUMN.equals(headers.get(column)))
                {
                    row.createCell(column).setCellValue(sample.getName());
                }
                else if (PlateSampleFilePropertyHelper.PLATELOCATION_COLUMN.equals(headers.get(column)))
                {
                    row.createCell(column).setCellValue(sample.getPositionDescription());
                }
                else if (NabVirusFilePropertyHelper.VIRUS_WELLGROUP_COLUMN.equals(headers.get(column)) && virus != null)
                {
                    row.createCell(column).setCellValue(virus.getName());
                }
                else if (defaultValue != null)
                {
                    Cell cell = row.createCell(column);
                    if (defaultValue instanceof Number)
                        cell.setCellValue(((Number) defaultValue).doubleValue());
                    else if (defaultValue instanceof Date)
                        cell.setCellValue((Date) defaultValue);
                    else if (defaultValue instanceof Boolean)
                        cell.setCellValue(((Boolean) defaultValue).booleanValue());
                    else
                        cell.setCellValue(defaultValue.toString());
                }
            }
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class SampleSpreadsheetTemplateAction extends ExportAction<SampleSpreadsheetForm>
    {
        @Override
        public void export(SampleSpreadsheetForm sampleSpreadsheetForm, HttpServletResponse response, BindException errors)
        {
            ViewContext context = getViewContext();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(sampleSpreadsheetForm.getProtocol());
            if (protocol == null)
            {
                throw new NotFoundException("Protocol " + sampleSpreadsheetForm.getProtocol() + " does not exist.");
            }

            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider == null || !(provider instanceof NabAssayProvider))
            {
                String message = "Protocol " + sampleSpreadsheetForm.getProtocol() + " is not a NAb protocol: " + protocol.getName();
                throw new NotFoundException(message);
            }
            NabAssayProvider nabProvider = ((NabAssayProvider) provider);
            Domain sampleDomain = nabProvider.getSampleWellGroupDomain(protocol);
            Domain virusDomain = nabProvider.getVirusWellGroupDomain(protocol);
            Plate template = nabProvider.getPlate(context.getContainer(), protocol);
            if (template == null)
            {
                throw new NotFoundException("The plate template for this assay design could not be found.  It may have been deleted by an administrator.");
            }

            List<WellGroup> sampleGroups = new ArrayList<>();
            List<WellGroup> virusGroups = new ArrayList<>();
            for (WellGroup group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.SPECIMEN)
                    sampleGroups.add(group);
                else if (group.getType() == WellGroup.Type.VIRUS)
                    virusGroups.add(group);
            }

            ExcelWriter xl = new SampleTemplateWriter(getContainer(), sampleDomain, sampleGroups, virusDomain, virusGroups);
            xl.setFilenamePrefix("metadata");
            xl.renderWorkbook(response);
        }
    }

    /**
     * Avoid serializing the NAb run, since many of its child objects aren't serializable themselves
     */
    public static class NAbRunWrapper implements Serializable
    {
        private transient DilutionAssayRun _run;
        private transient Date _date;

        public NAbRunWrapper(DilutionAssayRun run, Date date)
        {
            _run = run;
            _date = date;
        }

        public NAbRunWrapper()
        {
            // For serialization
        }

        public DilutionAssayRun getRun()
        {
            return _run;
        }

        public Date getDate()
        {
            return _date;
        }
    }

    public static class NabQCForm extends RenderAssayBean
    {
        private boolean _edit = true;
        private int _maxSamplesPerGraph;
        private int _graphsPerRow;
        private int _graphHeight;
        private int _graphWidth;

        public boolean isEdit()
        {
            return _edit;
        }

        public void setEdit(boolean edit)
        {
            _edit = edit;
        }

        @Override
        public int getMaxSamplesPerGraph()
        {
            return _maxSamplesPerGraph;
        }

        @Override
        public void setMaxSamplesPerGraph(int maxSamplesPerGraph)
        {
            _maxSamplesPerGraph = maxSamplesPerGraph;
        }

        @Override
        public int getGraphsPerRow()
        {
            return _graphsPerRow;
        }

        @Override
        public void setGraphsPerRow(int graphsPerRow)
        {
            _graphsPerRow = graphsPerRow;
        }

        @Override
        public int getGraphHeight()
        {
            return _graphHeight;
        }

        @Override
        public void setGraphHeight(int graphHeight)
        {
            _graphHeight = graphHeight;
        }

        @Override
        public int getGraphWidth()
        {
            return _graphWidth;
        }

        @Override
        public void setGraphWidth(int graphWidth)
        {
            _graphWidth = graphWidth;
        }

        public ActionURL getReturnUrl(Container container)
        {
            ActionURL url = new ActionURL(DetailsAction.class, container).addParameter("rowId", getRunId());
            if (getMaxSamplesPerGraph() > 0)
                url.addParameter("maxSamplesPerGraph", getMaxSamplesPerGraph());
            if (getGraphsPerRow() > 0)
                url.addParameter("graphsPerRow", getGraphsPerRow());
            if (getGraphHeight() > 0)
                url.addParameter("graphHeight", getGraphHeight());
            if (getGraphWidth() > 0)
                url.addParameter("graphWidth", getGraphWidth());

            return url;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class QCDataAction extends SimpleViewAction<NabQCForm>
    {
        private int _runId;
        private boolean _editMode;

        @Override
        public ModelAndView getView(NabQCForm form, BindException errors)
        {
            _runId = form.getRowId();
            _editMode = form.isEdit();
            if (!getContainer().hasPermission(getUser(), AdminPermission.class))
                form.setEdit(false);

            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }

            try
            {
                DilutionAssayRun assay = _getNabAssayRun(run, form.getFitTypeEnum(), getUser());
                if (RunDetailsHeaderView.canShowQCData(assay))
                {
                    form.setContext(getViewContext());
                    form.setAssay(assay);

                    return new JspView<RenderAssayBean>("/org/labkey/nab/view/nabQC.jsp", form);
                }
                else
                    return new HtmlView(HtmlString.unsafe("<span class='labkey-error'>NAb QC is not available for multi-virus configurations.</span>"));
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
                return new SimpleErrorView(errors);
            }
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            ActionURL detailsURL = new ActionURL(DetailsAction.class, getContainer()).addParameter("rowId", _runId);
            root.addChild("Run " + _runId + " Details", detailsURL);
            root.addChild(_editMode ? "QC NAb Data" : "Excluded Data");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetQCControlInfoAction extends ReadOnlyApiAction<NabQCForm>
    {
        @Override
        public ApiResponse execute(NabQCForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }

            try
            {
                DilutionAssayRun assay = _getNabAssayRun(run, form.getFitTypeEnum(), getUser());
                List<Map<String, Object>> plates = new ArrayList<>();

                // serialize the plates
                for (Plate plate : assay.getPlates())
                {
                    plates.add(serializePlate(plate, assay));
                }
                response.put("plates", plates);

                List<Map<String, Object>> dilutionSummaries = new ArrayList<>();
                response.put("dilutionSummaries", dilutionSummaries);
                int sampleNum = 0;
                for (DilutionAssayRun.SampleResult sampleResult : assay.getSampleResults())
                {
                    DilutionSummary summary = sampleResult.getDilutionSummary();
                    Map<String, Object> summaryMap = serializeDilutionSummary(form, summary, sampleNum);

                    ActionURL graphUrl = urlProvider(NabUrls.class).urlGraph(getContainer());
                    graphUrl.addParameter("rowId", form.getRowId())
                            .addParameter("maxSamples", 1)
                            .addParameter("firstSample", sampleNum++);

                    summaryMap.put("graphUrl", graphUrl.getLocalURIString());

                    dilutionSummaries.add(summaryMap);
                }
            }
            catch (ExperimentException e)
            {
                errors.reject(SpringActionController.ERROR_MSG, e.getMessage());
            }

            return response;
        }
    }

    /**
     * Serializes plate information for both control wells and the raw plate data
     *
     * @param plate
     * @return
     */
    private Map<String, Object> serializePlate(Plate plate, DilutionAssayRun assay)
    {
        Map<String, Object> o = new HashMap<>();
        List<String> columnLabel = new ArrayList<>();

        o.put("plateName", "Plate " + plate.getPlateNumber());
        Map<String, Object> controls = new HashMap<>();
        o.put("controls", controls);
        controls.put("columnLabel", columnLabel);
        controls.put("virusControlMean", Luc5Assay.intString(assay.getVirusControlMean(plate, null)));
        controls.put("virusControlPlusMinus", Luc5Assay.percentString(assay.getVirusControlPlusMinus(plate, null)));
        controls.put("cellControlMean", Luc5Assay.intString(assay.getCellControlMean(plate, null)));
        controls.put("cellControlPlusMinus", Luc5Assay.percentString(assay.getCellControlPlusMinus(plate, null)));

        WellGroup virusGroup = plate.getWellGroup(WellGroup.Type.CONTROL, DilutionManager.VIRUS_CONTROL_SAMPLE);
        WellGroup cellGroup = plate.getWellGroup(WellGroup.Type.CONTROL, DilutionManager.CELL_CONTROL_SAMPLE);

        Set<Integer> colPos = new TreeSet<>();
        Set<Integer> rowPos = new TreeSet<>();
        List<Integer> colOrder = new ArrayList<>();
        for (Position position : virusGroup.getPositions())
        {
            colPos.add(position.getColumn());
            rowPos.add(position.getRow());
        }
        colOrder.addAll(colPos);
        colPos.clear();
        for (Position position : cellGroup.getPositions())
        {
            colPos.add(position.getColumn());
            rowPos.add(position.getRow());
        }

        colOrder.addAll(colPos);
        for (Integer col : colOrder)
        {
            columnLabel.add(String.valueOf(col + 1));
        }

        List<List<Map<String, Object>>> rows = new ArrayList<>();
        controls.put("rows", rows);
        for (Integer row : rowPos)
        {
            List<Map<String, Object>> r = new ArrayList<>();
            for (Integer col : colOrder)
            {
                Map<String, Object> well = new HashMap<>();
                Position pos = new PositionImpl(getContainer(), row, col);

                well.put("plate", plate.getPlateNumber());
                well.put("col", col);
                well.put("rowlabel", (char) ('A' + row));
                well.put("row", row);
                well.put("value", plate.getWell(row, col).getValue());
                well.put("sampleName", virusGroup.contains(pos) ? DilutionManager.VIRUS_CONTROL_SAMPLE : DilutionManager.CELL_CONTROL_SAMPLE);
                r.add(well);
            }
            rows.add(r);
        }

        // serialize all raw data from the plate
        Map<String, Object> rawData = new HashMap<>();
        List<List<Map<String, Object>>> data = new ArrayList<>();
        List<String> colLabels = new ArrayList<>();

        o.put("rawdata", rawData);
        rawData.put("data", data);
        rawData.put("columnLabel", colLabels);

        for (int col = 0; col < plate.getColumns(); col++)
        {
            colLabels.add(String.valueOf(col + 1));
        }

        for (int rowIdx = 0; rowIdx < plate.getRows(); rowIdx++)
        {
            List<Map<String, Object>> row = new ArrayList<>();
            data.add(row);
            for (int col = 0; col < plate.getColumns(); col++)
            {
                Map<String, Object> well = new HashMap<>();

                well.put("plate", plate.getPlateNumber());
                well.put("col", col);
                well.put("rowlabel", (char) ('A' + rowIdx));
                well.put("row", rowIdx);
                well.put("value", plate.getWell(rowIdx, col).getValue());
                row.add(well);
            }
        }
        return o;
    }

    private Map<String, Object> serializeDilutionSummary(RenderAssayBean form, DilutionSummary summary, int sampleNum) throws FitFailedException
    {
        Map<String, Object> o = new HashMap<>();
        DecimalFormat shortDecFormat = new DecimalFormat("0.###");

        String sampleName = summary.getMaterialKey().getDisplayString(RunDetailOptions.DataIdentifier.Specimen);
        if (sampleName == null)
            sampleName = summary.getFirstWellGroup().getName();
        o.put("name", sampleName);
        o.put("methodLabel", summary.getMethod().getAbbreviation());
        o.put("neutLabel", form.getNeutralizationAbrev());
        o.put("sampleNum", sampleNum);

        List<Map<String, Object>> dilutions = new ArrayList<>();
        o.put("dilutions", dilutions);

        for (WellData data : summary.getWellData())
        {
            Map<String, Object> row = new HashMap<>();
            dilutions.add(row);

            row.put("dilution", shortDecFormat.format(data.getDilution()));
            row.put("neut", Luc5Assay.percentString(summary.getPercent(data)));
            row.put("neutPlusMinus", Luc5Assay.percentString(summary.getPlusMinus(data)));
            row.put("sampleName", sampleName);
            row.put("sampleNum", sampleNum);

            if (data instanceof WellGroup)
            {
                Plate plate = data.getPlate();
                List<Map<String, Object>> wells = new ArrayList<>();
                row.put("wells", wells);
                for (Position pos : ((WellGroup) data).getPositions())
                {
                    Map<String, Object> well = new HashMap<>();

                    well.put("col", pos.getColumn());
                    well.put("row", pos.getRow());
                    well.put("rowlabel", (char) ('A' + pos.getRow()));
                    well.put("value", plate.getWell(pos.getRow(), pos.getColumn()).getValue());
                    well.put("plateNum", plate.getPlateNumber());

                    wells.add(well);
                }
            }
        }
        return o;
    }

    @RequiresPermission(AdminPermission.class)
    public class SaveQCControlInfoAction extends MutatingApiAction<QCControlInfo>
    {
        private ExpRun _run;

        @Override
        public void validateForm(QCControlInfo form, Errors errors)
        {
            _run = ExperimentService.get().getExpRun(form.getRunId());
            if (_run == null)
            {
                errors.reject(ERROR_MSG, "NAb Run " + form.getRunId() + " does not exist.");
            }
        }

        @Override
        public ApiResponse execute(QCControlInfo form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            DbSchema schema = DilutionManager.getSchema();
            DbScope scope = schema.getScope();
            ExpRun run = ExperimentService.get().getExpRun(form.getRunId());

            if (run != null)
            {
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
                AssayProvider provider = AssayService.get().getProvider(protocol);

                if (provider instanceof DilutionAssayProvider)
                {
                    DilutionDataHandler handler = ((DilutionAssayProvider)provider).getDataHandler();
                    if (handler instanceof NabDataHandler)
                    {
                        NabDataHandler dataHandler = (NabDataHandler)handler;
                        try (DbScope.Transaction transaction = scope.ensureTransaction())
                        {
                            // clear all well exclusions for this run
                            DilutionManager.clearWellExclusions(scope.getSqlDialect(), form.getRunId());

                            // clear out prior qc flags
                            AssayService.get().deleteFlagsForRun(getContainer(), getUser(), provider, form.getRunId());
                            Set<String> excludedWells = new HashSet<>();
                            Collection<Integer> wellRowIds = new HashSet<>();

                            for (WellExclusion well : form.getExclusions())
                            {
                                excludedWells.add(NabAssayController.getKey(well.getPlate(), well.getRow(), well.getCol()));

                                // add the assay qc flag for the exclusions
                                NabWellQCFlag flag = new NabWellQCFlag(form.getRunId(), well);
                                AssayService.get().saveFlag(getContainer(), getUser(), provider, flag);
                            }

                            // get the rowid's for the wells to exclude
                            for (WellDataRow wellData : DilutionManager.getWellDataRows(_run))
                            {
                                if (excludedWells.contains(getKey(wellData)))
                                {
                                    wellRowIds.add(wellData.getRowId());
                                }
                            }

                            // set the updated well exclusions and create the QC flags
                            if (!wellRowIds.isEmpty())
                            {
                                DilutionManager.setWellExclusions(scope.getSqlDialect(), wellRowIds);
                            }

                            // update the dilutiondata tables to reflect the exclusions
                            List<Map<String, Object>> dilutionRows = new ArrayList<>();
                            List<Map<String, Object>> wellRows = new ArrayList<>();
                            dataHandler.recalculateWellData(protocol, run, getUser(), dilutionRows, wellRows);

                            // get the primary keys for the existing dilution rows
                            Map<String, DilutionDataRow> existingDilution = new CaseInsensitiveHashMap<>();
                            SimpleFilter filter = SimpleFilter.createContainerFilter(getContainer());
                            filter.addCondition(FieldKey.fromParts("runId"), form.getRunId());
                            for (DilutionDataRow row : new TableSelector(DilutionManager.getTableInfoDilutionData(), filter, null).getArrayList(DilutionDataRow.class))
                                existingDilution.put(getDilutionKey(row), row);

                            for (Map<String, Object> row : dilutionRows)
                            {
                                String key = getDilutionKey(row);
                                if (existingDilution.containsKey(key))
                                {
                                    DilutionDataRow current = existingDilution.get(key);
                                    Table.update(getUser(), DilutionManager.getTableInfoDilutionData(), row, current.getRowId());
                                }
                            }

                            // update nabspecimen and cutoffs to reflect the exclusions
                            List<Map<String, Object>> rawData = dataHandler.calculateDilutionStats(run, getUser(), null, true, true);
                            if (!rawData.isEmpty())
                            {
                                ExpData data = run.getDataOutputs().get(0);
                                List<Map<String, Object>> specimenRows = new ArrayList<>();
                                List<Map<String, Object>> cutoffRows = new ArrayList<>();
                                Map<String, Integer> specimenLsidToRowid = new HashMap<>();
                                dataHandler.recalculateDilutionStats(data, run, protocol, rawData, specimenRows, cutoffRows);

                                Map<String, Map<String, Object>> existingSpecimen = new HashMap<>();
                                for (Map<String, Object> row : new TableSelector(DilutionManager.getTableInfoNAbSpecimen(), new SimpleFilter(FieldKey.fromParts("runId"), form.getRunId()), null).getMapCollection())
                                    existingSpecimen.put(getSpecimenKey(row), row);

                                for (Map<String, Object> row : specimenRows)
                                {
                                    String key = getSpecimenKey(row);
                                    if (existingSpecimen.containsKey(key))
                                    {
                                        Map<String, Object> current = existingSpecimen.get(key);
                                        specimenLsidToRowid.put(String.valueOf(current.get("specimenLsid")), (Integer)current.get("rowId"));
                                        Table.update(getUser(), DilutionManager.getTableInfoNAbSpecimen(), row, current.get("rowId"));
                                    }
                                }

                                // get current cutoff values
                                Map<String, Map<String, Object>> existingCutoffs = new HashMap<>();
                                SQLFragment cutoffSql = new SQLFragment("SELECT * FROM ").append(DilutionManager.getTableInfoCutoffValue(), "").append(" WHERE nabSpecimenId ");
                                schema.getSqlDialect().appendInClauseSql(cutoffSql, specimenLsidToRowid.values());
                                for (Map<String, Object> row : new SqlSelector(schema, cutoffSql).getMapCollection())
                                    existingCutoffs.put(getCutoffKey(row), row);

                                for (Map<String, Object> row : cutoffRows)
                                {
                                    String specimenLsid = String.valueOf(row.get("nabSpecimenId"));
                                    Double cutoff = (Double)row.get("cutoff");

                                    if (specimenLsid != null && cutoff != null && specimenLsidToRowid.containsKey(specimenLsid))
                                    {
                                        Integer rowId = specimenLsidToRowid.get(specimenLsid);
                                        String key = getCutoffKey(rowId, cutoff);

                                        if (existingCutoffs.containsKey(key))
                                        {
                                            Map<String, Object> current = existingCutoffs.get(key);
                                            row.put("nabSpecimenId", rowId);
                                            Table.update(getUser(), DilutionManager.getTableInfoCutoffValue(), row, current.get("rowId"));
                                        }
                                    }
                                }
                            }
                            transaction.commit();
                            // clear the nab run cache
                            clearCachedRun();
                            NabProtocolSchema.clearProtocolFromCutoffCache(protocol.getRowId());
                            response.put("success", true);
                        }
                    }
                }
            }
            return response;
        }

        private String getCutoffKey(Integer specimenId, Double cutoff)
        {
            return specimenId + "-" + cutoff;
        }

        private String getCutoffKey(Map<String, Object> row)
        {
            return getCutoffKey((Integer)row.get("nabSpecimenId"), (Double)row.get("cutoff"));
        }

        private String getSpecimenKey(Map<String, Object> row)
        {
            return String.valueOf(row.get("specimenLsid"));
        }

        private String getDilutionKey(DilutionDataRow row)
        {
            return row.getRunId() + "-" + row.getWellgroupName() + "-" + row.getReplicateName() + "-" + row.getPlateNumber();
        }

        private String getDilutionKey(Map<String, Object> row)
        {
            return row.get("runId") + "-" + row.get("wellGroupName") + "-" + row.get("replicateName") + "-" + row.get("plateNumber");
        }

        private String getKey(WellDataRow row)
        {
            return NabAssayController.getKey(row.getPlateNumber(), row.getRow(), row.getColumn());
        }
    }

    private static String getKey(Object plate, Object row, Object col)
    {
        return String.format("%s-%s-%s", plate, row, col);
    }

    public static class QCControlInfo implements ApiJsonForm
    {
        private final List<WellExclusion> _exclusions = new ArrayList<>();
        private int _runId;

        public int getRunId()
        {
            return _runId;
        }

        public List<WellExclusion> getExclusions()
        {
            return _exclusions;
        }

        @Override
        public void bindJson(JSONObject json)
        {
            JSONArray excludedProp = json.optJSONArray("excluded");
            int runId = json.optInt("runId", -1);

            if (-1 != runId)
            {
                _runId = runId;
            }

            if (excludedProp != null)
            {
                for (JSONObject excluded : JsonUtil.toJSONObjectList(excludedProp))
                {
                    _exclusions.add(new WellExclusion(excluded.getInt("plate"),
                        excluded.getInt("row"),
                        excluded.getInt("col"),
                        excluded.optString("comment"), // issue 51532
                        excluded.optString("specimen")));
                }
            }
        }
    }

    @RequiresPermission(ReadPermission.class)
    @Marshal(Marshaller.Jackson)
    public class GetExcludedWellsAction extends ReadOnlyApiAction<RenderAssayBean>
    {
        @Override
        public ApiResponse execute(RenderAssayBean form, BindException errors)
        {
            ApiSimpleResponse response = new ApiSimpleResponse();
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run != null)
            {
                List<WellExclusion> exclusions = new ArrayList<>();
                Map<String, NabWellQCFlag> qcFlagMap = new HashMap<>();
                AssayProvider provider = AssayService.get().getProvider(run);

                // get the saved assay QC flags to pull comment information from
                for (NabWellQCFlag flag : AssayService.get().getFlags(provider, form.getRowId(), NabWellQCFlag.class))
                {
                    qcFlagMap.put(flag.getKey1(), flag);
                }

                DbSchema schema = DilutionManager.getSchema();

                List<WellDataRow> wells = DilutionManager.getExcludedWellDataRows(run);
                Set<String> specimenLsids = new HashSet<>();
                for (WellDataRow well : wells)
                {
                    specimenLsids.add(well.getSpecimenLsid());
                }

                Map<String, ExpMaterial> specimenMaterials = ExperimentService.get().getExpMaterialsByLsid(specimenLsids)
                        .stream().collect(Collectors.toMap(ExpMaterial::getLSID, material -> material));

                for (WellDataRow well : wells)
                {
                    // need the specimen name
                    String specimenName = null;
                    ExpMaterial material = specimenMaterials.get(well.getSpecimenLsid());
                    if (material != null)
                    {
                        // try to find the specimen id entered for this run
                        for (Map.Entry<PropertyDescriptor, Object> entry : material.getPropertyValues().entrySet())
                        {
                            if (AbstractAssayProvider.SPECIMENID_PROPERTY_NAME.equals(entry.getKey().getName()))
                            {
                                if (entry.getValue() != null)
                                    specimenName = String.valueOf(entry.getValue());
                                break;
                            }
                        }
                    }

                    // nothing found or entered, just default to the specimen well group name
                    if (specimenName == null)
                    {
                        if (well.getSpecimenLsid() != null)
                        {
                            SQLFragment sql = new SQLFragment("SELECT WellGroupName FROM ").append(DilutionManager.getTableInfoNAbSpecimen(), "").
                                    append(" WHERE RunId = ? AND SpecimenLsid = ?");
                            sql.addAll(form.getRowId(), well.getSpecimenLsid());

                            specimenName = new SqlSelector(schema, sql).getObject(String.class);
                        }
                        else
                        {
                            specimenName = well.getControlWellgroup();
                        }
                    }
                    WellExclusion exclusion = new WellExclusion(well.getPlateNumber(), well.getRow(), well.getColumn(), null, null);
                    if (qcFlagMap.containsKey(exclusion.getKey()))
                    {
                        NabWellQCFlag flag = qcFlagMap.get(exclusion.getKey());
                        if (!StringUtils.isBlank(flag.getComment()))
                            exclusion.setComment(flag.getComment());
                    }
                    exclusion.setSpecimen(specimenName);

                    exclusions.add(exclusion);
                }
                response.put("excluded", exclusions);
            }
            else
            {
                errors.reject(ERROR_MSG, "NAb Run " + form.getRunId() + " does not exist.");
            }
            return response;
        }
    }

    public static class WellExclusion
    {
        private int _row;
        private int _col;
        private int _plate;
        private String _comment;
        private String _specimen;

        public WellExclusion(int plate, int row, int col, String comment, String specimen)
        {
            _plate = plate;
            _row = row;
            _col = col;
            _comment = comment;
            _specimen = specimen;
        }

        public String getKey()
        {
            return NabAssayController.getKey(_plate, _row, _col);
        }

        public int getRow()
        {
            return _row;
        }

        public void setRow(int row)
        {
            _row = row;
        }

        public int getCol()
        {
            return _col;
        }

        public void setCol(int col)
        {
            _col = col;
        }

        public int getPlate()
        {
            return _plate;
        }

        public void setPlate(int plate)
        {
            _plate = plate;
        }

        public char getRowLabel()
        {
            return (char)(('A' + _row));
        }

        public String getSpecimen()
        {
            return _specimen;
        }

        public void setSpecimen(String specimen)
        {
            _specimen = specimen;
        }

        public String getComment()
        {
            return _comment;
        }

        public void setComment(String comment)
        {
            _comment = comment;
        }
    }
}