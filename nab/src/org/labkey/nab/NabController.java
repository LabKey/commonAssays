/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

import jxl.read.biff.BiffException;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.announcements.DiscussionService;
import org.labkey.api.assay.dilution.DilutionCurve;
import org.labkey.api.attachments.AttachmentFile;
import org.labkey.api.attachments.AttachmentForm;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.SpringAttachmentFile;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.reports.report.view.ReportUtil;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.ParticipantVisit;
import org.labkey.api.study.Plate;
import org.labkey.api.study.PlateQueryView;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.SpecimenService;
import org.labkey.api.study.Study;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.WellData;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.assay.AssayPublishService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewForm;
import org.labkey.api.view.template.PageConfig.Template;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class NabController extends SpringActionController
{
    private static final Logger _log = Logger.getLogger(NabController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(NabController.class);

    public NabController()
    {
        setActionResolver(_actionResolver);
    }


    private ActionURL getBeginURL()
    {
        return new ActionURL(BeginAction.class, getContainer());
    }


    private ActionURL getErrorURL(String error)
    {
        ActionURL url = getBeginURL();
        url.addParameter("error", error);
        return url;
    }


    private ActionURL getDisplayURL(Luc5Assay assay, boolean newRun, boolean printView)
    {
        return getDisplayURL(getContainer(), assay, newRun, printView);
    }


    private static ActionURL getDisplayURL(Container c, Luc5Assay assay, boolean newRun, boolean printView)
    {
        return getDisplayURL(c, assay.getRunRowId().intValue(), newRun, printView);
    }


    private static ActionURL getDisplayURL(Container c, int id, boolean newRun, boolean printView)
    {
        ActionURL url = new ActionURL(DisplayAction.class, c);
        url.addParameter("rowId", Integer.toString(id));

        if (newRun)
            url.addParameter("newRun", true);

        if (printView)
            url.addParameter("print", true);

        return url;
    }


    private ActionURL getRunsURL()
    {
        return new ActionURL(RunsAction.class, getContainer());
    }


    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction<CreateForm>
    {
        public ModelAndView getView(CreateForm form, BindException errors) throws Exception
        {
            VBox box = new VBox();

            box.addView(AssayService.get().createAssayListView(getViewContext(), false));
            box.addView(new HtmlView(PageFlowUtil.textLink("Deprecated NAb Run", new ActionURL(CreateAction.class, getContainer()))));

            return box;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Assays");
        }
    }

    @RequiresNoPermission
    public class CreateAction extends SimpleViewAction<CreateForm>
    {
        public ModelAndView getView(CreateForm form, BindException errors) throws Exception
        {
            if (!getUser().isGuest() && !getContainer().hasPermission(getUser(), InsertPermission.class))
            {
                throw new RedirectException(getRunsURL());
            }

            if (!getContainer().hasPermission(getUser(), InsertPermission.class))
            {
                throw new UnauthorizedException();
            }

            UploadAssayForm assayForm;

            if (form.isReset())
            {
                assayForm = new UploadAssayForm(true);
                OldNabManager.get().saveAsLastInputs(getViewContext(), null);
            }
            else
            {
                assayForm = OldNabManager.get().getLastInputs(getViewContext());
                if (form.getPlateTemplate() != null)
                    assayForm.setPlateTemplate(form.getPlateTemplate(), getContainer(), getUser());
            }

            return addHeaderView(new EditParametersView(assayForm));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Create NAb Run");
        }
    }

    public static class CreateForm
    {
        private boolean _reset;
        private String _plateTemplate;

        public boolean isReset()
        {
            return _reset;
        }

        public void setReset(boolean reset)
        {
            _reset = reset;
        }

        public String getPlateTemplate()
        {
            return _plateTemplate;
        }

        public void setPlateTemplate(String plateTemplate)
        {
            _plateTemplate = plateTemplate;
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class EditRunParametersAction extends SimpleViewAction<UploadAssayForm>
    {
        public ModelAndView getView(UploadAssayForm form, BindException errors) throws Exception
        {
            return addHeaderView(new EditParametersView(form));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Create NAb Run");
        }
    }


    private static class EditParametersView extends JspView<UploadAssayForm>
    {
        private EditParametersView(UploadAssayForm form)
        {
            super("/org/labkey/nab/editRunProperties.jsp", form);
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class UploadAction extends SimpleRedirectAction<UploadAssayForm>
    {
        @Override
        public void validate(UploadAssayForm form, BindException errors)
        {
            form.setDatafile(new SpringAttachmentFile(getFileMap().get("dataFile")));
            form.validate(getUser(), getContainer(), errors);
        }


        public ActionURL getRedirectURL(UploadAssayForm form) throws Exception
        {
            for (int i = 0; i < form.getSampleInfos().length; i++)
            {
                SampleInfo info = form.getSampleInfos()[i];
                if (i > 0)
                {
                    if (form.getRunSettings().isSameMethod())
                        info.setMethodName(form.getSampleInfos()[0].getMethodName());
                    if (form.getRunSettings().isSameFactor())
                        info.setFactor(form.getSampleInfos()[0].getFactor());
                    if (form.getRunSettings().isSameInitialValue())
                        info.setInitialDilution(form.getSampleInfos()[0].getInitialDilution());
                }
            }

            AttachmentFile datafile = form.getDataFile();

            SafeTextConverter.PercentConverter[] possibleCutoffs = form.getRunSettings().getCutoffs();
            Set<Integer> cutoffSet = new HashSet<Integer>();
            // eliminate duplicates and blank values:
            for (SafeTextConverter.PercentConverter possibleCutoff : possibleCutoffs)
            {
                if (possibleCutoff.getValue() != null)
                    cutoffSet.add(possibleCutoff.getValue());
            }
            int[] cutoffs = new int[cutoffSet.size()];
            int idx = 0;
            for (Integer cutoff : cutoffSet)
                cutoffs[idx++] = cutoff.intValue();

            OldNabManager.get().saveAsLastInputs(getViewContext(), form);
            OldNabAssayRun assay;
            try
            {
                assay = OldNabManager.get().saveResults(getContainer(), getUser(),
                        form.getPlateTemplate(), form.getMetadata(),
                        form.getSampleInfos(), cutoffs, datafile);
            }
            catch (BiffException e)
            {
                // this is a hacky way to deal with a file format problem, but it lets us get away with
                // only reading the xls once, rather than the double-read that would be required to validate
                // the file format in our validate method.
                return getErrorURL("Data file format error: " + e.getMessage());
            }
            catch (IOException e)
            {
                // this is a hacky way to deal with a file format problem, but it lets us get away with
                // only reading the xls once, rather than the double-read that would be required to validate
                // the file format in our validate method.
                return getErrorURL(e.getMessage());
            }

            cacheAssay(assay);

            return getDisplayURL(assay, true, false);
        }
    }


    public static class UploadAssayForm extends ViewForm
    {
        private SampleInfo[] _sampleInfos;
        private String _fileName;
        private RunMetadata _metadata = new RunMetadata();
        private RunSettings _runSettings;
        private String _plateTemplate;
        private AttachmentFile _datafile;

        public UploadAssayForm()
        {
            this(false);
        }

        public UploadAssayForm(boolean returnDefaultForUnsetBools)
        {
            _runSettings = new RunSettings(returnDefaultForUnsetBools);
            reset(getViewContext().getContainer(), getViewContext().getUser());
        }


        public void validate(User user, Container c, BindException errors)
        {
            AttachmentFile dataFile = getDataFile();

            if (null == _fileName && null == dataFile)
                errors.reject("main", "Please upload a file.");

            List<String> templateErrors = OldNabManager.get().isValidNabPlateTemplate(c, user, getPlateTemplate());
            for (String templateError : templateErrors)
                errors.reject("main", templateError);

            if (getMetadata().getExperimentDateString() != null)
            {
                try
                {
                    long dateTime = DateUtil.parseDateTime(getMetadata().getExperimentDateString());
                    getMetadata().setExperimentDate(new Date(dateTime));
                }
                catch (ConversionException e)
                {
                    errors.reject("main", "Could not parse experiment date: " +
                            getMetadata().getExperimentDateString() + ". Please re-enter in a standard date format.");
                }
            }

            SafeTextConverter.PercentConverter[] cutoffs = getRunSettings().getCutoffs();
            for (SafeTextConverter.PercentConverter cutoff : cutoffs)
            {
                if (cutoff.getValue() == null && cutoff.getText() != null)
                    errors.reject("main", cutoff.getText() + " is not a valid cutoff value.");
                if (cutoff.getValue() != null && (cutoff.getValue().intValue() < -100 || cutoff.getValue().intValue() > 200))
                    errors.reject("main", "Cutoff percentages must be between -100 and 200 percent.  " + cutoff.getText() + " is not a valid cutoff value.");
            }

            if (dataFile != null && getRunSettings().isInferFromFile())
            {
                String filename = dataFile.getFilename();

                if (filename.toLowerCase().endsWith(".xls"))
                    getMetadata().setFileId(filename.substring(0, filename.length() - 4));
                else
                    getMetadata().setFileId(filename);
                int pos;
                // trim down to the numeric portion of the filename, eliminating leading characters
                // and anything following a dot or semicolon:
                if ((pos = filename.indexOf(';')) >= 0)
                    filename = filename.substring(0, pos);
                if ((pos = filename.indexOf('.')) >= 0)
                    filename = filename.substring(0, pos);
                pos = 0;
                while (pos < filename.length() && !Character.isDigit(filename.charAt(pos)))
                    pos++;
                if (pos < filename.length())
                {
                    filename = filename.substring(pos);
                    int len = filename.length();
                    // we expect an eight digit date: four digits for the year, two each for day/month:
                    if (len == 8)
                    {
                        String dateString = filename.substring(0, 4) + "-" + filename.substring(4, 6) +
                                "-" + filename.substring(6, 8);
                        try
                        {
                            Date date = DateUtil.parseDateTime(dateString, "yyyy-MM-dd");
                            getMetadata().setExperimentDate(date);
                        }
                        catch (ParseException e)
                        {
                            errors.reject("main", "Could not parse experiment date from filename.");
                        }
                    }
                    else if (len == 6)
                    {
                        String dateString = filename.substring(0, 2) + "-" + filename.substring(2, 4) +
                                "-" + filename.substring(4, 6);
                        try
                        {
                            Date date = DateUtil.parseDateTime(dateString, "yy-MM-dd");
                            getMetadata().setExperimentDate(date);
                        }
                        catch (ParseException e)
                        {
                            errors.reject("main", "Could not parse experiment date from filename.");
                        }
                    }
                    else
                        errors.reject("main", "Filename was an unxpected length; cannot infer experiment properties.");
                }
                else
                    errors.reject("main", "Filename was in an unexpected format; cannot infer experiment properties.");
            }

            boolean missingSampleId = false;
            for (SampleInfo info : _sampleInfos)
            {
                if (info.getSampleId() == null || info.getSampleId().length() == 0)
                    missingSampleId = true;
                else
                {
                    if (info.getInitialDilution() == null || info.getInitialDilution() < 0)
                        errors.reject("main", info.getSampleId() + " initial dilution/concentration is invalid.");
                    if (info.getFactor() == null)
                        errors.reject("main", info.getSampleId() + " dilution/concentration factor is invalid.");
                }
            }
            if (missingSampleId)
                errors.reject("main", "All samples must be given an ID.");
        }

        public void reset(Container c, User user)
        {
            resetSpecimens(c, user);
            resetDefaultProperties(c, user);
        }

        private void resetSpecimens(Container c, User user)
        {
            PlateTemplate template = getActivePlateTemplate(c, user);
            int specimenCount = template.getWellGroupCount(WellGroup.Type.SPECIMEN);
            _sampleInfos = new SampleInfo[specimenCount];
            for (int i = 0; i < _sampleInfos.length; i++)
                _sampleInfos[i] = new SampleInfo(null);
        }

        public PlateTemplate getActivePlateTemplate(Container container, User user)
        {
            try
            {
                PlateTemplate template = null;
                if (_plateTemplate != null)
                    template = PlateService.get().getPlateTemplate(container, _plateTemplate);

                if (template == null)
                {
                    template = OldNabManager.get().ensurePlateTemplate(container, user);
                    _plateTemplate = template.getName();
                }
                return template;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public PlateTemplate[] getPlateTemplates(Container container, User user)
        {
            try
            {
                PlateTemplate[] templates = PlateService.get().getPlateTemplates(container);
                if (templates == null || templates.length == 0)
                {
                    PlateTemplate defaultTemplate = OldNabManager.get().ensurePlateTemplate(container, user);
                    templates = new PlateTemplate[] { defaultTemplate };
                }
                return templates;
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }

        public String getPlateTemplate()
        {
            return _plateTemplate;
        }

        public void setPlateTemplate(String plateTemplate, Container c, User user)
        {
            String previous = _plateTemplate;
            _plateTemplate = plateTemplate;

            if (!plateTemplate.equals(previous))
            {
                resetSpecimens(c, user);
                resetDefaultProperties(c, user);
            }
        }

        private void resetDefaultProperties(Container c, User user)
        {   
            PlateTemplate template = getActivePlateTemplate(c, user);
            Object experimentDate = template.getProperty("ExperimentDate");
            if (experimentDate instanceof Date)
                getMetadata().setExperimentDate((Date) experimentDate);
            else
                getMetadata().setExperimentDateString(experimentDate != null ? experimentDate.toString() : null);
            getMetadata().setExperimentId((String)template.getProperty("ExperimentId"));
            getMetadata().setExperimentPerformer((String)template.getProperty("ExperimentPerformer"));
            getMetadata().setFileId((String)template.getProperty("FileId"));
            getMetadata().setHostCell((String)template.getProperty("HostCell"));
            getMetadata().setIncubationTime((String)template.getProperty("IncubationTime"));
            getMetadata().setPlateNumber((String)template.getProperty("PlateNumber"));
            getMetadata().setStudyName((String)template.getProperty("StudyName"));
            getMetadata().setVirusId((String)template.getProperty("VirusId"));
            getMetadata().setVirusName((String)template.getProperty("VirusName"));
        }

        public SampleInfo[] getSampleInfos()
        {
            return _sampleInfos;
        }

        public void setSampleInfos(SampleInfo[] sampleInfos)
        {
            _sampleInfos = sampleInfos;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }

        public RunMetadata getMetadata()
        {
            return _metadata;
        }

        public void setMetadata(RunMetadata metadata)
        {
            _metadata = metadata;
        }

        public RunSettings getRunSettings()
        {
            return _runSettings;
        }

        public void setRunSettings(RunSettings runSettings)
        {
            _runSettings = runSettings;
        }

        public AttachmentFile getDataFile()
        {
            return _datafile;
        }

        public void setDatafile(AttachmentFile datafile)
        {
            _datafile = datafile;
        }
    }


    public static class RenderAssayForm extends RowIdForm
    {
        private boolean _print;
        private boolean _newRun;

        public boolean isPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }

        public boolean isNewRun()
        {
            return _newRun;
        }

        public void setNewRun(boolean newRun)
        {
            _newRun = newRun;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class DisplayAction extends SimpleViewAction<RenderAssayForm>
    {
        public ModelAndView getView(RenderAssayForm form, BindException errors) throws Exception
        {
            if (form.getRowId() < 0)
            {
                throw new RedirectException(getBeginURL());
            }

            OldNabAssayRun assay = getCachedAssay(form.getRowId());
            if (assay == null)
                return new HtmlView("The requested assay run is no longer available.");

            JspView<RenderAssayBean> assayView = new JspView<RenderAssayBean>("/org/labkey/nab/runResults.jsp", new RenderAssayBean(assay, form.isNewRun(), form.isPrint()));

            if (form.isPrint())
            {
                getPageConfig().setTemplate(Template.Print);
                setTitle("NAB Run Details: " + assay.getName());
                return assayView;
            }
            else
            {
                ActionURL printURL = getDisplayURL(assay, false, true);
                setTitle("NAB Run Details");
                return addHeaderView(assayView, printURL, assay.getPlate());
            }

        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    public static class RenderAssayBean
    {
        private OldNabAssayRun _assay;
        private boolean _newRun;
        private boolean _printView;

        public RenderAssayBean(OldNabAssayRun assay, boolean newRun, boolean printView)
        {
            _assay = assay;
            _newRun = newRun;
            _printView = printView;
        }

        public OldNabAssayRun getAssay()
        {
            return _assay;
        }

        public boolean isNewRun()
        {
            return _newRun;
        }

        public PlateQueryView getDuplicateDataFileView(ViewContext context, OldNabAssayRun assay)
        {
            SimpleFilter filter = new SimpleFilter("Property/DataFile", assay.getPlate().getProperty("DataFile"));
            filter.addCondition("RowId", assay.getRunRowId(), CompareType.NEQ);
            PlateQueryView duplicateDataFileView = PlateService.get().getPlateGridView(context, filter);
            duplicateDataFileView.setShowExportButtons(false);
            ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
            selectButton.setDisplayPermission(InsertPermission.class);
            List<ActionButton> buttons = new ArrayList<ActionButton>();
            buttons.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(InsertPermission.class);
            buttons.add(clearButton);

            ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete", DataRegion.MODE_GRID, ActionButton.Action.POST);
            deleteButton.setRequiresSelection(true);
            deleteButton.setDisplayPermission(DeletePermission.class);
            buttons.add(deleteButton);
            duplicateDataFileView.setButtons(buttons);
            duplicateDataFileView.addHiddenFormField("runId", "" + assay.getRunRowId());
            duplicateDataFileView.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
            return duplicateDataFileView;
        }

        public boolean isPrintView()
        {
            return _printView;
        }

        public HttpView getDiscussionView(ViewContext context)
        {
            ActionURL pageUrl = getDisplayURL(_assay.getPlate().getContainer(), _assay, _newRun, _printView);
            String discussionTitle = "Discuss Run " + _assay.getRunRowId() + ": " + _assay.getName();
            String entityId = _assay.getPlate().getEntityId();
            DiscussionService.Service service = DiscussionService.get();
            return service.getDisussionArea(context,
                    entityId, pageUrl, discussionTitle, true, false);
        }
    }


    private ModelAndView addHeaderView(HttpView view) throws Exception
    {
        return addHeaderView(view, null, null);
    }


    private ModelAndView addHeaderView(HttpView view, ActionURL printLink, Plate dataFilePlate) throws Exception
    {
        JspView<HeaderBean> headerView = new JspView<HeaderBean>("/org/labkey/nab/header.jsp",
                new HeaderBean(getViewContext(), printLink,
                        dataFilePlate != null ? OldNabManager.get().getDataFileDownloadLink(dataFilePlate) : null));
        return new VBox(headerView, view);
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


    private OldNabAssayRun getCachedAssay(int rowId) throws Exception
    {
        HttpSession session = getViewContext().getRequest().getSession(true);
        OldNabAssayRun assay = (OldNabAssayRun)session.getAttribute("nabAssay");

        if (assay == null || assay.getRunRowId().intValue() != rowId)
        {
            assay = OldNabManager.get().loadFromDatabase(getUser(), getContainer(), rowId);
            session.setAttribute("nabAssay", assay);
        }

        return assay;
    }


    private void cacheAssay(OldNabAssayRun assay)
    {
        HttpSession session = getViewContext().getRequest().getSession(true);
        session.setAttribute("nabAssay", assay);
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class RenderChartAction extends ExportAction<RowIdForm>
    {
        public void export(RowIdForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            Luc5Assay assay = getCachedAssay(form.getRowId());
            if (assay == null)
                ReportUtil.renderErrorImage(response.getOutputStream(), 425, 300, "The requested assay run is no longer available");
            else
                renderChartPNG(response, assay.getSummaries(), assay.getCutoffs());
        }
    }


    private static final Color[] GRAPH_COLORS = {
            ChartColor.BLUE,
            ChartColor.RED,
            ChartColor.DARK_GREEN,
            ChartColor.DARK_YELLOW,
            ChartColor.MAGENTA
    };


    private void renderChartPNG(HttpServletResponse response, DilutionSummary[] dilutionSummaries, int[] cutoffs) throws IOException, DilutionCurve.FitFailedException
    {
        XYSeriesCollection curvesDataset = new XYSeriesCollection();
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, "Percentage", curvesDataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setDataset(1, pointDataset);
        plot.getRenderer(0).setStroke(new BasicStroke(1.5f));
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(true, true);
        plot.setRenderer(1, pointRenderer);
        pointRenderer.setStroke(new BasicStroke(
                0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[]{4.0f, 4.0f}, 0.0f));
        plot.getRenderer(0).setSeriesVisibleInLegend(false);
        pointRenderer.setShapesFilled(true);
        for (DilutionSummary summary : dilutionSummaries)
        {
            String sampleId = summary.getSampleId();
            if (sampleId == null)
                sampleId = "[no sample id]";
            XYSeries pointSeries = new XYSeries(sampleId);
            for (WellData well : summary.getWellData())
            {
                double percentage = 100 * summary.getPercent(well);
                double dilution = summary.getDilution(well);
                pointSeries.add(dilution, percentage);
            }
            pointDataset.addSeries(pointSeries);
            int pointDatasetCount = pointDataset.getSeriesCount();
            Color currentColor;
            if (pointDatasetCount <= GRAPH_COLORS.length)
            {
                currentColor = GRAPH_COLORS[pointDatasetCount - 1];
                plot.getRenderer(0).setSeriesPaint(pointDatasetCount - 1, currentColor);
            }
            else
                currentColor = (Color) plot.getRenderer(0).getSeriesPaint(pointDatasetCount - 1);

            XYSeries curvedSeries = new XYSeries(sampleId);
            DilutionCurve.DoublePoint[] curve = summary.getCurve();
            for (DilutionCurve.DoublePoint point : curve)
                curvedSeries.add(point.getX(), point.getY());
            curvesDataset.addSeries(curvedSeries);
            if (currentColor != null)
                plot.getRenderer(1).setSeriesPaint(curvesDataset.getSeriesCount() - 1, currentColor);
        }

        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Dilution"));
        chart.getXYPlot().addRangeMarker(new ValueMarker(0f, Color.DARK_GRAY, new BasicStroke()));
        for (int cutoff : cutoffs)
            chart.getXYPlot().addRangeMarker(new ValueMarker(cutoff));

        response.setContentType("image/png");
        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 425, 300);
    }


    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteRunAction extends SimpleRedirectAction<RowIdForm>
    {
        public ActionURL getRedirectURL(RowIdForm form) throws Exception
        {
            OldNabManager.get().deletePlate(getContainer(), form.getRowId());
            return getBeginURL();
        }
    }

    public static class RowIdForm
    {
        private int _rowId = -1;

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class RunsAction extends SimpleViewAction<RowIdForm>
    {
        public ModelAndView getView(RowIdForm rowIdForm, BindException errors) throws Exception
        {
            PlateQueryView previousRuns = PlateService.get().getPlateGridView(getViewContext());

            List<ActionButton> buttons = new ArrayList<ActionButton>();

            ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
            selectButton.setDisplayPermission(InsertPermission.class);
            buttons.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(InsertPermission.class);
            buttons.add(clearButton);

            ActionButton deleteButton = new ActionButton("deleteRuns.view", "Delete", DataRegion.MODE_GRID, ActionButton.Action.POST);
            deleteButton.setDisplayPermission(DeletePermission.class);
            deleteButton.setRequiresSelection(true);
            buttons.add(deleteButton);

            if (!AssayPublishService.get().getValidPublishTargets(getUser(), InsertPermission.class).isEmpty())
            {
                ActionButton publishButton = new ActionButton("publishPlatesChooseStudy.view", "Copy to Study", DataRegion.MODE_GRID, ActionButton.Action.POST);
                publishButton.setDisplayPermission(InsertPermission.class);
                publishButton.setRequiresSelection(true);
                buttons.add(publishButton);
            }

            previousRuns.setButtons(buttons);

            return addHeaderView(previousRuns);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Nab Runs");
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class DownloadAction extends ExportAction<AttachmentForm>
    {
        public void export(AttachmentForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            if (form.getEntityId() == null || form.getName() == null)
            {
                throw new NotFoundException("Page not found: EntityId and name are required URL parameters- incomplete URL?");
            }

            Plate plate = PlateService.get().getPlate(getContainer(), form.getEntityId());

            if (plate == null)
            {
                throw new NotFoundException("Page not found: The specified plate does not exist.  It may have been deleted from the database.");
            }

            AttachmentService.get().download(response, plate, form.getName());
        }
    }


    public static class GraphSelectedForm
    {
        private int[] _id;
        private boolean _print;

        public boolean isPrint()
        {
            return _print;
        }

        public void setPrint(boolean print)
        {
            _print = print;
        }

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class GraphSelectedAction extends SimpleViewAction<GraphSelectedForm>
    {
        public ModelAndView getView(GraphSelectedForm form, BindException errors) throws Exception
        {
            int[] wellGroupIds;
            if (form.getId() != null)
                wellGroupIds = form.getId();
            else
            {
                Set<String> wellgroupIdStrings = DataRegionSelection.getSelected(getViewContext(), false);

                if (wellgroupIdStrings == null || wellgroupIdStrings.size() == 0)
                    return null;

                wellGroupIds = new int[wellgroupIdStrings.size()];
                int idx = 0;

                for (String wellgroupIdString : wellgroupIdStrings)
                    wellGroupIds[idx++] = Integer.parseInt(wellgroupIdString);
            }

            DilutionSummary[] summaries = getDilutionSummaries(wellGroupIds);
            int[] cutoffList = getCutoffs(summaries);
            cacheSummaries(summaries);
            JspView<GraphSelectedBean> multiGraphView = new JspView<GraphSelectedBean>("/org/labkey/nab/legacyGraph.jsp",
                    new GraphSelectedBean(summaries, cutoffList));

            if (form.isPrint())
            {
                getPageConfig().setTemplate(Template.Print);
                return multiGraphView;
            }
            else
            {
                ActionURL printLink = new ActionURL(GraphSelectedAction.class, getContainer());
                printLink.addParameter("print", true);
                for (int id : wellGroupIds)
                    printLink.addParameter("id", Integer.toString(id));

                return addHeaderView(multiGraphView, printLink, null);
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Graph Selected Specimens");
        }
    }


    public static class GraphSelectedBean
    {
        private DilutionSummary[] _dilutionSummaries;
        private int[] _cutoffs;

        public GraphSelectedBean(DilutionSummary[] dilutions, int[] cutoffs)
        {
            _dilutionSummaries = dilutions;
            _cutoffs = cutoffs;
        }

        public DilutionSummary[] getDilutionSummaries()
        {
            return _dilutionSummaries;
        }

        public int[] getCutoffs()
        {
            return _cutoffs;
        }
    }


    public static class RenderMultiChartForm
    {
        private int[] _wellGroupId;

        public int[] getWellGroupId()
        {
            return _wellGroupId;
        }

        public void setWellGroupId(int[] wellGroupId)
        {
            _wellGroupId = wellGroupId;
        }
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class RenderMultiRunChart extends ExportAction<RenderMultiChartForm>
    {
        public void export(RenderMultiChartForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            DilutionSummary[] summaries = getCachedSummaries(form.getWellGroupId());
            int[] cutoffs = getCutoffs(summaries);
            renderChartPNG(response, summaries, cutoffs);
        }
    }


    private void cacheSummaries(DilutionSummary[] summaries)
    {
        HttpSession session = getViewContext().getRequest().getSession(true);
        session.setAttribute("nabSummaries", summaries);
    }


    private DilutionSummary[] getCachedSummaries(int ids[]) throws SQLException, ServletException
    {
        HttpSession session = getViewContext().getRequest().getSession(true);
        DilutionSummary[] summaries = (DilutionSummary[])session.getAttribute("nabSummaries");
        boolean cacheValid = true;

        if (summaries != null && summaries.length == ids.length)
        {
            for (int i = 0; i < summaries.length && cacheValid; i++)
            {
                if (ids[i] != summaries[i].getFirstWellGroup().getRowId().intValue())
                    cacheValid = false;
            }
        }
        else
            cacheValid = false;

        if (!cacheValid)
            summaries = getDilutionSummaries(ids);

        return summaries;
    }


    private DilutionSummary[] getDilutionSummaries(int[] wellGroupIds) throws SQLException, ServletException
    {
        List<DilutionSummary> summaries = new ArrayList<DilutionSummary>(wellGroupIds.length);

        for (int wellgroupId : wellGroupIds)
        {
            try
            {
                DilutionSummary summary = OldNabManager.get().getDilutionSummary(getContainer(), wellgroupId);
                summaries.add(summary);
            }
            catch (NumberFormatException e)
            {
                _log.warn("Bad post to graphSelected", e);
            }
        }

        return summaries.toArray(new DilutionSummary[summaries.size()]);
    }


    private int[] getCutoffs(DilutionSummary[] summaries)
    {
        Set<Integer> cutoffSet = new HashSet<Integer>();
        for (DilutionSummary summary : summaries)
        {
            try
            {
                for (int cutoff : summary.getAssay().getCutoffs())
                    cutoffSet.add(cutoff);
            }
            catch (NumberFormatException e)
            {
                _log.warn("Bad post to graphSelected", e);
            }
        }
        List<Integer> sortedUniqueCutoffs = new ArrayList<Integer>(cutoffSet);
        Collections.sort(sortedUniqueCutoffs);
        int[] cutoffs = new int[sortedUniqueCutoffs.size()];
        for (int i = 0; i < cutoffs.length; i++)
            cutoffs[i] = sortedUniqueCutoffs.get(i).intValue();
        return cutoffs;
    }


    public static class RunIdForm
    {
        private Integer _runId;

        public Integer getRunId()
        {
            return _runId;
        }

        public void setRunId(Integer runId)
        {
            _runId = runId;
        }
    }


    @RequiresPermissionClass(DeletePermission.class)
    public class DeleteRunsAction extends SimpleRedirectAction<RunIdForm>
    {
        public ActionURL getRedirectURL(RunIdForm form) throws Exception
        {
            Set<String> rowids = DataRegionSelection.getSelected(getViewContext(), true);

            if (rowids != null)
            {
                for (String rowidStr : rowids)
                {
                    try
                    {
                        int rowid = Integer.parseInt(rowidStr);
                        OldNabManager.get().deletePlate(getContainer(), rowid);
                    }
                    catch (NumberFormatException e)
                    {
                        _log.warn("Bad post to delete runs.", e);
                    }
                }
            }

            if (form.getRunId() != null)
                return getDisplayURL(getContainer(), form.getRunId().intValue(), false, false);
            else
                return getRunsURL();
        }
    }


    public static class PublishBean
    {
        private User _user;
        private List<Integer> _ids;
        private boolean _plateIds;

        public PublishBean(User user, List<Integer> ids, boolean plateIds)
        {
            _user = user;
            _ids = ids;
            _plateIds = plateIds;
        }

        public Set<Study> getValidTargets()
        {
            return AssayPublishService.get().getValidPublishTargets(_user, InsertPermission.class);
        }

        public List<Integer> getIds()
        {
            return _ids;
        }

        public boolean isPlateIds()
        {
            return _plateIds;
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class PublishPlatesChooseStudy extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            List<Integer> plateIds = getCheckboxIds();

            if (plateIds.isEmpty())
            {
                errors.reject("main", "You must select at least one row to copy to a study.");
                return new SimpleErrorView(errors);
            }
            else
            {
                JspView<PublishBean> chooseStudyView = new JspView<PublishBean>("/org/labkey/nab/publishChooseStudy.jsp",
                        new PublishBean(getUser(), plateIds, true));
                return addHeaderView(chooseStudyView);
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Choose Target Study");
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class PublishWellGroupsChooseStudy extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            List<Integer> wellgroupIds = getCheckboxIds();

            if (wellgroupIds.isEmpty())
            {
                errors.reject("main", "You must select at least one specimen to copy to a study.");
                return new SimpleErrorView(errors);
            }
            else
            {
                JspView<PublishBean> chooseStudyView = new JspView<PublishBean>("/org/labkey/nab/publishChooseStudy.jsp",
                        new PublishBean(getUser(), wellgroupIds, false));
                return addHeaderView(chooseStudyView);
            }
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Choose Target Study");
        }
    }


    private List<Integer> getCheckboxIds()
    {
        Set<String> idStrings = DataRegionSelection.getSelected(getViewContext(), false);
        List<Integer> ids = new ArrayList<Integer>();

        if (idStrings == null)
            return ids;

        for (String rowIdStr : idStrings)
        {
            try
            {
                ids.add(Integer.parseInt(rowIdStr));
            }
            catch (NumberFormatException e)
            {
                // fall through: we'll continue with the valid plate ids.
            }
        }

        return ids;
    }


    @RequiresPermissionClass(ReadPermission.class)
    public class SampleListAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            PlateQueryView queryView = PlateService.get().getWellGroupGridView(getViewContext(), WellGroup.Type.SPECIMEN);

            List<ActionButton> buttons = new ArrayList<ActionButton>();
            ActionButton selectButton = ActionButton.BUTTON_SELECT_ALL.clone();
            selectButton.setDisplayPermission(InsertPermission.class);
            buttons.add(selectButton);

            ActionButton clearButton = ActionButton.BUTTON_CLEAR_ALL.clone();
            clearButton.setDisplayPermission(InsertPermission.class);
            buttons.add(clearButton);

            ActionButton graphSelectedButton = new ActionButton(GraphSelectedAction.class, "Graph Selected");
            graphSelectedButton.setActionType(ActionButton.Action.POST);
            graphSelectedButton.setRequiresSelection(true);
            buttons.add(graphSelectedButton);

            if (!AssayPublishService.get().getValidPublishTargets(getUser(), InsertPermission.class).isEmpty())
            {
                ActionButton publishButton = new ActionButton("publishWellGroupsChooseStudy.view", "Copy to Study", DataRegion.MODE_GRID, ActionButton.Action.POST);
                publishButton.setDisplayPermission(InsertPermission.class);
                publishButton.setRequiresSelection(true);
                buttons.add(publishButton);
            }

            queryView.setButtons(buttons);

            return addHeaderView(queryView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("NAB Runs By Sample");
        }
    }


    public static class PublishForm
    {
        private String[] _includedSampleIds;
        private String[] _sequenceNums;
        private String[] _sampleIds;
        private String[] _participantIds;
        private String[] _dates;
        private int[] _id;
        private boolean _plateIds;
        private String _targetContainerId;

        public String[] getIncludedSampleIds()
        {
            return _includedSampleIds;
        }

        public void setIncludedSampleIds(String[] includedSampleIds)
        {
            _includedSampleIds = includedSampleIds;
        }

        public String[] getParticipantIds()
        {
            return _participantIds;
        }

        public void setParticipantIds(String[] participantIds)
        {
            _participantIds = participantIds;
        }

        public String[] getSampleIds()
        {
            return _sampleIds;
        }

        public void setSampleIds(String[] sampleIds)
        {
            _sampleIds = sampleIds;
        }

        public String[] getSequenceNums()
        {
            return _sequenceNums;
        }

        public void setSequenceNums(String[] sequenceNums)
        {
            _sequenceNums = sequenceNums;
        }

        public String[] getDates()
        {
            return _dates;
        }

        public void setDates(String[] dates)
        {
            _dates = dates;
        }

        public int[] getId()
        {
            return _id;
        }

        public void setId(int[] id)
        {
            _id = id;
        }

        public boolean isPlateIds()
        {
            return _plateIds;
        }

        public void setPlateIds(boolean plateIds)
        {
            _plateIds = plateIds;
        }

        public String getTargetContainerId()
        {
            return _targetContainerId;
        }

        public void setTargetContainerId(String targetContainerId)
        {
            _targetContainerId = targetContainerId;
        }

        public ParticipantVisit getReshowData(final String sampleId)
        {
            if (_sampleIds == null)
                return null;

            for (int index = 0; index < _sampleIds.length; index++)
            {
                if (_sampleIds[index].equals(sampleId))
                {
                    String sequenceNum =  _sequenceNums != null ?  _sequenceNums[index] : null;
                    String date =  _dates != null ?  _dates[index] : null;
                    Container targetContainer = _targetContainerId != null ? ContainerManager.getForId(_targetContainerId) : null;
                    return new PublishSampleInfo(_participantIds[index], sampleId, sequenceNum, date, targetContainer);
                }
            }
            return null;
        }


        public void validate(Errors errors)
        {
            Set<String> selectedSamples = new HashSet<String>();

            if (getIncludedSampleIds() == null)
                errors.reject("main", "You must select at least one specimen to copy to a study.");
            else
            {
                selectedSamples.addAll(Arrays.asList(getIncludedSampleIds()));

                for (int i = 0; i < _sampleIds.length; i++)
                {
                    if (!selectedSamples.contains(_sampleIds[i]))
                        continue;
                    if (_participantIds[i] == null || _participantIds[i].length() == 0)
                        errors.reject("main", "Participant ID is required for sample " + _sampleIds[i]);
                    if (AssayPublishService.get().getTimepointType(ContainerManager.getForId(getTargetContainerId())) == TimepointType.VISIT)
                    {
                        if (_sequenceNums[i] == null)
                            errors.reject("main", "Visit Sequence number is required for sample " + _sampleIds[i]);
                        else
                        {
                            try
                            {
                                double d = Double.parseDouble(_sequenceNums[i]);
                            }
                            catch (NumberFormatException e)
                            {
                                errors.reject("main", "Visit Sequence numbers should be decimal values.  The following is invalid: " + _sequenceNums[i]);
                            }
                        }
                    }
                    else
                    {
                        if (_dates[i] == null)
                            errors.reject("main", "Date is required for sample " + _sampleIds[i]);
                        else
                        {
                            try
                            {
                                Date d = (Date) ConvertUtils.convert(_dates[i], Date.class);
                            }
                            catch (ConversionException e)
                            {
                                errors.reject("main", "The following is not a valid date: " + _dates[i]);
                            }
                        }

                    }
                }
            }
        }
    }


    @RequiresPermissionClass(InsertPermission.class)
    public class PublishAction extends FormViewAction<PublishForm>
    {
        private ActionURL _returnURL = null;

        public void validateCommand(PublishForm form, Errors errors)
        {
            form.validate(errors);
        }

        public ModelAndView getView(PublishForm form, boolean reshow, BindException errors) throws Exception
        {
            Container targetContainer = ContainerManager.getForId(form.getTargetContainerId());
            if (!targetContainer.hasPermission(getUser(), InsertPermission.class))
            {
                throw new UnauthorizedException();
            }

            List<WellGroup> sampleList = new ArrayList<WellGroup>();
            if (form.isPlateIds())
            {
                for (Integer plateId : form.getId())
                {
                    Plate plate = PlateService.get().getPlate(getContainer(), plateId.intValue());
                    if (plate != null)
                    {
                        for (WellGroup sampleGroup : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                            sampleList.add(sampleGroup);
                    }
                }
            }
            else
            {
                for (Integer wellGroupId : form.getId())
                {
                    WellGroup group = PlateService.get().getWellGroup(getContainer(), wellGroupId.intValue());
                    if (group != null)
                    {
                        assert group.getType() == WellGroup.Type.SPECIMEN : "Expected only specimen well groups";
                        sampleList.add(group);
                    }
                }
            }

            List<WellGroup> sortedGroups = new ArrayList<WellGroup>();
            for (WellGroup wellgroup : sampleList)
                sortedGroups.add(wellgroup);
            Collections.sort(sortedGroups, new Comparator<WellGroup>()
            {
                public int compare(WellGroup group1, WellGroup group2)
                {
                    String sampleId1 = (String) group1.getProperty(OldNabManager.SampleProperty.SampleId.name());
                    String sampleId2 = (String) group2.getProperty(OldNabManager.SampleProperty.SampleId.name());
                    return sampleId1.compareToIgnoreCase(sampleId2);
                }
            });

            Map<WellGroup, ParticipantVisit> sampleInfoMap = new LinkedHashMap<WellGroup, ParticipantVisit>();
            for (WellGroup wellgroup : sortedGroups)
            {
                String sampleId = (String) wellgroup.getProperty(OldNabManager.SampleProperty.SampleId.name());
                ParticipantVisit sampleInfo = form.getReshowData(sampleId);
                if (sampleInfo == null)
                    sampleInfo = SpecimenService.get().getSampleInfo(targetContainer, sampleId);
                sampleInfoMap.put(wellgroup, sampleInfo);
            }

            JspView<PublishVerifyBean> publishVerifyView = new JspView<PublishVerifyBean>("/org/labkey/nab/publishVerify.jsp",
                    new PublishVerifyBean(targetContainer, sampleInfoMap), errors);

            return addHeaderView(publishVerifyView);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Associate Data");
        }

        public boolean handlePost(PublishForm form, BindException errors) throws Exception
        {
            Container targetContainer = ContainerManager.getForId(form.getTargetContainerId());
            if (!targetContainer.hasPermission(getUser(), InsertPermission.class))
            {
                throw new UnauthorizedException();
            }

            boolean dateBased = AssayPublishService.get().getTimepointType(targetContainer) != TimepointType.VISIT;
            Set<String> includedSamples = new HashSet<String>();
            includedSamples.addAll(Arrays.asList(form.getIncludedSampleIds()));

            if (!includedSamples.isEmpty())
            {
                List<Plate> plates = new ArrayList<Plate>();
                List<Map<String, Object>> sampleProperties = new ArrayList<Map<String, Object>>();

                for (int i = 0; i < form.getSampleIds().length; i++)
                {
                    String sampleId = form.getSampleIds()[i];

                    if (includedSamples.contains(sampleId))
                    {
                        int wellGroupId = form.getId()[i];
                        WellGroup sample = PlateService.get().getWellGroup(getContainer(), wellGroupId);

                        if (sample != null)
                        {
                            plates.add(sample.getPlate());
                            String ptid = form.getParticipantIds()[i];
                            String visitIdString;
                            Double visitId = null;
                            String dateString;
                            Date date = null;

                            if (dateBased)
                            {
                                dateString = form.getDates()[i];
                                date = (Date) ConvertUtils.convert(dateString, Date.class);
                            }
                            else
                            {
                                visitIdString = form.getSequenceNums()[i];
                            // parse the double here: we've already verified that it's convertable in 'validate'.
                                visitId = Double.parseDouble(visitIdString);
                            }

                            Map<String, Object> samplePropertyMap = new HashMap<String, Object>();
                            for (String property : sample.getPropertyNames())
                                samplePropertyMap.put(property, sample.getProperty(property));

                            for (String property : sample.getPlate().getPropertyNames())
                                samplePropertyMap.put(property, sample.getPlate().getProperty(property));
                            samplePropertyMap.put("participantid", ptid);
                            if (dateBased)
                                samplePropertyMap.put("date", date);
                            else
                                samplePropertyMap.put("sequencenum", visitId);
                            samplePropertyMap.put("sourceLsid", sample.getLSID());
                            String virusName = (String) samplePropertyMap.get(OldNabManager.PlateProperty.VirusName.name());
                            String virusId = (String) samplePropertyMap.get(OldNabManager.PlateProperty.VirusId.name());
                            String virusKey = (virusId != null && virusId.length() > 0) ? virusId : "Unknown";
                            if (virusName != null && virusName.length() > 0)
                                virusKey +=  " (" + virusName + ")";
                            samplePropertyMap.put(OldNabManager.PlateProperty.VirusId.name(), virusKey);
                            sampleProperties.add(samplePropertyMap);
                        }
                    }
                }

                if (!sampleProperties.isEmpty())
                {
                    List<String> sampleErrors = new ArrayList<String>();
                    _returnURL = AssayPublishService.get().publishAssayData(getUser(), getContainer(), targetContainer,
                            "NAB", null, sampleProperties,
                            OldNabManager.get().getPropertyTypes(plates),
                            OldNabManager.PlateProperty.VirusId.name(), sampleErrors);

                    if (errors != null && !sampleErrors.isEmpty())
                    {
                        for (String sampleError : sampleErrors)
                            errors.reject("main", sampleError);

                        return false;
                    }
                    else
                    {
                        return true;
                    }
                }
            }

            errors.reject("main", "At least one sample must be selected to copy to a study.");

            return false;
        }

        public ActionURL getSuccessURL(PublishForm form)
        {
            return _returnURL;
        }
    }

    public static class PublishSampleInfo implements ParticipantVisit
    {
        private String _ptid;
        private String _specimenID;
        private String _visitID;
        private String _dateString;
        private Container _studyContainer;

        public PublishSampleInfo(String ptid, String sampleId, String sequenceNum, String dateString, Container studyContainer)
        {
            _visitID = sequenceNum;
            _specimenID = sampleId;
            _ptid = ptid;
            _dateString = dateString;
            _studyContainer = studyContainer;
        }

        @Override
        public Container getStudyContainer()
        {
            return _studyContainer;
        }

        public String getParticipantID()
        {
            return _ptid;
        }

        public String getSpecimenID()
        {
            return _specimenID;
        }

        public String getVisitIDString()
        {
            return _visitID;
        }

        public Double getVisitID()
        {
            try
            {
                if (_visitID == null)
                    return null;
                else
                    return Double.parseDouble(_visitID);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        public ExpMaterial getMaterial()
        {
            throw new UnsupportedOperationException("Not Implemented for PublishSampleInfo");
        }

        public Integer getCohortID()
        {
            throw new UnsupportedOperationException("Not Implemented for PublishSampleInfo");
        }

        public Date getDate()
        {
            try
            {
                if (null == _dateString)
                    return null;
                return (Date) ConvertUtils.convert(_dateString, Date.class);
            }
            catch (ConversionException x)
            {
                return null;
            }
        }

        public String getDateString()
        {
            return _dateString;
        }
    }


    public static class PublishVerifyBean
    {
        private List<String> _sampleProperties;
        private Map<WellGroup, ParticipantVisit> _sampleInfoMap;
        private Container _targetContainer;
        private DecimalFormat _decimalFormat = new DecimalFormat("0.##");
        private List<String> _uploadErrors;

        public PublishVerifyBean(Container targetContainer, Map<WellGroup, ParticipantVisit> sampleInfoMap)
        {
            _sampleInfoMap = sampleInfoMap;
            _targetContainer = targetContainer;
            Set<String> propertySet = new HashSet<String>();
            for (WellGroup group : sampleInfoMap.keySet())
                propertySet.addAll(group.getPropertyNames());
            propertySet.remove(OldNabManager.SampleProperty.SampleId.name());
            _sampleProperties = new ArrayList<String>(propertySet);
            Collections.sort(_sampleProperties);
        }

        public Map<WellGroup, ParticipantVisit> getSampleInfoMap()
        {
            return _sampleInfoMap;
        }

        public Container getTargetContainer()
        {
            return _targetContainer;
        }

        public List<String> getSampleProperties()
        {
            return _sampleProperties;
        }

        public String getSampleIdCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.SpecimenGlobalUniqueId);
        }

        public String getVisitIdCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.VisitId);
        }

        public String getParticipantCompletionBase()
        {
            return SpecimenService.get().getCompletionURLBase(_targetContainer,
                    SpecimenService.CompletionType.ParticipantId);
        }

        public List<String> getUploadErrors()
        {
            return _uploadErrors;
        }

        public void setUploadErrors(List<String> uploadErrors)
        {
            _uploadErrors = uploadErrors;
        }

        public String format(Object obj)
        {
            if (obj == null)
                return null;
            if (obj instanceof Double|| obj instanceof Float)
                return _decimalFormat.format(obj);
            if (obj instanceof Date)
                return DateUtil.formatDate((Date) obj);
            return obj.toString();
        }
    }
}
