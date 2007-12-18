package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.chart.DensityPlot;
import org.labkey.flow.analysis.chart.PlotFactory;
import org.labkey.flow.analysis.chart.HistPlot;
import org.labkey.flow.analysis.model.CompensationCalculation;
import org.labkey.flow.analysis.model.Polygon;

import java.net.URI;
import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.ui.RectangleInsets;
import org.jfree.data.Range;
import org.apache.log4j.Logger;
import org.labkey.api.view.Stats;

import javax.imageio.ImageIO;

public class FCSAnalyzer
{
    static public final SubsetSpec compSubset = new SubsetSpec(null, "comp");
    static private FCSAnalyzer instance;
    static private Logger _log = Logger.getLogger(FCSAnalyzer.class);
    static private int GRAPH_HEIGHT = 300;
    static private int GRAPH_WIDTH = 300;

    static public class Result<T>
    {
        public T spec;
        public Throwable exception;

        public Result(T spec)
        {
            this.spec = spec;
        }
    }

    static public class StatResult extends Result<StatisticSpec>
    {
        public double value;

        public StatResult(StatisticSpec spec)
        {
            super(spec);
        }
    }

    static public class GraphResult extends Result<GraphSpec>
    {
        public byte[] bytes;

        public GraphResult(GraphSpec spec)
        {
            super(spec);
        }
    }

    synchronized static public FCSAnalyzer get()
    {
        if (instance == null)
        {
            instance = new FCSAnalyzer();
        }
        return instance;
    }

    FCSCache _cache = new FCSCache();

    public FCS readFCS(URI uri) throws IOException
    {
        return _cache.readFCS(uri);
    }

    public PopulationSet findSubGroup(PopulationSet group, SubsetSpec subset)
    {
        if (subset == null) return group;
        for (String name : subset.getSubsets())
        {
            group = group.getPopulation(name);
        }
        return group;
    }

    public byte[] generateGraph(String title, Subset subset, String[] axes, List<Polygon> polys) throws Exception
    {
        Plot plot;
        if (axes.length == 2)
        {
            DensityPlot dPlot = PlotFactory.createContourPlot(subset, axes[0], axes[1]);
            for (Polygon poly : polys)
            {
                dPlot.addPolygon("", poly);
            }
            plot = dPlot;
        }
        else
        {
            HistPlot hPlot = PlotFactory.createHistogramPlot(subset, axes[0]);
            for (Polygon poly : polys)
            {
                hPlot.addGate(new Range(poly.xmin, poly.xmax));
            }
            plot = hPlot;
        }
        JFreeChart chart = new JFreeChart(title, plot);
        BufferedImage img = chart.createBufferedImage(GRAPH_HEIGHT, GRAPH_WIDTH);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] generateGraph(String title, Subset subset, PopulationSet subGroup, String[] axes) throws Exception
    {
        List<Polygon> polys = new ArrayList();
        String xAxis = axes[0];
        String yAxis;
        if (axes.length == 2)
        {
            yAxis = axes[1];
        }
        else
        {
            yAxis = xAxis;
        }

        for (Population pop : subGroup.getPopulations())
        {
            for (Gate gate : pop.getGates())
            {
                gate.getPolygons(polys, xAxis, yAxis);
            }
        }
        return generateGraph(title, subset, axes, polys);
    }

    private GraphResult generateGraph(Map<SubsetSpec, Subset> subsetMap, PopulationSet group, GraphSpec graphSpecification) throws IOException
    {
        GraphResult ret = new GraphResult(graphSpecification);
        try
        {
            Subset subset = getSubset(subsetMap, group, graphSpecification.getSubset());
            PopulationSet subGroup = findSubGroup(group, graphSpecification.getSubset());
            ret.bytes = generateGraph(subset.getName(), subset, subGroup, graphSpecification.getParameters());
        }
        catch (Throwable t)
        {
            ret.exception = t;
        }
        return ret;
    }

    public List<GraphResult> generateGraphs(URI uri, CompensationMatrix comp, ScriptComponent group, Collection<GraphSpec> graphs) throws IOException
    {
        if (graphs.size() == 0)
        {
            return Collections.EMPTY_LIST;
        }
        List<GraphResult> ret = new ArrayList(graphs.size());
        Map<SubsetSpec, Subset> subsetMap = new HashMap();
        subsetMap.put(null, getSubset(uri, group.getSettings(), comp));
        for (GraphSpec graph : graphs)
        {
            ret.add(generateGraph(subsetMap, group, graph));
        }
        return ret;
    }

    public PlotInfo generateDesignGraph(URI uri, CompensationMatrix comp, ScriptComponent group, GraphSpec spec, int width, int height, boolean useEmptyDataset) throws IOException
    {
        Map<SubsetSpec, Subset> subsetMap = new HashMap();
        if (useEmptyDataset)
        {
            subsetMap.put(null, getEmptySubset(uri, group.getSettings(), comp));
        }
        else
        {
            subsetMap.put(null, getSubset(uri, group.getSettings(), comp));
        }
        Subset subset = getSubset(subsetMap, group, spec.getSubset());
        ValueAxis domainAxis, rangeAxis;
        JFreeChart chart;
        if (spec.getParameters().length == 1)
        {
            HistPlot plot = PlotFactory.createHistogramPlot(subset, spec.getParameters()[0]);
            plot.setInsets(new RectangleInsets(0, 0, 0, 0));
            domainAxis = plot.getDomainAxis();
            rangeAxis = plot.getRangeAxis();
            chart = new JFreeChart(plot);
        }
        else
        {
            DensityPlot plot = PlotFactory.createContourPlot(subset, spec.getParameters()[0], spec.getParameters()[1]);
            plot.setToolTipGenerator(null);
            plot.setInsets(new RectangleInsets(0, 0, 0, 0));
            domainAxis = plot.getDomainAxis();
            rangeAxis = plot.getRangeAxis();
            chart = new JFreeChart(plot);
        }
        ChartRenderingInfo info = new ChartRenderingInfo(null);
        BufferedImage img = chart.createBufferedImage(width, height, info);
        return new PlotInfo(subset, img, info, domainAxis, rangeAxis);
    }

    private List<StatResult> calculateStatistics(Map<SubsetSpec, Subset> subsetMap, ScriptComponent group, Collection<StatisticSpec> stats) throws IOException
    {
        List<StatResult> ret = new ArrayList(stats.size());
        Map<SubsetSpec, Map<String, Stats.DoubleStats>> subsetStatsMap = new HashMap();
        for (StatisticSpec stat : stats)
        {
            StatResult result = new StatResult(stat);
            ret.add(result);
            StatisticSpec statisticSpecification = result.spec;
            try
            {
                Subset subset = getSubset(subsetMap, group, statisticSpecification.getSubset());
                Map<String, Stats.DoubleStats> statsMap = subsetStatsMap.get(statisticSpecification.getSubset());
                if (statsMap == null)
                {
                    statsMap = new HashMap();
                    subsetStatsMap.put(statisticSpecification.getSubset(), statsMap);
                }
                result.value = StatisticSpec.calculate(subset, statisticSpecification, statsMap);
            }
            catch (Throwable t)
            {
                result.exception = t;
            }
        }
        return ret;
    }

    public List<StatResult> calculateStatistics(URI uri, CompensationMatrix comp, Analysis analysis) throws IOException
    {
        Map<SubsetSpec, Subset> subsetMap = new HashMap();
        Subset root = getSubset(uri, analysis.getSettings(), comp);
        subsetMap.put(null, root);
        Collection<StatisticSpec> stats = analysis.materializeStatistics(root);
        return calculateStatistics(subsetMap, analysis, stats);
    }

    public List<StatResult> calculateStatistics(URI uri, CompensationMatrix comp, ScriptComponent group, Collection<StatisticSpec> stats) throws IOException
    {
        Map<SubsetSpec, Subset> subsetMap = new HashMap();
        subsetMap.put(null, getSubset(uri, group.getSettings(), comp));

        return calculateStatistics(subsetMap, group, stats);
    }

    protected Subset getSubset(URI uri, ScriptSettings settings, CompensationMatrix comp) throws IOException
    {
        FCS fcs = _cache.readFCS(uri);
        Subset subset = new Subset(fcs, settings);
        if (comp != null && !comp.isSingular())
        {
            subset = subset.apply(comp);
        }
        return subset;
    }

    protected Subset getEmptySubset(URI uri, ScriptSettings settings, CompensationMatrix comp) throws IOException
    {
        FCSHeader header = _cache.readFCSHeader(uri);
        DataFrame data = header.createEmptyDataFrame();
        data = data.translate(settings);
        Subset subset = new Subset(null, null, header, data);
        if (comp != null && !comp.isSingular())
        {
            subset = subset.apply(comp);
        }
        return subset;
    }

    public Subset getSubset(Map<SubsetSpec, Subset> subsetMap, PopulationSet group, SubsetSpec subsetSpecification)
    {
        Subset ret = subsetMap.get(subsetSpecification);
        if (ret != null) return ret;
        Subset parentSubset = getSubset(subsetMap, group, subsetSpecification.getParent());
        if (subsetSpecification.isExpression())
        {
            PopulationSet parentPop = findSubGroup(group, subsetSpecification.getParent());
            SubsetExpression expr = SubsetExpression.fromString(subsetSpecification.getSubset());
            BitSet bits = expr.apply(parentSubset, parentPop);
            ret = parentSubset.apply(subsetSpecification.getSubset(), bits);
        }
        else
        {
            Population pop = (Population) findSubGroup(group, subsetSpecification);
            if (pop == null) throw new FlowException("Could not find subset " + subsetSpecification);
            ret = parentSubset.apply(pop.getName(), pop.getGates().toArray(new Gate[0]));
        }
        subsetMap.put(subsetSpecification, ret);
        return ret;
    }

    FCSKeywordData resolveRef(FCSRef ref) throws IOException
    {
        FCSHeader header = _cache.readFCSHeader(ref.getURI());
        return new FCSKeywordData(ref, header);
    }

    public FCSKeywordData readAllKeywords(URI uri) throws IOException
    {
        return readAllKeywords(new FCSRef(uri, null));
    }

    public FCSKeywordData readAllKeywords(FCSRef fcsRef) throws IOException
    {
        return resolveRef(fcsRef);
    }

    public FCSKeywordData findHeader(List<FCSKeywordData> headers, SampleCriteria criteria)
    {
        FCSKeywordData header = criteria.find(headers);
        if (header == null)
        {
            throw new FlowException("Could not find sample " + criteria);
        }
        return header;
    }

    protected Subset findSubset(FCSKeywordData header, ScriptComponent group, SubsetSpec subsetSpec) throws Exception
    {
        Map<SubsetSpec, Subset> map = new HashMap();
        map.put(null, getSubset(header.getURI(), group.getSettings(), null));
        return getSubset(map, group, subsetSpec);
    }

    public CompensationMatrix calculateCompensationMatrix(List<FCSRef> fcsRefs, CompensationCalculation calc, List<CompensationResult> results) throws Exception
    {
        List<FCSKeywordData> headers = new ArrayList();
        for (FCSRef ref : fcsRefs)
        {
            headers.add(resolveRef(ref));
        }
        CompHandler handler = new CompHandler(this, calc, headers);
        return handler.calculateCompensationMatrix(results);
    }

    private List<String> getFieldNames(FCSHeader header)
    {
        List<String> ret = new ArrayList();
        for (int i = 1; ; i++)
        {
            String keyword = "$P" + i + "N";
            String name = header.getKeyword(keyword);
            if (name == null)
            {
                return ret;
            }
            ret.add(name);
        }
    }

    public Map<String, String> getParameterNames(URI uriFCS, String[] compChannelNames) throws Exception
    {
        LinkedHashMap<String, String> ret = new LinkedHashMap();
        if (uriFCS == null)
        {
            return ret;
        }
        FCSHeader fcs = _cache.readFCSHeader(uriFCS);
        if (fcs == null)
        {
            return ret;
        }
        List<String> names = getFieldNames(fcs);
        for (int i = 0; i < names.size(); i++)
        {
            ret.put(names.get(i), PlotFactory.getLabel(fcs, i, false));
        }
        for (String channel : compChannelNames)
        {
            int index = names.indexOf(channel);
            if (index >= 0)
            {
                ret.put(CompensationMatrix.PREFIX + channel + CompensationMatrix.SUFFIX, PlotFactory.getLabel(fcs, index, true));
            }
        }
        return ret;
    }

    public Map<String, String> getParameterNames(URI uriFCS, CompensationMatrix comp) throws Exception
    {
        String[] channelNames = new String[0];
        if (comp != null)
        {
            channelNames = comp.getChannelNames();
        }
        return getParameterNames(uriFCS, channelNames);
    }

    public Map<String, String> getParameterNames(URI uriFCS, CompensationCalculation calc) throws Exception
    {
        String[] channelNames = new String[0];
        if (calc != null)
        {
            List<CompensationCalculation.ChannelInfo> channels = calc.getChannels();
            channelNames = new String[channels.size()];
            for (int i = 0; i < channelNames.length; i++)
            {
                channelNames[i] = channels.get(i).getName();
            }
        }
        return getParameterNames(uriFCS, channelNames);
    }

    /**
     * Match a FCS header against a list of criteria.
     * @return true if all criteria matches; false otherwise.
     */
    public boolean matchesCriteria(Collection<SampleCriteria> criteria, FCSRef ref) throws IOException
    {
        if (criteria == null) return true;
        FCSKeywordData header = resolveRef(ref);
        for (SampleCriteria c : criteria)
        {
            if (!c.matches(header))
                return false;
        }
        return true;
    }

    public boolean matchesCriteria(SampleCriteria criteria, FCSRef ref) throws Exception
    {
        if (criteria == null) return true;
        FCSKeywordData header = resolveRef(ref);
        return criteria.matches(header);
    }

    public boolean isFCSFile(File file)
    {
        return FCS.isFCSFile(file);
    }

    public byte[] getFCSBytes(URI uri, int maxEventCount) throws Exception
    {
        FCS fcs = _cache.readFCS(uri);
        return fcs.getFCSBytes(new File(uri), maxEventCount);
    }

    public boolean containsFCSFiles(File directory)
    {
        if (!directory.isDirectory()) return false;
        try
        {
            for (File file : directory.listFiles())
            {
                if (isFCSFile(file)) return true;
            }
        }
        catch (Exception e)
        {
            _log.error("Error", e);
        }
        return false;
    }

    public Dimension getGraphSize()
    {
        Dimension ret = new Dimension();
        ret.width = GRAPH_WIDTH;
        ret.height = GRAPH_HEIGHT;
        return ret;
    }
}
