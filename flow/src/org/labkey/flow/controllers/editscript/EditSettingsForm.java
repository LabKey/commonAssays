package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang.ObjectUtils;
import org.apache.struts.action.ActionMapping;
import org.fhcrc.cpas.flow.script.xml.CriteriaDef;
import org.fhcrc.cpas.flow.script.xml.ParameterDef;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.fhcrc.cpas.flow.script.xml.FilterDef;
import org.labkey.common.util.Pair;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EditSettingsForm extends EditScriptForm
{
    public String[] ff_parameter;
    public String[] ff_minValue;

    public String[] ff_criteria_keyword = new String[0];
    public String[] ff_criteria_pattern = new String[0];

    public void reset(ActionMapping mapping, HttpServletRequest request)
    {
        super.reset(mapping, request);
        SettingsDef settings = analysisDocument.getScript().getSettings();
        Map<String, Double> parameters = new TreeMap();
        for (String param : getParameterNames(getRun(), new String[0]).keySet())
        {
            parameters.put(param, null);
        }
        if (settings != null)
        {
            for (ParameterDef param: settings.getParameterArray())
            {
                parameters.put(param.getName(), param.getMinValue());
            }

            FilterDef filterDef = settings.getFilter();
            if (filterDef != null)
            {
                int criteriaLen = filterDef.sizeOfCriteriaArray();
                ff_criteria_keyword = new String[criteriaLen];
                ff_criteria_pattern = new String[criteriaLen];
                for (int i = 0; i < criteriaLen; i++)
                {
                    CriteriaDef crit = filterDef.getCriteriaArray(i);
                    ff_criteria_keyword[i] = crit.getKeyword();
                    ff_criteria_pattern[i] = crit.getPattern();
                }
            }
        }

        ff_parameter = parameters.keySet().toArray(new String[parameters.size()]);
        ff_minValue = new String[ff_parameter.length];
        for (int i = 0; i < ff_parameter.length; i ++)
        {
            ff_minValue[i] = ObjectUtils.toString(parameters.get(ff_parameter[i]));
        }

    }

    public void setFf_parameter(String[] parameters)
    {
        ff_parameter = parameters;
    }

    public void setFf_minValue(String[] values)
    {
        ff_minValue = values;
    }

    public void setFf_criteria_keyword(String[] keyword)
    {
        this.ff_criteria_keyword = keyword;
    }

    public void setFf_criteria_pattern(String[] pattern)
    {
        this.ff_criteria_pattern = pattern;
    }
}
