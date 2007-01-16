package org.labkey.flow.controllers.editscript;

import org.labkey.flow.analysis.model.*;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.data.FlowWell;
import org.labkey.flow.data.FlowProtocolStep;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

abstract public class EditGatesPage extends ScriptController.Page<EditGatesForm>
{
    public Map<String, String> getParameters() throws Exception
    {
        CompensationMatrix comp = null;
        if (form.getCompensationMatrix() != null && form.step != FlowProtocolStep.calculateCompensation)
        {
            comp = form.getCompensationMatrix().getCompensationMatrix();
        }

        Map<String, String> params = FlowAnalyzer.getParameters(form.well, comp);


        return params;
    }

    protected String quote(String value)
    {
        return "'" + value + "'";
    }

    private void jscriptPopulationSet(StringBuilder buf, PopulationSet pop)
    {
        buf.append("populations:[");
        for (int i = 0; i < pop.getPopulations().size(); i ++)
        {
            if (i != 0)
                buf.append(",");
            jscriptPopulation(buf, pop.getPopulations().get(i));
        }
        buf.append("]");
    }

    private void jscriptPopulation(StringBuilder buf, Population pop)
    {
        buf.append("{name:" + quote(pop.getName()));
        if (pop.getGates().size() == 1)
        {
            Gate gate = pop.getGates().get(0);
            if (gate instanceof PolygonGate)
            {
                PolygonGate polyGate = (PolygonGate) gate;
                buf.append("\n,gate: {\n");
                buf.append("x:" + quote(polyGate.getX()) + ",\n");
                buf.append("y:" + quote(polyGate.getY()) + ",\n");
                buf.append("points:[\n");
                Polygon poly = polyGate.getPolygon();
                for (int i = 0; i < poly.len; i ++)
                {
                    if (i != 0)
                        buf.append(",\n");
                    buf.append("{ x : " + poly.X[i] + ",\n");
                    buf.append("y : " + poly.Y[i] + "}\n");
                }
                buf.append("]\n");
            }
        }
        buf.append("\n,");
        jscriptPopulationSet(buf, pop);
        buf.append("}");
    }

    public String jscriptAnalysis(PopulationSet analysis)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("{");
        jscriptPopulationSet(ret, analysis);
        ret.append("};");
        return ret.toString();
    }

    private boolean isRelevent(SubsetSpec subset, CompensationCalculation.ChannelSubset channelSubset)
    {
        SubsetSpec subsetCompare = channelSubset.getSubset();
        if (subsetCompare == null)
        {
            return false;
        }
        return subsetCompare.toString().startsWith(subset.toString());
    }

    private List<CompensationCalculation.ChannelSubset> getReleventSubsets(SubsetSpec subset, CompensationCalculation calc)
    {
        List<CompensationCalculation.ChannelSubset> matches = new ArrayList();
        for (CompensationCalculation.ChannelInfo channel : calc.getChannels())
        {
            CompensationCalculation.ChannelSubset positiveSubset = channel.getPositive();
            CompensationCalculation.ChannelSubset negativeSubset = channel.getNegative();
            if (isRelevent(subset, positiveSubset))
            {
                matches.add(positiveSubset);
            }
            if (isRelevent(subset, negativeSubset))
            {
                matches.add(negativeSubset);
            }
        }
        return matches;
    }

    public FlowWell getReleventWell(SubsetSpec subset, CompensationCalculation calc, FlowWell[] wells, FCSKeywordData[] headers)
    {
        List<FCSKeywordData> lstHeaders = Arrays.asList(headers);
        List<CompensationCalculation.ChannelSubset> matches = getReleventSubsets(subset, calc);
        if (matches.size() != 1)
            return null;
        try
        {
            FCSKeywordData match = FCSAnalyzer.get().findHeader(lstHeaders, matches.get(0).getCriteria());
            if (match != null)
            {
                return wells[lstHeaders.indexOf(match)];
            }
        }
        catch (FlowException e)
        {
            return null;
        }
        return null;
    }
}
