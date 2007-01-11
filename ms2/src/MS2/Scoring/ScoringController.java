/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package MS2.Scoring;

import org.labkey.api.view.*;
import org.labkey.api.security.ACL;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.ms2.MS2Manager;
import org.labkey.api.ms2.MS2Run;
import org.apache.log4j.Logger;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.struts.action.ActionMapping;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.Enumeration;
import java.awt.*;

/**
 */
@Jpf.Controller
public class ScoringController extends ViewController
{
    private static Logger _log = Logger.getLogger("org.labkey.api." + ScoringController.class);

    private Forward _renderInTemplate(HttpView view, String title, String helpTopic) throws Exception
    {
        if (helpTopic == null)
            helpTopic = "ms2";

        NavTrailConfig trailConfig = new NavTrailConfig(getViewContext());
        if (title != null)
            trailConfig.setTitle(title);
        trailConfig.setHelpTopic(helpTopic);

        return includeView(new HomeTemplate(getViewContext(), view, trailConfig));
    }

    @Jpf.Action
    protected Forward begin()
    {
        return null;
    }

    @Jpf.Action
    protected Forward compare(ChartForm form) throws ServletException, URISyntaxException, Exception
    {
        requiresPermission(ACL.PERM_READ);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        String[] compareRunIds = null;

        int[] runIds = form.getRunIds();
        if (runIds == null || runIds.length == 0)
            compareRunIds = currentUrl.getParameters(DataRegion.SELECT_CHECKBOX_NAME);
        else
        {
            compareRunIds = new String[runIds.length];
            for (int i = 0; i < runIds.length; i++)
                compareRunIds[i] = Integer.toString(runIds[i]);
        }
        MS2Run[] compareRuns = null;
        String[] runErrors = null;

        String error = "";
        if (null == compareRunIds || compareRunIds.length == 0)
        {
            error = "No runs specified.";
        }
        else
        {
            compareRuns = new MS2Run[compareRunIds.length];
            runErrors = new String[compareRunIds.length];

            for (int i = 0; i < compareRuns.length; i++)
            {
                final MS2Run run = MS2Manager.getRun(compareRunIds[i]);
                if (run == null)
                    continue;
                compareRuns[i] = run;
                if (run.getNegativeHitCount() < run.getPeptideCount() / 3)
                {
                    runErrors[i] = "Insufficient negative hit data to perform analysis.";
                }
            }
        }

        HttpView v = new GroovyView("/MS2/Scoring/compare.gm");
        v.addObject("error", error);
        v.addObject("runs", compareRuns);
        v.addObject("runErrors", runErrors);
        v.addObject("form", form);

        return _renderInTemplate(v, "Compare MS2 Peptide Scoring", "MS2-Scoring/compare");
    }

    public static class ChartForm extends ViewForm
    {
        private String title = "Scoring Comparison";
        private double increment = 0.2;
        private int limit = 200;
        private int width = 500;
        private int height = 500;
        private String marks = "0,50";
        private int runIds[];
        private boolean discriminates[][];

        public void reset(ActionMapping am, HttpServletRequest request)
        {
            int size = 0;
            try
            {
                size = Integer.parseInt(request.getParameter("size"));
            }
            catch (Exception e)
            {
            }

            runIds = new int[size];
            discriminates = new boolean[size][];

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
        }

        public boolean[][] getDiscriminates()
        {
            return discriminates;
        }

        public void setDiscriminates(boolean[][] discriminates)
        {
            this.discriminates = discriminates;
        }

        public int[] getRunIds()
        {
            return runIds;
        }

        public void setRunIds(int[] runIds)
        {
            this.runIds = runIds;
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

        public String getMarks()
        {
            return marks;
        }

        public void setMarks(String marks)
        {
            this.marks = marks;
        }

        public String getTitle()
        {
            return title;
        }

        public void setTitle(String title)
        {
            this.title = title;
        }
    }

    @Jpf.Action
    protected Forward chartCompare(ChartForm form) throws ServletException, URISyntaxException, IOException
    {
        requiresPermission(ACL.PERM_READ);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        /* @todo: error handling. */

        String[] markStrs = new String[0];

        String markParam = form.getMarks();
        if (markParam != null && markParam.length() > 0)
            markStrs = markParam.split(",");

        int[] marks = new int[markStrs.length];
        for (int i = 0; i < marks.length; i++)
            marks[i] = Integer.parseInt(markStrs[i]);

        XYSeriesCollection collection =
                MS2Manager.getROCData(form.getRunIds(),
                        form.getDiscriminates(),
                        form.getIncrement(),
                        form.getLimit(),
                        marks,
                        c);

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

        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(),
                chart,
                form.getWidth(),
                form.getHeight(),
                new ChartRenderingInfo(new StandardEntityCollection()));

        return null;
    }


    public static class ChartDiscForm extends ViewForm
    {
        private String title;
        private int runId;
        private int charge = 2;
        private double percentAACorrect = 0.1;
        private int width = 500;
        private int height = 500;
        private double[] buckets = new double[] { 0.2, 0.1, 0.1 };
        private String[] expressions = new String[]
                { "-log(expect)", "-log(expect)", "-log(expect)" };
        private int[] scaleFactors = new int[] { 1, 2, 4 };

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

    @Jpf.Action
    protected Forward discriminate(ChartDiscForm form) throws ServletException, URISyntaxException, Exception
    {
        requiresPermission(ACL.PERM_READ);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        String error = "";

        int runId = form.getRunId();

        final MS2Run run = MS2Manager.getRun(runId);
        if (run == null)
            error = "No run specified.";
        if (run.getNegativeHitCount() < run.getPeptideCount() / 3)
            error = "Insufficient negative hit data to perform analysis.";

        ViewURLHelper runUrl = new ViewURLHelper("MS2-Scoring", "chartDiscriminate", getContainer());
        runUrl.deleteParameters();
        runUrl.addParameter("runId", Integer.toString(form.getRunId()));
        runUrl.addParameter("width", Integer.toString(form.getWidth()));
        runUrl.addParameter("height", Integer.toString(form.getHeight()));
        runUrl.addParameter("percentAACorrect", Double.toString(form.getPercentAACorrect()));

        ViewURLHelper urlC1 = runUrl.clone();
        urlC1.addParameter("charge", "1");
        urlC1.addParameter("buckets[0]", Double.toString(form.getBuckets()[0]));
        urlC1.addParameter("scaleFactors[0]", Integer.toString(form.getScaleFactors()[0]));
        ViewURLHelper urlC2 = runUrl.clone();
        urlC2.addParameter("charge", "2");
        urlC2.addParameter("expressions[1]", form.getExpressions()[1]);
        urlC2.addParameter("buckets[1]", Double.toString(form.getBuckets()[1]));
        urlC2.addParameter("scaleFactors[1]", Integer.toString(form.getScaleFactors()[1]));
        ViewURLHelper urlC3 = runUrl.clone();
        urlC3.addParameter("charge", "3");
        urlC3.addParameter("expressions[2]", form.getExpressions()[2]);
        urlC3.addParameter("buckets[2]", Double.toString(form.getBuckets()[2]));
        urlC3.addParameter("scaleFactors[2]", Integer.toString(form.getScaleFactors()[2]));

        if (form.title != null && form.title.length() > 0)
        {
            urlC1.addParameter("title", form.title + " (charge 1)");
            urlC2.addParameter("title", form.title + " (charge 2)");
            urlC3.addParameter("title", form.title + " (charge 3)");
        }

        ViewURLHelper urlROC = new ViewURLHelper("MS2-Scoring", "chartDiscriminateROC", getContainer());
        urlROC.addParameter("runId", Integer.toString(form.getRunId()));
        urlROC.addParameter("width", Integer.toString(form.getWidth()));
        urlROC.addParameter("height", Integer.toString(form.getHeight()));
        urlROC.addParameter("expressions[0]", form.expressions[0]);
        urlROC.addParameter("expressions[1]", form.expressions[1]);
        urlROC.addParameter("expressions[2]", form.expressions[2]);


        HttpView v = new GroovyView("/MS2/Scoring/discriminate.gm");
        v.addObject("error", error);
        v.addObject("run", run);
        v.addObject("form", form);
        v.addObject("container", c);
        v.addObject("runUrl", runUrl);
        v.addObject("urlC1", urlC1);
        v.addObject("urlC2", urlC2);
        v.addObject("urlC3", urlC3);
        v.addObject("urlROC", urlROC);

        return _renderInTemplate(v, "MS2 Peptide Scoring Discriminate Charts", "MS2-Compare/discriminate");
    }

    @Jpf.Action
    protected Forward chartDiscriminate(ChartDiscForm form) throws ServletException, URISyntaxException, IOException
    {
        requiresPermission(ACL.PERM_READ);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        /* @todo: error handling. */

        int charge = form.getCharge();
        XYSeriesCollection collection =
                MS2Manager.getDiscriminateData(form.getRunId(),
                        charge,
                        form.getPercentAACorrect(),
                        form.getExpressions()[charge-1],
                        form.getBuckets()[charge-1],
                        form.getScaleFactors()[charge-1],
                        c);

        JFreeChart chart = ChartFactory.createXYLineChart(form.getTitle(),
                form.getExpressions()[charge-1],
                "Number of Spectra",
                collection,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(),
                chart,
                form.getWidth(),
                form.getHeight(),
                new ChartRenderingInfo(new StandardEntityCollection()));

        return null;
    }

    @Jpf.Action
    protected Forward chartDiscriminateROC(ChartDiscForm form) throws ServletException, URISyntaxException, IOException
    {
        requiresPermission(ACL.PERM_READ);

        Container c = getContainer();
        ViewURLHelper currentUrl = cloneViewURLHelper();

        /* @todo: error handling. */

        XYSeriesCollection collection =
                MS2Manager.getDiscriminateROCData(form.getRunId(),
                        form.getExpressions(),
                        0.2,
                        150,
                        new int[] {0, 50},
                        c);

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

        getResponse().setContentType("image/png");
        ChartUtilities.writeChartAsPNG(getResponse().getOutputStream(),
                chart,
                form.getWidth(),
                form.getHeight(),
                new ChartRenderingInfo(new StandardEntityCollection()));

        return null;
    }
}
