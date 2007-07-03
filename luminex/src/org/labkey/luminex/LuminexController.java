package org.labkey.luminex;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.apache.struts.upload.FormFile;
import org.apache.struts.upload.MultipartRequestHandler;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartColor;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;

import java.util.*;
import java.util.List;
import java.io.IOException;
import java.awt.*;
import java.text.DateFormat;

import jxl.Workbook;
import jxl.Sheet;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")}, longLived = true)
public class LuminexController extends ViewController
{
    static Logger _log = Logger.getLogger(LuminexController.class);

    private Map<String, LuminexRun> _cachedRuns = new LinkedHashMap<String, LuminexRun>();

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward begin() throws Exception
    {
        List<LuminexRun> runs = new ArrayList<LuminexRun>();
        for (LuminexRun run : _cachedRuns.values())
        {
            runs.add(run);
        }
        JspView<List<LuminexRun>> v = new JspView<List<LuminexRun>>("/org/labkey/luminex/begin.jsp", runs);
        return renderInTemplate(v, getContainer(), "Luminex Runs");
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    protected Forward upload() throws Exception
    {
        JspView v = new JspView("/org/labkey/luminex/upload.jsp");
        return renderInTemplate(v, getContainer(), "Upload a New Luminex Run");
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward showRun(LuminexRunForm form) throws Exception
    {
        LuminexRun run = getCachedRun(form.getFileName());
        JspView<LuminexRun> analytesView = new JspView<LuminexRun>("/org/labkey/luminex/analyteSummary.jsp", run);
        analytesView.setTitle("Analytes");

        JspView<LuminexRun> plateView = new JspView<LuminexRun>("/org/labkey/luminex/plateLayout.jsp", run);
        plateView.setTitle("Plate Layout");

        JspView<LuminexRun> specimensView = new JspView<LuminexRun>("/org/labkey/luminex/specimenSummary.jsp", run);
        specimensView.setTitle("Specimens");

        VBox box = new VBox(analytesView, plateView, specimensView);

        for (AnalyteInfo analyteInfo : run.getAnalyteInfos())
        {
            JspView<AnalyteInfo> analyteView = new JspView<AnalyteInfo>("/org/labkey/luminex/analyteInfo.jsp", analyteInfo);
            analyteView.setTitle(analyteInfo.getAnalyteName());
            box.addView(analyteView);
        }

        return renderInTemplate(box, getContainer(), "Luminex Run: " + run.getFileName());
    }

    public static class UploadForm extends ViewForm
    {
        private String _thawList;
        private String _labId;
        
        public String getLabId()
        {
            return _labId;
        }

        public void setLabId(String labId)
        {
            _labId = labId;
        }

        public String getThawList()
        {
            return _thawList;
        }

        public void setThawList(String thawList)
        {
            _thawList = thawList;
        }

        public FormFile getUploadFile()
        {
            MultipartRequestHandler handler = getMultipartRequestHandler();
            if (null == handler)
                return null;

            Map<String, FormFile> formFiles = handler.getFileElements();
            if (null == formFiles)
                return null;

            FormFile formFile = formFiles.get("file1");
            if (null == formFile)
                return null;

            if (0 == formFile.getFileSize())
                return null;

            return formFile;
        }
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_INSERT)
    protected Forward processUpload(UploadForm form) throws Exception
    {
        FormFile file = form.getUploadFile();

        Map<String, String> thawMap = new HashMap<String, String>();

        String thawList = form.getThawList();
        if (thawList == null)
        {
            thawList = "";
        }
        String[] thaws = thawList.split("\\r");
        for (String thaw : thaws)
        {
            StringTokenizer st = new StringTokenizer(thaw, "\t", false);
            if (st.hasMoreTokens())
            {
                String thawNumber = st.nextToken().trim();
                if (st.hasMoreTokens())
                {
                    thawMap.put(thawNumber, st.nextToken().trim());
                }
            }
        }

        Workbook workbook = Workbook.getWorkbook(file.getInputStream());
        LuminexRun run = new LuminexRun(file.getFileName());
        run.setCreatedBy(getUser().getDisplayName());
        run.setLab(form.getLabId());
        run.setCreatedOn(DateFormat.getInstance().format(new Date()));
        for (int sheetIndex = 1; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
        {
            Sheet analyteSheet = workbook.getSheet(sheetIndex);
            AnalyteInfo analyteInfo = new AnalyteInfo(analyteSheet.getName());

            int row = 0;
            while (!"".equals(analyteSheet.getCell(0, row++).getContents()));

            List<String> colNames = new ArrayList<String>();
            for (int col = 1; col < analyteSheet.getColumns(); col++)
            {
                colNames.add(analyteSheet.getCell(col, row).getContents());
            }
            row++;

            for (; row < analyteSheet.getRows() && !"".equals(analyteSheet.getCell(0, row).getContents()); row++)
            {
                Map<String, String> rowValues = new LinkedHashMap<String, String>();
                for (int col = 1; col < analyteSheet.getColumns(); col++)
                {
                    rowValues.put(colNames.get(col - 1), analyteSheet.getCell(col, row).getContents());
                }
                String specimenID = thawMap.get(rowValues.get("Type"));
                if (specimenID == null)
                {
                    specimenID = rowValues.get("Type");
                }
                rowValues.put("SpecimenID", specimenID);

                analyteInfo.addValue(rowValues);
            }
            run.addAnalyteInfo(analyteInfo);
        }

        run.getSpecimenInfos();

        _cachedRuns.put(file.getFileName(), run);

        ViewURLHelper url = cloneViewURLHelper();
        url.deleteParameters();
        url.addParameter("fileName", run.getFileName());
        url.setAction("showRun.view");
        return new ViewForward(url);
    }

    public static class LuminexRunForm extends ViewForm
    {
        private String _fileName;

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward showSpecimenPlot(LuminexRunForm form) throws IOException
    {
        LuminexRun run = getCachedRun(form.getFileName());

        CombinedRangeXYPlot parent = new CombinedRangeXYPlot(new LogarithmicAxis("Log(Conc in Range)"));

        for (SpecimenInfo specimenInfo : run.getNonControlSpecimenInfos())
        {
            XYSeries series = new XYSeries(specimenInfo.getName());
            int x = 0;
            for (LuminexDataRow value: specimenInfo.getValues())
            {
                series.add(x++, value.getConcInRange());
            }
            XYDataset dataset = new XYSeriesCollection(series);
            XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(true, true);
            pointRenderer.setStroke(new BasicStroke(
                    0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{0.0f, 100.0f}, 0.0f));
            pointRenderer.setSeriesPaint(0, specimenInfo.getColor());
            pointRenderer.setSeriesShape(0, pointRenderer.getSeriesShape(0));
            XYPlot subplot = new XYPlot(dataset, new NumberAxis(specimenInfo.getName()), null,
                    pointRenderer);
            parent.add(subplot);
            subplot.getDomainAxis().setLabelAngle(Math.PI / 2);
        }

        JFreeChart chart = new JFreeChart("Conc in Range by Specimen", JFreeChart.DEFAULT_TITLE_FONT, parent, false);

        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(), chart, 1000, 300);
        return null;
    }

    @Jpf.Action @RequiresPermission(ACL.PERM_READ)
    protected Forward showAnalytePlot(LuminexRunForm form) throws IOException
    {
        LuminexRun run = getCachedRun(form.getFileName());

        CombinedRangeXYPlot parent = new CombinedRangeXYPlot(new LogarithmicAxis("Log(Conc in Range)"));

        for (AnalyteInfo analyteInfo : run.getAnalyteInfos())
        {
            XYSeriesCollection dataset = new XYSeriesCollection();
            int x = 0;
            XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(true, true);
            for (LuminexDataRow value: analyteInfo.getDataRows())
            {
                XYSeries series = new XYSeries(analyteInfo.getAnalyteName());
                series.add(x++, value.getConcInRange());
                dataset.addSeries(series);
                pointRenderer.setSeriesPaint(dataset.getSeriesCount() - 1, value.getSpecimen() == null ? Color.BLACK : value.getSpecimen().getColor());
                pointRenderer.setSeriesShape(dataset.getSeriesCount() - 1, pointRenderer.getSeriesShape(0));
            }
            pointRenderer.setStroke(new BasicStroke(
                    0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{0.0f, 100.0f}, 0.0f));
            XYPlot subplot = new XYPlot(dataset, new NumberAxis(analyteInfo.getAnalyteName()), null,
                    pointRenderer);
            parent.add(subplot);
            subplot.getDomainAxis().setLabelAngle(Math.PI / 2);
        }

        JFreeChart chart = new JFreeChart("Conc in Range by Analyte", JFreeChart.DEFAULT_TITLE_FONT, parent, false);

        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(), chart, 2000, 300);
        return null;
    }


    private LuminexRun getCachedRun(String fileName)
    {
        LuminexRun result = _cachedRuns.get(fileName);
        if (result == null)
        {
            HttpView.throwNotFound("Could not find run for " + fileName);
        }
        return result;
    }

    private Forward renderInTemplate(HttpView view) throws Exception
    {
        HttpView template = new HomeTemplate(getViewContext(), getContainer(), view);
        includeView(template);
        return null;
    }
}