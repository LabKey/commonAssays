package Flow.EditScript;

import com.labkey.flow.model.FlowJoWorkspace;
import com.labkey.flow.model.Analysis;
import com.labkey.flow.model.Population;
import com.labkey.flow.web.SubsetSpec;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.fhcrc.cpas.flow.script.xml.*;

abstract public class CompensationCalculationPage extends ScriptController.Page<EditCompensationCalculationForm>
{
    public enum Sign
    {
        positive,
        negative
    }

    public CompensationCalculationDef compensationCalculationDef()
    {
        ScriptDocument doc = getScriptDocument();
        if (doc == null)
            return null;
        ScriptDef script = doc.getScript();
        if (script == null)
            return null;
        return script.getCompensationCalculation();
    }
    public ChannelDef channelDef(ScriptDocument doc, String channel)
    {
        CompensationCalculationDef calc = compensationCalculationDef();
        if (calc == null)
            return null;
        for (ChannelDef channelDef : calc.getChannelArray())
        {
            if (channel.equals(channelDef.getName()))
                return channelDef;
        }
        return null;
    }

    public ScriptDocument getScriptDocument()
    {
        return form.analysisDocument;
    }

    public ChannelSubsetDef channelSubset(ScriptDocument doc, Sign sign, String channel)
    {
        ChannelDef channelDef = channelDef(doc, channel);
        if (channelDef == null)
            return null;
        switch (sign)
        {
            case positive:
                return channelDef.getPositive();
            case negative:
                return channelDef.getNegative();
        }
        return null;
    }
    public Map<String,Map<String, FlowJoWorkspace.SampleInfo>> keywordValueSampleMap;

    public void setForm(EditCompensationCalculationForm form)
    {
        super.setForm(form);
        if (form.workspace == null)
            return;
        this.keywordValueSampleMap = getKeywordValueSampleMap(form.workspace);
    }

    private void addSubsetNames(Population pop, SubsetSpec parent, List<String> list)
    {
        SubsetSpec cur = new SubsetSpec(parent, pop.getName());
        list.add(cur.toString());
        for (Population child : pop.getPopulations())
        {
            addSubsetNames(child, cur, list);
        }
    }

    public String[] getSubsetNames(Analysis analysis)
    {
        List<String> ret = new ArrayList();
        for (Population pop : analysis.getPopulations())
        {
            addSubsetNames(pop, null, ret);
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Walks all of the samples in the workspace, looking for keyword/value pairs that uniquely identify a sample.
     * For each pair that is found, returns the list of subset names.
     * @param workspace
     */
    public Map<String, Map<String, FlowJoWorkspace.SampleInfo>> getKeywordValueSampleMap(FlowJoWorkspace workspace)
    {
        Set<String> keywordsSet = new TreeSet();
        Map<FlowJoWorkspace.SampleInfo,String[]> sampleSubsetMap = new HashMap();
        for (FlowJoWorkspace.SampleInfo sample : workspace.getSamples()) {
            Analysis analysis = workspace.getSampleAnalysis(sample);
            if (analysis == null)
                continue;
            sampleSubsetMap.put(sample, getSubsetNames(analysis));
            keywordsSet.addAll(sample.getKeywords().keySet());
        }
        Map<String,Map<String, FlowJoWorkspace.SampleInfo>> keywordValueSubsetListMap = new LinkedHashMap();
        for (String keyword : keywordsSet)
        {
            Map<String, List<FlowJoWorkspace.SampleInfo>> sampleMap = new LinkedHashMap();
            for (FlowJoWorkspace.SampleInfo sample : sampleSubsetMap.keySet())
            {
                String value = sample.getKeywords().get(keyword);
                if (value != null)
                {
                    List<FlowJoWorkspace.SampleInfo> list = sampleMap.get(value);
                    if (list == null)
                    {
                        list = new ArrayList();
                        sampleMap.put(value, list);
                    }
                    list.add(sample);
                }
            }
            Map<String, FlowJoWorkspace.SampleInfo> valueSampleMap = new TreeMap();
            for (Map.Entry<String, List<FlowJoWorkspace.SampleInfo>> entry : sampleMap.entrySet())
            {
                if (entry.getValue().size() != 1)
                    continue;

                valueSampleMap.put(entry.getKey(), entry.getValue().get(0));
            }
            if (valueSampleMap.size() > 0)
            {
                keywordValueSubsetListMap.put(keyword, valueSampleMap);
            }
        }
        return keywordValueSubsetListMap;
    }
    public String javascriptArray(String[] strings)
    {
        if (strings.length == 0)
            return "[]";
        return "['" + StringUtils.join(strings, "',\n'") + "']";
    }

    public String option(String value, String display, String currentValue)
    {
        boolean selected = currentValue != null && currentValue.equals(value);
        StringBuilder ret = new StringBuilder("<option value=\"" + h(value) + "\"");
        if (selected)
            ret.append(" selected");
        ret.append(">" + h(display) + "</option>\n");
        return ret.toString();
    }

    public String getKeywordName(Sign sign, int index)
    {
        return sign == Sign.positive ? form.positiveKeywordName[index] : form.negativeKeywordName[index];
    }
    public String getKeywordValue(Sign sign, int index)
    {
        return sign == Sign.positive ? form.positiveKeywordValue[index] : form.negativeKeywordValue[index];
    }
    public String getSubset(Sign sign, int index)
    {
        return sign == Sign.positive ? form.positiveSubset[index] : form.negativeSubset[index];
    }

    public String selectKeywordNames(Sign sign, int index)
    {
        StringBuilder ret = new StringBuilder();
        String current = getKeywordName(sign, index);
        ret.append("<select name=\"" + sign + "KeywordName[" + index + "]\"");
        ret.append(" onChange=\"populateKeywordValues('" + sign + "'," + index + ")\">");
        ret.append(option("", "", current));
        for (String keyword : keywordValueSampleMap.keySet())
        {
            ret.append(option(keyword, keyword, current));
        }
        ret.append("\n</select>");
        return ret.toString();
    }


    public String selectKeywordValues(Sign sign, int index)
    {
        StringBuilder ret = new StringBuilder();
        Map<String, FlowJoWorkspace.SampleInfo> valueSubsetMap = this.keywordValueSampleMap.get(getKeywordName(sign, index));
        String current = getKeywordValue(sign, index);
        String[] options;
        if (valueSubsetMap == null)
        {
            options = new String[0];
        }
        else
        {
            options = valueSubsetMap.keySet().toArray(new String[0]);
        }
        ret.append("<select name=\"" + sign + "KeywordValue[" + index + "]\"");
        ret.append("onChange=\"populateSubsets('" + sign + "'," + index + ")\">");
        ret.append(option("", "", current));
        for (String option : options)
        {
            ret.append(option(option, option, current));
        }
        ret.append("</select>");
        return ret.toString();
    }

    private boolean subsetNameMatches(String subsetUser, String subsetWorkspace, Sign sign, String channel)
    {
        if (StringUtils.equals(subsetUser, subsetWorkspace))
            return true;
        if (StringUtils.equals(subsetUser, channel + subsetWorkspace))
            return true;
        String strSign = sign == Sign.positive ? "+" : "-";
        if (StringUtils.equals(subsetUser, channel + strSign + subsetWorkspace))
            return true;
        if (StringUtils.equals(subsetUser, strSign + subsetWorkspace))
            return true;
        return false;
    }
    /**
     * The edit compensation calculation page munges some subset names so that they are unique, even
     * when the analyses for all of the channels are combined.
     * This method will return true if subsetUser is a subset name that could have possible come
     * from subsetWorkspace.
     */
    private boolean subsetMatches(SubsetSpec subsetUser, SubsetSpec subsetWorkspace, Sign sign, int index)
    {
        if (subsetUser == null && subsetWorkspace == null)
            return true;
        if (subsetUser == null || subsetWorkspace == null)
            return false;
        String[] userSubsets = subsetUser.getSubsets();
        String[] workspaceSubsets = subsetWorkspace.getSubsets();
        if (userSubsets.length != workspaceSubsets.length)
            return false;
        String strChannel = this.form.parameters[index];
        for (int i = 0; i < userSubsets.length; i ++)
        {
            if (!subsetNameMatches(userSubsets[i], workspaceSubsets[i], sign, strChannel))
                return false;
        }
        return true;
    }

    public String selectSubsets(Sign sign, int index)
    {
        StringBuilder ret = new StringBuilder();
        Map<String, FlowJoWorkspace.SampleInfo> valueSubsetMap = this.keywordValueSampleMap.get(getKeywordName(sign, index));
        String[] subsets = new String[0];
        if (valueSubsetMap == null)
        {
        }
        else
        {
            FlowJoWorkspace.SampleInfo sample = valueSubsetMap.get(getKeywordValue(sign, index));
            if (sample != null)
            {
                Analysis analysis = form.workspace.getSampleAnalysis(sample);
                if (analysis != null)
                {
                    subsets = getSubsetNames(analysis);
                }
            }
        }
        SubsetSpec current = SubsetSpec.fromString(getSubset(sign, index));
        ret.append("<select name=\"" + sign + "Subset[" + index + "]\">");
        ret.append(option("", "Ungated", ""));
        for (String subset : subsets)
        {
            SubsetSpec workspaceSubset = SubsetSpec.fromString(subset);
            boolean selected = subsetMatches(current, workspaceSubset, sign, index);
            ret.append("\n<option value=\"" + h(subset) + "\"");
            if (selected)
            {
                ret.append(" selected");
            }
            ret.append(">" + h(subset) + "</option>");
        }
        ret.append("</select>");
        return ret.toString();
    }
}
