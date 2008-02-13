package org.labkey.flow.controllers.editscript;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMapping;
import org.fhcrc.cpas.flow.script.xml.FilterDef;
import org.fhcrc.cpas.flow.script.xml.FiltersDef;
import org.fhcrc.cpas.flow.script.xml.ParameterDef;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.labkey.api.data.CompareType;
import org.labkey.api.query.FieldKey;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class EditSettingsForm extends EditScriptForm
{
    public String[] ff_parameter;
    public String[] ff_minValue;

    public FieldKey[] ff_filter_field = new FieldKey[0];
    public String[] ff_filter_op = new String[0];
    public String[] ff_filter_value = new String[0];

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

            FiltersDef filtersDef = settings.getFilters();
            if (filtersDef != null)
            {
                int filterLen = filtersDef.sizeOfFilterArray();
                ff_filter_field = new FieldKey[filterLen];
                ff_filter_op = new String[filterLen];
                ff_filter_value = new String[filterLen];
                for (int i = 0; i < filterLen; i++)
                {
                    FilterDef crit = filtersDef.getFilterArray(i);
                    ff_filter_field[i] = FieldKey.fromString(crit.getField());
                    ff_filter_op[i] = crit.getOp().toString();
                    ff_filter_value[i] = crit.isSetValue() ? crit.getValue() : "";
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

    public void setFf_filter_field(String[] fields)
    {
        ff_filter_field = new FieldKey[fields.length];
        for (int i = 0; i < fields.length; i ++)
        {
            if (StringUtils.isEmpty(fields[i]))
                continue;
            ff_filter_field[i] = FieldKey.fromString(fields[i]);
        }
    }

    public void setFf_filter_op(String[] op)
    {
        this.ff_filter_op = op;
    }

    public void setFf_filter_value(String[] value)
    {
        this.ff_filter_value = value;
    }

    public Map<FieldKey, String> getFieldOptions()
    {
        Map<FieldKey, String> options = new LinkedHashMap<FieldKey, String>();
        options.put(null, "");
        options.put(FieldKey.fromParts("Name"), "FCS file name");
        options.put(FieldKey.fromParts("Run", "Name"), "Run name");

        FieldKey keyKeyword = FieldKey.fromParts("Keyword");
        for (String keyword : getAvailableKeywords())
        {
            options.put(new FieldKey(keyKeyword, keyword), keyword);
        }
        return options;
    }

    public Map<String, String> getOpOptions()
    {
        Map<String, String> options = new LinkedHashMap<String, String>();
        addCompare(options, CompareType.EQUAL);
        addCompare(options, CompareType.NEQ_OR_NULL);
        addCompare(options, CompareType.ISBLANK);
        addCompare(options, CompareType.NONBLANK);
        addCompare(options, CompareType.STARTS_WITH);
        addCompare(options, CompareType.CONTAINS);
        return options;
    }

    private void addCompare(Map<String, String> options, CompareType ct)
    {
        options.put(ct.getUrlKey(), ct.getDisplayValue());
    }
}
