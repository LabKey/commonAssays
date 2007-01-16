package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.model.*;

import java.util.ArrayList;
import java.util.List;

class ChannelData
{
    static private SubsetSpec compSubset = SubsetSpec.fromString("comp");

    // Returns the SSC-H field, or whatever field is appropriate for histogram data
    public DataFrame.Field findHistogramField(DataFrame data)
    {
        DataFrame.Field field = data.getField("SSC-H");
        if (field == null)
        {
            field = data.getField("SSC-A");
        }
        if (field == null)
        {
            field = data.getField("FSC-A");
        }
        if (field == null)
        {
            field = data.getField("FSC-H");
        }
        if (field == null)
        {
            field = data.getField(0);
        }
        return field;
    }


    CompHandler _handler;
    final ChannelKey _key;
    private CompHandler.FileData _file;
    CompensationCalculation.ChannelSubset _channelSubset;

    PopulationSet _populationSet;
    Subset _subset;
    Subset _subsetParent;
    double _count;
    double[] _medians;

    List<FCSAnalyzer.Result> results;

    public ChannelData(CompHandler handler, CompSign sign, int index)
    {
        this._handler = handler;
        this._key = new ChannelKey(sign, _handler._calc.getChannels().get(index).getName());
        CompensationCalculation.ChannelInfo ci = _handler._calc.getChannels().get(index);
        this._channelSubset = _key._sign == CompSign.positive ? ci.getPositive() : ci.getNegative();
        results = new ArrayList();
        this._populationSet = _handler._analyzer.findSubGroup(_handler._calc, this._channelSubset.getSubset());
    }

    public void setFile(CompHandler.FileData file)
    {
        _file = file;
        setSubset(_handler._analyzer.getSubset(_file.subsetMap, _handler._calc, _channelSubset.getSubset()));
    }

    public String getChannelName()
    {
        return _key.getName();
    }

    public double calculateStatistic(Subset subset, StatisticSpec spec)
    {
        FCSAnalyzer.StatResult result = new FCSAnalyzer.StatResult(spec);
        results.add(result);
        try
        {
            result.value = StatisticSpec.calculate(subset, spec);
            return result.value;
        }
        catch (Throwable t)
        {
            result.exception = t;
            return 0;
        }
    }

    private String makeCompensated(String str)
    {
        return CompensationMatrix.PREFIX + str + CompensationMatrix.SUFFIX;
    }



    public void drawGraph(Subset subset, String parameter, boolean compensated)
    {
        String xAxis = getChannelName();
        String yAxis = parameter;
        if (xAxis.equals(yAxis))
        {
            DataFrame.Field yAxisField = findHistogramField(subset.getDataFrame());
            yAxis = yAxisField.getName();
        }
        else
        {
            if (compensated)
            {
                yAxis = makeCompensated(yAxis);
            }
        }

        if (compensated)
        {
            xAxis = makeCompensated(xAxis);
        }

        GraphSpec spec = new GraphSpec(null, compensated ? makeCompensated(parameter) : parameter);

        List<Polygon> polys = new ArrayList();
        if (!compensated)
        {
            if (_populationSet instanceof Population)
            {
                Population positivePop = (Population) _populationSet;
                for (Gate gate : positivePop.getGates())
                {
                    gate.getPolygons(polys, getChannelName(), getChannelName());
                }
            }
        }
        List<Polygon> displayPolys = new ArrayList();
        if (polys.size() > 0)
        {
            double xMin = Double.MAX_VALUE;
            double xMax = Double.MIN_VALUE;
            double yMin = Double.MIN_VALUE;
            double yMax = Double.MAX_VALUE;
            for (Polygon poly : polys)
            {
                for (Double x : poly.X)
                {
                    xMin = Math.min(x, xMin);
                    xMax = Math.max(x, xMax);
                }
            }
            double[] X = new double[] { xMin, xMax, xMax, xMin };
            double[] Y = new double[] { yMin, yMin, yMax, yMax };
            displayPolys.add(new Polygon(X, Y));
        }

        FCSAnalyzer.GraphResult result = new FCSAnalyzer.GraphResult(spec);
        results.add(result);
        StringBuilder title = new StringBuilder();
        if (compensated)
            title.append("comp-");
        title.append(getChannelName());
        title.append(_key.getSign() == CompSign.positive ? "+" : "-");
        title.append(":");
        title.append(parameter);
        try
        {
            result.bytes = _handler._analyzer.generateGraph(title.toString(), subset, xAxis, yAxis, displayPolys);
        }
        catch (Throwable t)
        {
            result.exception = t;
        }
    }

    public void setSubset(Subset subset)
    {
        this._subset = subset;
        _subsetParent = subset.getParent();
        if (_subsetParent == null)
        {
            _subsetParent = subset;
        }
    }

    public void calculateUncompensatedValues()
    {
        _count = calculateStatistic(_subset, new StatisticSpec(compSubset, StatisticSpec.STAT.Count, null));
        calculateStatistic(_subset, new StatisticSpec(compSubset, StatisticSpec.STAT.Freq_Of_Parent, null));
        int channelCount = _handler._calc.getChannelCount();
        _medians = new double[channelCount];
        for (int i = 0; i < channelCount; i ++)
        {
            CompensationCalculation.ChannelInfo info = _handler._calc.getChannelInfo(i);
            _medians[i] = calculateStatistic(_subset, new StatisticSpec(compSubset, StatisticSpec.STAT.Median, info.getName()));
            drawGraph(_subsetParent, info.getName(), false);
        }
    }

    public void calculateCompensatedValues(CompensationMatrix matrix)
    {
        Subset parentComp = _subsetParent.apply(matrix);
        int channelCount = _handler._calc.getChannelCount();
        for (int i = 0; i < channelCount; i ++)
        {
            String channelName = _handler._calc.getChannelName(i);
            drawGraph(parentComp, channelName, true);
        }
    }

    public CompensationResult getCompensationResult()
    {
        CompensationResult ret = new CompensationResult(_key.getSign(), _key.getName(), _file.header.getURI());
        for (FCSAnalyzer.Result result : results)
        {
            ret.addResult(result);
        }
        return ret;
    }
}
