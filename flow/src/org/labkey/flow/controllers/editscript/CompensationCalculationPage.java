package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang.StringUtils;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.api.query.FieldKey;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.AutoCompensationScript;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.web.SubsetSpec;

import java.util.*;

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

    public Map<String,Map<String, List<String>>> keywordValueSampleMap;

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

    public List<String> getSubsetNames(Analysis analysis)
    {
        List<String> ret = new ArrayList();
        for (Population pop : analysis.getPopulations())
        {
            addSubsetNames(pop, null, ret);
        }
        return ret;
    }

    protected boolean isValidCompKeyword(String keyword)
    {
        if ("$BEGINDATA".equals(keyword) || "$ENDDATA".equals(keyword) || "$TOT".equals(keyword))
            return false;
        if (keyword.startsWith("$P") && keyword.endsWith("V"))
        {
            return false;
        }
        return true;
    }

    /**
     * Walks all of the samples in the workspace, looking for keyword/value pairs that uniquely identify a sample.
     * For each pair that is found, returns the list of subset names.
     * @param workspace
     * @return Keyword -> Value -> Subsets
     */
    public Map<String, Map<String, List<String>>> getKeywordValueSampleMap(FlowJoWorkspace workspace)
    {
        Set<String> keywordsSet = new TreeSet();
        Map<FlowJoWorkspace.SampleInfo, List<String>> sampleSubsetMap = new HashMap();
        for (FlowJoWorkspace.SampleInfo sample : workspace.getSamples()) {
            Analysis analysis = workspace.getSampleAnalysis(sample);
            if (analysis == null)
                continue;
            sampleSubsetMap.put(sample, getSubsetNames(analysis));
            keywordsSet.addAll(sample.getKeywords().keySet());
        }
        Map<String, Map<String, List<String>>> keywordValueSubsetListMap = new LinkedHashMap();
        for (String keyword : keywordsSet)
        {
            if (!isValidCompKeyword(keyword))
                continue;
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
            Map<String, List<String>> valueSampleMap = new TreeMap();
            for (Map.Entry<String, List<FlowJoWorkspace.SampleInfo>> entry : sampleMap.entrySet())
            {
                if (entry.getValue().size() != 1)
                    continue;
                List<String> subsets = sampleSubsetMap.get(entry.getValue().get(0));
                valueSampleMap.put(entry.getKey(), subsets);
            }
            if (valueSampleMap.size() > 0)
            {
                keywordValueSubsetListMap.put(keyword, valueSampleMap);
            }
        }

        // always add keywords, values, and subsets from autocomp scripts
        for (AutoCompensationScript autoComp : workspace.getAutoCompensationScripts())
        {
            //AutoCompensationScript.MatchingCriteria criteria = autoComp.getCriteria();
            for (AutoCompensationScript.ParameterDefinition param : autoComp.getParameters().values())
            {
                Map<String, List<String>> valueSampleMap =
                    keywordValueSubsetListMap.get(param.getSearchKeyword());
                if (valueSampleMap == null)
                {
                    valueSampleMap = new TreeMap();
                    keywordValueSubsetListMap.put(param.getSearchKeyword(), valueSampleMap);
                }

                List<String> subsets = valueSampleMap.get(param.getSearchValue());
                if (subsets == null)
                {
                    subsets = new ArrayList<String>();
                    valueSampleMap.put(param.getSearchValue(), subsets);
                }

                // XXX: insert the subset into the proper position
                if (StringUtils.isNotEmpty(param.getPositiveGate()) && !subsets.contains(param.getPositiveGate()))
                {
                    subsets.add(param.getPositiveGate());
                }

                if (StringUtils.isNotEmpty(param.getNegativeGate()) && !subsets.contains(param.getNegativeGate()))
                {
                    subsets.add(param.getNegativeGate());
                }
            }
        }

        return keywordValueSubsetListMap;
    }

    public String javascriptArray(List<String> strings)
    {
        if (strings == null || strings.size() == 0)
            return "[]";
        return "['" + StringUtils.join(strings, "',\n'") + "']";
    }

    public String javascriptArray(String... strings)
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
        Map<String, List<String>> valueSubsetMap = this.keywordValueSampleMap.get(getKeywordName(sign, index));
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
        Map<String, List<String>> valueSubsetMap = this.keywordValueSampleMap.get(getKeywordName(sign, index));
        List<String> subsets = Collections.emptyList();
        if (valueSubsetMap == null)
        {
        }
        else
        {
            List<String> valueSubsets = valueSubsetMap.get(getKeywordValue(sign, index));
            if (valueSubsets != null)
            {
                subsets = valueSubsets;
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

    public String[] getGroupAnalysisNames()
    {
        if (form.workspace == null)
            return new String[0];
        List<String> ret = new ArrayList();
        for (Analysis analysis : form.workspace.getGroupAnalyses().values())
        {
            if (analysis.getPopulations().size() > 0)
            {
                ret.add(analysis.getName());
            }
        }
        return ret.toArray(new String[0]);
    }

    public Map<FieldKey, String> getFieldOptions()
    {
        Map<FieldKey, String> options = form.getFieldOptions();

        FieldKey keyKeyword = FieldKey.fromParts("Keyword");
        for (String keyword : keywordValueSampleMap.keySet())
        {
            addOption(options, keyKeyword, keyword);
        }

        for (AutoCompensationScript script : form.workspace.getAutoCompensationScripts())
        {
            AutoCompensationScript.MatchingCriteria criteria = script.getCriteria();
            if (criteria == null)
                continue;
            if (criteria.getPrimaryKeyword() != null)
                addOption(options, keyKeyword, criteria.getPrimaryKeyword());
            if (criteria.getSecondaryKeyword() != null)
                addOption(options, keyKeyword, criteria.getSecondaryKeyword());
        }
        return options;
    }
    
    private void addOption(Map<FieldKey, String> options, FieldKey keyKeyword, String keyword)
    {
        FieldKey key = new FieldKey(keyKeyword, keyword);
        if (!options.containsKey(key))
            options.put(key, keyword);
    }
}
