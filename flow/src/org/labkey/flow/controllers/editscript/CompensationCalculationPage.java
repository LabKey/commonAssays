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

package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang3.StringUtils;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.api.query.FieldKey;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.AutoCompensationScript;
import org.labkey.flow.analysis.model.Population;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.model.SubsetPart;
import org.labkey.flow.analysis.model.Workspace;
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
    public Map<String, Map<String, List<String>>> getKeywordValueSampleMap(Workspace workspace)
    {
        Set<String> keywordsSet = new TreeSet();
        Map<Workspace.SampleInfo, List<String>> sampleSubsetMap = new HashMap();
        for (Workspace.SampleInfo sample : workspace.getSamplesComplete()) {
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
            Map<String, List<Workspace.SampleInfo>> sampleMap = new LinkedHashMap();
            for (Workspace.SampleInfo sample : sampleSubsetMap.keySet())
            {
                String value = sample.getKeywords().get(keyword);
                if (value != null)
                {
                    List<Workspace.SampleInfo> list = sampleMap.get(value);
                    if (list == null)
                    {
                        list = new ArrayList();
                        sampleMap.put(value, list);
                    }
                    list.add(sample);
                }
            }
            Map<String, List<String>> valueSampleMap = new TreeMap();
            for (Map.Entry<String, List<Workspace.SampleInfo>> entry : sampleMap.entrySet())
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
                    subsets = new ArrayList<>();
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
        SubsetPart[] userSubsets = subsetUser.getSubsets();
        SubsetPart[] workspaceSubsets = subsetWorkspace.getSubsets();
        if (userSubsets.length != workspaceSubsets.length)
            return false;
        String strChannel = this.form.parameters[index];
        for (int i = 0; i < userSubsets.length; i ++)
        {
            if (!subsetNameMatches(userSubsets[i].toString(), workspaceSubsets[i].toString(), sign, strChannel))
                return false;
        }
        return true;
    }

    public String selectSubsets(Sign sign, int index)
    {
        StringBuilder ret = new StringBuilder();
        String keywordName = getKeywordName(sign, index);
        Map<String, List<String>> valueSubsetMap = keywordName == null ? null : keywordValueSampleMap.get(keywordName);
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

        SubsetSpec current = SubsetSpec.fromEscapedString(getSubset(sign, index));

        ret.append("<select name=\"" + sign + "Subset[" + index + "]\">");
        ret.append(option("", "Ungated", ""));
        for (String subset : subsets)
        {
            SubsetSpec workspaceSubset = SubsetSpec.fromEscapedString(subset);
            boolean selected = subsetMatches(current, workspaceSubset, sign, index);
            ret.append("\n<option value=\"" + h(workspaceSubset.toString()) + "\"");
            if (selected)
            {
                ret.append(" selected");
            }
            ret.append(">" + h(subset) + "</option>");
        }
        ret.append("</select>");
        return ret.toString();
    }

    public String[] getGroupAnalysisDisplayNames()
    {
        if (form.workspace == null)
            return new String[0];
        List<String> ret = new ArrayList();
        for (Analysis analysis : form.workspace.getGroupAnalyses().values())
        {
            if (analysis.getPopulations().size() > 0)
            {
                PopulationName name = analysis.getName();
                if (name != null)
                    ret.add(name.getRawName());
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
