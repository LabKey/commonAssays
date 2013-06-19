/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
package org.labkey.ms2.scoring;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.util.Enumeration;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ScoringController extends SpringActionController
{
    private static Logger _log = Logger.getLogger(ScoringController.class);
    private static DefaultActionResolver _resolver = new DefaultActionResolver(ScoringController.class);

    private static HelpTopic getHelpTopic(String topic)
    {
        return new HelpTopic(topic);
    }

    public ScoringController()
    {
        super();
        setActionResolver(_resolver);
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class CompareAction extends SimpleViewAction<ChartForm>
    {
        public ModelAndView getView(ChartForm form, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic("MS2-Scoring/compare"));

            form.initArrays(getViewContext().getRequest());

            if (form.getRuns().length == 0)
                errors.reject(ERROR_MSG, "No runs specified.");

            return new JspView<>("/org/labkey/ms2/scoring/compare.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Compare MS2 Peptide Scoring");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    abstract public class ChartCompareBaseAction extends ExportAction<ChartForm>
    {
        abstract public XYSeriesCollection getSeriesCollection(ChartForm form);

        public void export(ChartForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            form.initArrays(getViewContext().getRequest());

            XYSeriesCollection collection = getSeriesCollection(form);

            JFreeChart chart = ChartFactory.createXYLineChart(form.getTitle(),
                    "False Positives",
                    "Correct IDs",
                    collection,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);
            chart.getTitle().setFont(chart.getTitle().getFont().deriveFont(16.0f));
            chart.setBackgroundPaint(Color.white);

            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < collection.getSeriesCount(); i++)
            {
                final Paint paint = plot.getRenderer().getSeriesPaint(i);
                MS2Manager.XYSeriesROC series = (MS2Manager.XYSeriesROC) collection.getSeries(i);
                series.plotAnnotations(plot, paint);
            }

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(),
                    chart,
                    form.getWidth(),
                    form.getHeight(),
                    new ChartRenderingInfo(new StandardEntityCollection()));
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ChartCompareAction extends ChartCompareBaseAction
    {
        public XYSeriesCollection getSeriesCollection(ChartForm form)
        {
            /* @todo: error handling. */

            XYSeriesCollection collection =
                    MS2Manager.getROCData(form.getRunIds(),
                            form.getDiscriminates(),
                            form.getIncrement(),
                            form.getPercentAACorrect(),
                            form.getLimit(),
                            form.getMarkNums(),
                            form.isMarkFdr()
                    );

            if (form.isSaveTsvs())
            {
                File tempDir = new File(System.getenv("TEMP"));
                for (int i = 0; i < collection.getSeriesCount(); i++)
                {
                    XYSeries series = collection.getSeries(i);
                    File tempFile = new File(tempDir, series.getKey() + ".tsv");
                    FileWriter writer = null;
                    try
                    {
                        writer = new FileWriter(tempFile);
                        for (int point = 0; point < series.getItemCount(); point++)
                        {
                            writer.write(series.getX(point).toString());
                            writer.write("\t");
                            writer.write(series.getY(point).toString());
                            writer.write("\n");
                        }
                    }
                    catch (IOException e)
                    {
                        _log.error("Failed to save scoring TSV " + tempFile, e);
                    }
                    finally
                    {
                        if (writer != null)
                        {
                            try { writer.close(); }
                            catch(Exception e) {}
                        }
                    }
                }
            }

            return collection;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ChartCompareProtAction extends ChartCompareBaseAction
    {
        public XYSeriesCollection getSeriesCollection(ChartForm form)
        {
            /* @todo: error handling. */

            return MS2Manager.getROCDataProt(form.getRunIds(),
                            form.getIncrement(),
                            form.getDiscriminates(),
                            form.getPercentAACorrect(),
                            form.getLimit(),
                            form.getMarkNums(),
                            form.isMarkFdr()
            );
        }
    }

    public static class ChartForm
    {
        private String title = "Scoring Comparison";
        private double percentAACorrect = 0.1;
        private double increment = 0.2;
        private int limit = 200;
        private int width = 500;
        private int height = 500;
        private String marks = "0,50";
        private boolean markFdr;
        private boolean saveTsvs;
        private boolean discriminates[][];
        private MS2Run[] runs;

        public void initArrays(HttpServletRequest request)
        {
            int size = 0;
            String sizeValue = request.getParameter("size");
            if (sizeValue != null)
            {
                try
                {
                    size = Integer.parseInt(sizeValue);
                }
                catch (Exception e)
                {
                    _log.warn("Invalid size parameter comparing scores.", e);
                }
            }

            String[] runIdValues;
            if (size == 0)
            {
                runIdValues = request.getParameterValues(DataRegion.SELECT_CHECKBOX_NAME);
                size = runIdValues.length;
            }
            else
            {
                runIdValues = new String[size];
                for (int i = 0; i < size; i++)
                    runIdValues[i] = request.getParameter("runIds_" + i);
            }

            MS2Run[] runs = new MS2Run[size];
            for (int i = 0; i < size; i++)
                runs[i] = MS2Manager.getRun(runIdValues[i]);
            setRuns(runs);

            boolean[][] discriminates = new boolean[size][];

            for (int i = 0; i < size; i++)
                discriminates[i] = new boolean[64]; // Safe maximum.

            Enumeration names = request.getParameterNames();
            while (names.hasMoreElements())
            {
                final String name = (String) names.nextElement();
                if (!name.startsWith("discriminates_"))
                    continue;
                final String[] nameParts = name.split("_");
                if (nameParts.length != 3)
                    continue;
                final String value = request.getParameter(name);
                if ("on".equals(value))
                    discriminates[Integer.parseInt(nameParts[1])][Integer.parseInt(nameParts[2])] = true;
            }

            setDiscriminates(discriminates);
        }

        public boolean[][] getDiscriminates()
        {
            return discriminates;
        }

        public void setDiscriminates(boolean[][] discriminates)
        {
            this.discriminates = discriminates;
        }

        public int getWidth()
        {
            return width;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }

        public int getHeight()
        {
            return height;
        }

        public void setHeight(int height)
        {
            this.height = height;
        }

        public int getLimit()
        {
            return limit;
        }

        public void setLimit(int limit)
        {
            this.limit = limit;
        }

        public double getIncrement()
        {
            return increment;
        }

        public void setIncrement(double increment)
        {
            this.increment = increment;
        }

        public double getPercentAACorrect()
        {
            return percentAACorrect;
        }

        public void setPercentAACorrect(double percentAACorrect)
        {
            this.percentAACorrect = percentAACorrect;
        }

        public String getMarks()
        {
            return marks;
        }

        public void setMarks(String marks)
        {
            this.marks = marks;
        }

        public boolean isMarkFdr()
        {
            return markFdr;
        }

        public void setMarkFdr(boolean markFdr)
        {
            this.markFdr = markFdr;
        }

        public boolean isSaveTsvs()
        {
            return saveTsvs;
        }

        public void setSaveTsvs(boolean saveTsvs)
        {
            this.saveTsvs = saveTsvs;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public MS2Run[] getRuns()
        {
            return runs;
        }

        public void setRuns(MS2Run[] runs)
        {
            this.runs = runs;
        }

        public double[] getMarkNums()
        {
            String[] markStrs = new String[0];

            if (marks.length() > 0)
                markStrs = marks.split(",");

            double[] markNums = new double[markStrs.length];
            for (int i = 0; i < markNums.length; i++)
                markNums[i] = Double.parseDouble(markStrs[i]);
            return markNums;
        }

        public int[] getRunIds()
        {
            int[] runIds = new int[runs.length];
            for (int i = 0; i < runs.length; i++)
                runIds[i] = runs[i].getRun();
            return runIds;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class DiscriminateAction extends SimpleViewAction<ChartDiscForm>
    {
        public ModelAndView getView(ChartDiscForm form, BindException errors) throws Exception
        {
            setHelpTopic(getHelpTopic("MS2-Scoring/discriminate"));

            form.initArrays(getViewContext().getRequest());

            final MS2Run run = MS2Manager.getRun(form.getRunId());
            if (run == null)
                errors.reject(ERROR_MSG, "No run specified.");
            else if (run.getNegativeHitCount() < run.getPeptideCount() / 3)
                errors.reject(ERROR_MSG, "Insufficient negative hit data to perform analysis.");

            form.setRun(run);

            return new JspView<>("/org/labkey/ms2/scoring/discriminate.jsp", form);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("MS2 Peptide Scoring Discriminate Charts");
        }
    }

    public static ActionURL urlChartDiscriminate(Container c, int charge, ChartDiscForm form)
    {
        int i = charge - 1;
        return new ActionURL(ChartDiscriminateAction.class, c)
                .addParameter(ChartDiscForm.PARAMS.runId, form.getRunId())
                .addParameter(ChartDiscForm.PARAMS.width, form.getWidth())
                .addParameter(ChartDiscForm.PARAMS.height, form.getHeight())
                .addParameter(ChartDiscForm.PARAMS.percentAACorrect, Double.toString(form.getPercentAACorrect()))
                .addParameter(ChartDiscForm.PARAMS.charge, charge)
                .addParameter(ChartDiscForm.PARAMS.buckets + "_" + i, Double.toString(form.getBuckets()[i]))
                .addParameter(ChartDiscForm.PARAMS.expressions + "_" + i, form.getExpressions()[i])
                .addParameter(ChartDiscForm.PARAMS.scaleFactors + "_" + i, form.getScaleFactors()[i])
                .addParameter(ChartDiscForm.PARAMS.title, form.getTitle() + " (charge " + charge + ")");
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ChartDiscriminateAction extends ExportAction<ChartDiscForm>
    {
        public void export(ChartDiscForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            Container c = getContainer();

            form.initArrays(getViewContext().getRequest());

            /* @todo: error handling. */

            int charge = form.getCharge();
            XYSeriesCollection collection =
                    MS2Manager.getDiscriminateData(form.getRunId(),
                            charge,
                            form.getPercentAACorrect(),
                            form.getExpressions()[charge-1],
                            form.getBuckets()[charge-1],
                            form.getScaleFactors()[charge-1]
                    );

            JFreeChart chart = ChartFactory.createXYLineChart(form.getTitle(),
                    form.getExpressions()[charge-1],
                    "Number of Spectra",
                    collection,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(),
                    chart,
                    form.getWidth(),
                    form.getHeight(),
                    new ChartRenderingInfo(new StandardEntityCollection()));
        }
    }

    public static ActionURL urlChartDiscriminateROC(Container c, ChartDiscForm form)
    {
        return new ActionURL(ChartDiscriminateROCAction.class, c)
                .addParameter(ChartDiscForm.PARAMS.runId, form.getRunId())
                .addParameter(ChartDiscForm.PARAMS.width, form.getWidth())
                .addParameter(ChartDiscForm.PARAMS.height, form.getHeight())
                .addParameter(ChartDiscForm.PARAMS.expressions + "_" + 0, form.getExpressions()[0])
                .addParameter(ChartDiscForm.PARAMS.expressions + "_" + 1, form.getExpressions()[1])
                .addParameter(ChartDiscForm.PARAMS.expressions + "_" + 2, form.getExpressions()[2]);
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ChartDiscriminateROCAction extends ExportAction<ChartDiscForm>
    {
        public void export(ChartDiscForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            Container c = getContainer();

            form.initArrays(getViewContext().getRequest());

            /* @todo: error handling. */

            XYSeriesCollection collection =
                    MS2Manager.getDiscriminateROCData(form.getRunId(),
                            form.getExpressions(),
                            0.2,
                            150,
                            new int[] {0, 50}
                    );

            JFreeChart chart = ChartFactory.createXYLineChart(form.getTitle(),
                    "False Positives",
                    "Correct IDs",
                    collection,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);

            XYPlot plot = chart.getXYPlot();
            for (int i = 0; i < collection.getSeriesCount(); i++)
            {
                final Paint paint = plot.getRenderer().getSeriesPaint(i);
                MS2Manager.XYSeriesROC series = (MS2Manager.XYSeriesROC) collection.getSeries(i);
                series.plotAnnotations(plot, paint);
            }

            response.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(response.getOutputStream(),
                    chart,
                    form.getWidth(),
                    form.getHeight(),
                    new ChartRenderingInfo(new StandardEntityCollection()));
        }
    }

    public static class ChartDiscForm
    {
        enum PARAMS
        {
            title,
            runId,
            charge,
            width,
            height,
            percentAACorrect,
            buckets,
            expressions,
            scaleFactors
        }

        private String title = "Discriminate";
        private int runId;
        private MS2Run run;
        private int charge = 2;
        private double percentAACorrect = 0.1;
        private int width = 500;
        private int height = 500;
        private double[] buckets = new double[] { 0.2, 0.1, 0.1 };
        private String[] expressions = new String[]
                { "-log(expect)", "-log(expect)", "-log(expect)" };
        private int[] scaleFactors = new int[] { 1, 2, 4 };

        public void initArrays(HttpServletRequest request)
        {
            for (int i = 0; i < 3; i++)
            {
                String param = request.getParameter(PARAMS.buckets.toString() + "_" + i);
                if (param != null)
                {
                    try
                    {
                        buckets[i] = Double.parseDouble(param);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
            }
            for (int i = 0; i < 3; i++)
            {
                String param = request.getParameter(PARAMS.expressions.toString() + "_" + i);
                if (param != null)
                    expressions[i] = param;
            }
            for (int i = 0; i < 3; i++)
            {
                String param = request.getParameter(PARAMS.scaleFactors.toString() + "_" + i);
                if (param != null)
                {
                    try
                    {
                        scaleFactors[i] = Integer.parseInt(param);
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
            }
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }

        public int getRunId()
        {
            return runId;
        }

        public void setRunId(int runId)
        {
            this.runId = runId;
        }

        public MS2Run getRun()
        {
            return run;
        }

        public void setRun(MS2Run run)
        {
            this.run = run;
        }

        public int getCharge()
        {
            return charge;
        }

        public void setCharge(int charge)
        {
            this.charge = charge;
        }

        public int getWidth()
        {
            return width;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }

        public int getHeight()
        {
            return height;
        }

        public void setHeight(int height)
        {
            this.height = height;
        }

        public String[] getExpressions()
        {
            return expressions;
        }

        public void setExpressions(String[] expressions)
        {
            this.expressions = expressions;
        }

        public double[] getBuckets()
        {
            return buckets;
        }

        public void setBuckets(double[] buckets)
        {
            this.buckets = buckets;
        }

        public int[] getScaleFactors()
        {
            return scaleFactors;
        }

        public void setScaleFactors(int[] scaleFactors)
        {
            this.scaleFactors = scaleFactors;
        }

        public double getPercentAACorrect()
        {
            return percentAACorrect;
        }

        public void setPercentAACorrect(double percentAACorrect)
        {
            this.percentAACorrect = percentAACorrect;
        }
    }
}
