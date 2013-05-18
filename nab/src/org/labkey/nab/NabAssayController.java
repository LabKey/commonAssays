/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.assay.dilution.DilutionSummary;
import org.labkey.api.assay.nab.GraphForm;
import org.labkey.api.assay.nab.Luc5Assay;
import org.labkey.api.assay.nab.RenderAssayBean;
import org.labkey.api.assay.nab.RenderAssayForm;
import org.labkey.api.assay.nab.view.RunDetailsAction;
import org.labkey.api.data.*;
import org.labkey.api.defaults.DefaultValueService;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.nab.NabUrls;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.ContextualRoles;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.roles.ReaderRole;
import org.labkey.api.security.roles.Role;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.WellGroupTemplate;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.*;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.*;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Number;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
        super();
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

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getViewContext().getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class HeaderBean
    {
        private ActionURL _printURL;
        private ActionURL _datafileURL;
        private boolean _writer;

        public HeaderBean(ViewContext context, ActionURL printLink, ActionURL dataFileLink)
        {
            _printURL = printLink;
            _datafileURL = dataFileLink;
            _writer = context.getContainer().hasPermission(context.getUser(), InsertPermission.class);
        }

        public boolean showPrintView()
        {
            return _printURL != null;
        }

        public ActionURL getPrintURL()
        {
            return _printURL;
        }

        public ActionURL getDatafileURL()
        {
            return _datafileURL;
        }

        public boolean showNewRunLink()
        {
            return _writer;
        }
    }

    protected DilutionAssayProvider getProvider(ExpRun run)
    {
        AssayProvider provider = AssayService.get().getProvider(run.getProtocol());
        if (!(provider instanceof DilutionAssayProvider))
            throw new IllegalArgumentException("Run " + run.getRowId() + " is not a NAb run.");
        return (DilutionAssayProvider) provider;
    }

    protected DilutionDataHandler getDataHandler(ExpRun run)
    {
        return getProvider(run).getDataHandler();
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadDatafileAction extends SimpleViewAction<RenderAssayForm>
    {
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

        public NavTree appendNavTrail(NavTree root)
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

    private DilutionAssayRun getCachedRun(ExpRun run)
    {
        NAbRunWrapper cache = (NAbRunWrapper) getViewContext().getSession().getAttribute(LAST_NAB_RUN_KEY);
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

    private DilutionAssayRun _getNabAssayRun(ExpRun run, DilutionCurve.FitType fit, User elevatedUser) throws ExperimentException
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
                    getViewContext().getSession().setAttribute(LAST_NAB_RUN_KEY, new NAbRunWrapper(assay, new Date()));
            }
            catch (SinglePlateNabDataHandler.MissingDataFileException e)
            {
                throw new NotFoundException(e.getMessage());
            }
        }
        return assay;
    }

    private static final String LAST_NAB_RUN_KEY = NabAssayController.class.getName() + "/LastNAbRun";

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class DetailsAction extends RunDetailsAction<RenderAssayBean>
    {
        @Override
        protected DilutionAssayRun getNabAssayRun(ExpRun run, DilutionCurve.FitType fit, User user) throws ExperimentException
        {
            return _getNabAssayRun(run, fit, user);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getContainer(), _protocol, _runRowId);
            return root.addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _runRowId + " Details");
        }
    }

    public static class GraphSelectedForm
    {
        private int _protocolId;
        private int[] _id;
        private String _captionColumn;
        private String _chartTitle;
        private DilutionCurve.FitType _fitType;
        private int _height = -1;
        private int _width = -1;

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }

        public int getProtocolId()
        {
            return _protocolId;
        }

        public void setProtocolId(int protocolId)
        {
            _protocolId = protocolId;
        }

        public String getCaptionColumn()
        {
            return _captionColumn;
        }

        public void setCaptionColumn(String captionColumn)
        {
            _captionColumn = captionColumn;
        }

        public String getChartTitle()
        {
            return _chartTitle;
        }

        public void setChartTitle(String chartTitle)
        {
            _chartTitle = chartTitle;
        }

        public String getFitType()
        {
            return _fitType != null ? _fitType.name() : null;
        }

        public void setFitType(String fitType)
        {
            _fitType = fitType != null ? DilutionCurve.FitType.valueOf(fitType) : null;
        }

        public DilutionCurve.FitType getFitTypeEnum()
        {
            return _fitType;
        }

        public int getHeight()
        {
            return _height;
        }

        public void setHeight(int height)
        {
            _height = height;
        }

        public int getWidth()
        {
            return _width;
        }

        public void setWidth(int width)
        {
            _width = width;
        }
    }

    public static class GraphSelectedBean
    {
        private ViewContext _context;
        private int[] _cutoffs;
        private ExpProtocol _protocol;
        private int[] _dataObjectIds;
        private QueryView _queryView;
        private int[] _graphableIds;
        private String _captionColumn;
        private String _chartTitle;

        public GraphSelectedBean(ViewContext context, ExpProtocol protocol, int[] cutoffs, int[] dataObjectIds, String captionColumn, String chartTitle)
        {
            _context = context;
            _cutoffs = cutoffs;
            _protocol = protocol;
            _dataObjectIds = dataObjectIds;
            _captionColumn = captionColumn;
            _chartTitle = chartTitle;
        }

        public int[] getCutoffs()
        {
            return _cutoffs;
        }

        public ExpProtocol getProtocol()
        {
            return _protocol;
        }

        public String getCaptionColumn()
        {
            return _captionColumn;
        }

        public String getChartTitle()
        {
            return _chartTitle;
        }

        public int[] getGraphableObjectIds() throws IOException, SQLException
        {
            if (_graphableIds == null)
            {
                QueryView dataView = getQueryView();
                ResultSet rs = null;
                try
                {
                    rs = dataView.getResultSet();
                    Set<Integer> graphableIds = new HashSet<Integer>();
                    while (rs.next())
                        graphableIds.add(rs.getInt("RowId"));
                    _graphableIds = new int[graphableIds.size()];
                    int i = 0;
                    for (Integer id : graphableIds)
                        _graphableIds[i++] = id.intValue();
                }
                finally
                {
                    if (rs != null) try { rs.close(); } catch (SQLException e) {}
                }
            }
            return _graphableIds;
        }

        public QueryView getQueryView()
        {
            if (_queryView == null)
            {
                NabAssayProvider provider = (NabAssayProvider)AssayService.get().getProvider(_protocol);
                NabProtocolSchema schema = provider.createProtocolSchema(_context.getUser(), _context.getContainer(), _protocol, null);
                QuerySettings settings = schema.getSettings(_context, AssayProtocolSchema.DATA_TABLE_NAME, AssayProtocolSchema.DATA_TABLE_NAME);
                QueryView dataView = new NabProtocolSchema.NabResultsQueryView(_protocol, _context, settings)
                {
                    public DataView createDataView()
                    {
                        DataView view = super.createDataView();
                        SimpleFilter filter = new SimpleFilter();
                        SimpleFilter existingFilter = (SimpleFilter) view.getRenderContext().getBaseFilter();
                        if (existingFilter != null)
                            filter.addAllClauses(existingFilter);
                        List<Integer> objectIds = new ArrayList<Integer>(_dataObjectIds.length);
                        for (int dataObjectId : _dataObjectIds)
                            objectIds.add(new Integer(dataObjectId));

                        filter.addInClause(FieldKey.fromString("RowId"), objectIds);
                        view.getDataRegion().setRecordSelectorValueColumns("RowId");
                        view.getRenderContext().setBaseFilter(filter);
                        return view;
                    }
                };
                _queryView = dataView;
            }
            return _queryView;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class GraphSelectedAction extends SimpleViewAction<GraphSelectedForm>
    {
        private ExpProtocol _protocol;
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            _protocol = ExperimentService.get().getExpProtocol(form.getProtocolId());
            if (_protocol == null)
            {
                throw new NotFoundException();
            }
            int[] objectIds;
            if (form.getId() != null)
                objectIds = form.getId();
            else
            {
                Set<String> objectIdStrings = DataRegionSelection.getSelected(getViewContext(), false);
                if (objectIdStrings == null || objectIdStrings.size() == 0)
                {
                    throw new NotFoundException("No samples specified.");
                }
                objectIds = new int[objectIdStrings.size()];
                int idx = 0;
                for (String objectIdString : objectIdStrings)
                    objectIds[idx++] = Integer.parseInt(objectIdString);
            }

            Set<Integer> cutoffSet = new HashSet<Integer>();
            NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(_protocol);
            Map<DilutionSummary, DilutionAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getUser(), form.getFitTypeEnum(), objectIds);
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }

            GraphSelectedBean bean = new GraphSelectedBean(getViewContext(), _protocol, toArray(cutoffSet), objectIds, form.getCaptionColumn(), form.getChartTitle());

            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/multiRunGraph.jsp", bean);

            return new VBox(new AssayHeaderView(_protocol, provider, false, true, null), multiGraphView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _protocol);
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild("Graph Selected Specimens");
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

    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteRunAction extends SimpleViewAction<DeleteRunForm>
    {
        public ModelAndView getView(DeleteRunForm deleteRunForm, BindException errors) throws Exception
        {
            if (deleteRunForm.getRowId() == 0)
            {
                throw new NotFoundException("No run specified");
            }
            ExpRun run = ExperimentService.get().getExpRun(deleteRunForm.getRowId());
            if (run == null)
                throw new NotFoundException("Run " + deleteRunForm.getRowId() + " does not exist.");
            File file = null;
            if (deleteRunForm.isReupload())
            {
                file = getDataHandler(run).getDataFile(run);
                if (file == null)
                {
                    throw new NotFoundException("Data file for run " + run.getName() + " was not found.  Deleted from the file system?");
                }
            }

            run.delete(getUser());

            if (deleteRunForm.isReupload())
            {
                ActionURL reuploadURL = new ActionURL(NabUploadWizardAction.class, getContainer());
                reuploadURL.addParameter("dataFile", file.getPath());
                throw new RedirectException(reuploadURL);
            }
            throw new RedirectException(PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), run.getProtocol()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException("Expected redirect did not occur.");
        }
    }

    private int[] toArray(Collection<Integer> integerList)
    {
        int[] arr = new int[integerList.size()];
        int i = 0;
        for (Integer cutoff : integerList)
            arr[i++] = cutoff.intValue();
        return arr;
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class MultiGraphAction extends SimpleViewAction<GraphSelectedForm>
    {
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            int[] ids = form.getId();
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(form.getProtocolId());
            NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
            Map<DilutionSummary, DilutionAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getUser(), form.getFitTypeEnum(), ids);
            Set<Integer> cutoffSet = new HashSet<Integer>();
            for (DilutionSummary summary : summaries.keySet())
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }

            NabGraph.Config config = new NabGraph.Config();
            config.setCutoffs(toArray(cutoffSet));
            config.setLockAxes(false);
            config.setCaptionColumn(form.getCaptionColumn());
            config.setChartTitle(form.getChartTitle());
            if (form.getHeight() > 0)
                config.setHeight(form.getHeight());
            if (form.getWidth() > 0)
            config.setWidth(form.getWidth());
            NabGraph.renderChartPNG(getViewContext().getResponse(), summaries, config);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class GraphAction extends SimpleViewAction<GraphForm>
    {
        public ModelAndView getView(GraphForm form, BindException errors) throws Exception
        {
            if (form.getRowId() == -1)
                throw new NotFoundException("Run ID not specified.");
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");

            // See comment in DetailsAction about the elevatedUser
            User elevatedUser = getUser();
            if (!getContainer().hasPermission(getUser(), ReadPermission.class))
            {
                User currentUser = getUser();
                Set<Role> contextualRoles = new HashSet<Role>(currentUser.getStandardContextualRoles());
                contextualRoles.add(RoleManager.getRole(ReaderRole.class));
                elevatedUser = new LimitedUser(currentUser, currentUser.getGroups(), contextualRoles, false);
            }
            DilutionAssayRun assay = _getNabAssayRun(run, form.getFitTypeEnum(), elevatedUser);
            if (assay == null)
                throw new NotFoundException("Could not load NAb results for run " + form.getRowId() + ".");

            NabGraph.Config config = new NabGraph.Config();
            config.setCutoffs(assay.getCutoffs());
            config.setLockAxes(assay.isLockAxes());
            config.setFirstSample(form.getFirstSample());
            config.setMaxSamples(form.getMaxSamples());
            if (form.getHeight() > 0)
                config.setHeight(form.getHeight());
            if (form.getWidth() > 0)
                config.setWidth(form.getWidth());

            NabGraph.renderChartPNG(getViewContext().getResponse(), assay, config);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
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
        private Container _container;
        private User _user;
        private Domain _sampleDomain;
        private List<WellGroupTemplate> _sampleGroups;

        public SampleTemplateWriter(Container container, User user, Domain sampleDomain, List<WellGroupTemplate> sampleGroups)
        {
            _sampleDomain = sampleDomain;
            _sampleGroups = sampleGroups;
            _container = container;
            _user = user;
        }

        @Override
        public void renderSheet(int sheetNumber)
        {
            Sheet sheet = _workbook.createSheet(getSheetName(sheetNumber));
            sheet.getPrintSetup().setPaperSize(PrintSetup.LETTER_PAPERSIZE);

            // Render the header row:
            List<String> headers = new ArrayList<String>();
            headers.add(PlateSampleFilePropertyHelper.WELLGROUP_COLUMN);
            headers.add(PlateSampleFilePropertyHelper.PLATELOCATION_COLUMN);

            Map<DomainProperty, Object> defaultValues = DefaultValueService.get().getDefaultValues(_container, _sampleDomain);
            Map<Integer, Object> columnToDefaultValue = new HashMap<Integer, Object>();
            for (DomainProperty property : _sampleDomain.getProperties())
            {
                columnToDefaultValue.put(headers.size(), defaultValues.get(property));
                headers.add(property.getName());
            }

            Row firstRow = sheet.createRow(0);
            for (int column = 0; column < headers.size(); column++)
            {
                String header = headers.get(column);
                Cell cell = firstRow.getCell(column, Row.CREATE_NULL_AS_BLANK);
                cell.setCellValue(header);
                cell.setCellStyle(getBoldFormat());
            }

            // Render the rows, which just contain well group names:
            for (int group = 0; group < _sampleGroups.size(); group++)
            {
                Row row = sheet.createRow(group + 1);
                WellGroupTemplate sample = _sampleGroups.get(group);
                row.createCell(0).setCellValue(sample.getName());
                row.createCell(1).setCellValue(sample.getPositionDescription());
                for (int column = 2; column < headers.size(); column++)
                {
                    Object defaultValue = columnToDefaultValue.get(column);
                    if (defaultValue != null)
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
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SampleSpreadsheetTemplateAction extends ExportAction<SampleSpreadsheetForm>
    {
        @Override
        public void export(SampleSpreadsheetForm sampleSpreadsheetForm, HttpServletResponse response, BindException errors) throws Exception
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
                String message = "Protocol " + sampleSpreadsheetForm.getProtocol() + " is not a NAb protocol: " +
                        (protocol != null ? protocol.getName() : "null");
                throw new NotFoundException(message);
            }
            NabAssayProvider nabProvider = ((NabAssayProvider) provider);
            Domain sampleDomain = nabProvider.getSampleWellGroupDomain(protocol);
            PlateTemplate template = nabProvider.getPlateTemplate(context.getContainer(), protocol);
            if (template == null)
            {
                throw new NotFoundException("The plate template for this assay design could not be found.  It may have been deleted by an administrator.");
            }
            List<WellGroupTemplate> sampleGroups = new ArrayList<WellGroupTemplate>();
            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.SPECIMEN)
                    sampleGroups.add(group);
            }

            ExcelWriter xl = new SampleTemplateWriter(getViewContext().getContainer(), getUser(), sampleDomain, sampleGroups);
            xl.setFilenamePrefix("metadata");
            xl.write(response);
        }
    }

    /** Avoid serializing the NAb run, since many of its child objects aren't serializable themselves */
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
}
