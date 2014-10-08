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

package org.labkey.nab;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.assay.dilution.DilutionAssayRun;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.assay.nab.RenderAssayBean;
import org.labkey.api.assay.nab.RenderAssayForm;
import org.labkey.api.assay.nab.view.DilutionGraphAction;
import org.labkey.api.assay.nab.view.GraphSelectedAction;
import org.labkey.api.assay.nab.view.GraphSelectedBean;
import org.labkey.api.assay.nab.view.GraphSelectedForm;
import org.labkey.api.assay.nab.view.MultiGraphAction;
import org.labkey.api.assay.nab.view.RunDetailsAction;
import org.labkey.api.data.Container;
import org.labkey.api.data.ExcelWriter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.statistics.StatsService;
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
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.PlateSampleFilePropertyHelper;
import org.labkey.api.study.assay.RunDataSetContextualRoles;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
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
                    getViewContext().getSession().setAttribute(LAST_NAB_RUN_KEY, new NAbRunWrapper(assay, new Date()));
            }
            catch (DilutionDataHandler.MissingDataFileException e)
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
        protected DilutionAssayRun getNabAssayRun(ExpRun run, StatsService.CurveFitType fit, User user) throws ExperimentException
        {
            return _getNabAssayRun(run, fit, user);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(getContainer(), _protocol, _runRowId);
            return root.addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _runRowId + " Details");
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
                public DataView createDataView()
                {
                    DataView view = super.createDataView();
                    SimpleFilter filter = new SimpleFilter();
                    SimpleFilter existingFilter = (SimpleFilter) view.getRenderContext().getBaseFilter();
                    if (existingFilter != null)
                        filter.addAllClauses(existingFilter);
                    List<Integer> objectIds = new ArrayList<>(_dataObjectIds.length);
                    for (int dataObjectId : _dataObjectIds)
                        objectIds.add(new Integer(dataObjectId));

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

    @RequiresPermissionClass(ReadPermission.class)
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
    public class NabMultiGraphAction extends MultiGraphAction<GraphSelectedForm>
    {
    }

    @RequiresPermissionClass(ReadPermission.class)
    @ContextualRoles(RunDataSetContextualRoles.class)
    public class GraphAction extends DilutionGraphAction
    {
        @Override
        protected User getGraphUser()
        {
            // See comment in DetailsAction about the elevatedUser
            User elevatedUser = getUser();
            if (!getContainer().hasPermission(getUser(), ReadPermission.class))
            {
                User currentUser = getUser();
                Set<Role> contextualRoles = new HashSet<>(currentUser.getStandardContextualRoles());
                contextualRoles.add(RoleManager.getRole(ReaderRole.class));
                elevatedUser = new LimitedUser(currentUser, currentUser.getGroups(), contextualRoles, false);
            }
            return elevatedUser;
        }

        @Override
        protected DilutionAssayRun getAssayRun(ExpRun run, StatsService.CurveFitType fit, User user) throws ExperimentException
        {
            return _getNabAssayRun(run, fit, user);
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
        private Domain _virusDomain;
        private List<WellGroupTemplate> _virusGroups;

        public SampleTemplateWriter(Container container, User user, Domain sampleDomain, List<WellGroupTemplate> sampleGroups, Domain virusDomain, List<WellGroupTemplate> virusGroups)
        {
            _sampleDomain = sampleDomain;
            _sampleGroups = sampleGroups;
            _virusDomain = virusDomain;
            _virusGroups = virusGroups;
            _container = container;
            _user = user;
        }

        @Override
        public void renderSheet(int sheetNumber)
        {
            Sheet sheet = _workbook.createSheet(getSheetName(sheetNumber));
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
                Cell cell = firstRow.getCell(column, Row.CREATE_NULL_AS_BLANK);
                cell.setCellValue(header);
                cell.setCellStyle(getBoldFormat());
            }

            // Render the rows (i.e. well group names, virus group names, and default property values):
            int rowNum = 1;
            for (WellGroupTemplate sampleGroup : _sampleGroups)
            {
                if (_virusGroups.size() > 0)
                {
                    for (WellGroupTemplate virusGroup : _virusGroups)
                        renderRow(sheet, headers, columnToDefaultValue, rowNum++, sampleGroup, virusGroup);
                }
                else
                    renderRow(sheet, headers, columnToDefaultValue, rowNum++, sampleGroup, null);
            }
        }

        private void renderRow(Sheet sheet, List<String> headers, Map<String, Object> columnToDefaultValue, int rowNum, WellGroupTemplate sample, @Nullable WellGroupTemplate virus)
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
            Domain virusDomain = nabProvider.getVirusWellGroupDomain(protocol);
            PlateTemplate template = nabProvider.getPlateTemplate(context.getContainer(), protocol);
            if (template == null)
            {
                throw new NotFoundException("The plate template for this assay design could not be found.  It may have been deleted by an administrator.");
            }

            List<WellGroupTemplate> sampleGroups = new ArrayList<>();
            List<WellGroupTemplate> virusGroups = new ArrayList<>();
            for (WellGroupTemplate group : template.getWellGroups())
            {
                if (group.getType() == WellGroup.Type.SPECIMEN)
                    sampleGroups.add(group);
                else if (group.getType() == WellGroup.Type.VIRUS)
                    virusGroups.add(group);
            }

            ExcelWriter xl = new SampleTemplateWriter(getContainer(), getUser(), sampleDomain, sampleGroups, virusDomain, virusGroups);
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
