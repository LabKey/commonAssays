package org.labkey.flow.controllers.editscript;

import org.apache.struts.action.ActionMapping;
import org.apache.commons.lang.ObjectUtils;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.fhcrc.cpas.flow.script.xml.ParameterDef;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class EditSettingsForm extends EditScriptForm
{
    public String[] ff_parameter;
    public String[] ff_minValue;


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
        }
        ff_parameter = parameters.keySet().toArray(new String[0]);
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

}
