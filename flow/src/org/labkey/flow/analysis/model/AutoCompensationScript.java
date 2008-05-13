/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.flow.analysis.model;

import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: kevink
 * Date: Feb 13, 2008 4:05:41 PM
 */
public class AutoCompensationScript implements Serializable
{
    String _name;
    MatchingCriteria _criteria; // may be null
    ValidateCompensation _validate; // may be null
    Map<String, ParameterDefinition> _parameters = new LinkedHashMap();

    public static AutoCompensationScript readAutoComp(Element el)
    {
        AutoCompensationScript script = new AutoCompensationScript();
        script.setName(el.getAttribute("name"));

        script.setCriteria(MatchingCriteria.readMatching(
                FlowJoWorkspace.getElementByTagName(el, "MatchingCriteria")));

        script.setValidation(ValidateCompensation.readValidation(
                FlowJoWorkspace.getElementByTagName(el, "ValidateCompensation")));

        for (Element elParameterDef : FlowJoWorkspace.getElementsByTagName(el, "ParameterDefinition"))
        {
            script.addParameter(ParameterDefinition.readParameter(elParameterDef));
        }

        return script;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public MatchingCriteria getCriteria()
    {
        return _criteria;
    }

    public void setCriteria(MatchingCriteria criteria)
    {
        _criteria = criteria;
    }

    public ValidateCompensation getValidation()
    {
        return _validate;
    }

    public void setValidation(ValidateCompensation validate)
    {
        _validate = validate;
    }

    public Map<String, ParameterDefinition> getParameters()
    {
        return _parameters;
    }

    public void addParameter(ParameterDefinition parameter)
    {
        if (parameter != null)
            _parameters.put(parameter.getParameterName(), parameter);
    }

    public static class MatchingCriteria implements Serializable
    {
        String _primaryKeyword;
        String _secondaryKeyword;
        String _secondaryValue;

        public static MatchingCriteria readMatching(Element el)
        {
            if (el == null)
                return null;

            MatchingCriteria criteria = new MatchingCriteria();
            criteria.setPrimaryKeyword(el.getAttribute("primaryKeyword"));
            criteria.setSecondaryKeyword(el.getAttribute("secondaryKeyword"));
            criteria.setSecondaryValue(el.getAttribute("secondaryValue"));
            return criteria;
        }

        public String getPrimaryKeyword()
        {
            return _primaryKeyword;
        }

        public void setPrimaryKeyword(String primaryKeyword)
        {
            _primaryKeyword = primaryKeyword;
        }

        public String getSecondaryKeyword()
        {
            return _secondaryKeyword;
        }

        public void setSecondaryKeyword(String secondaryKeyword)
        {
            _secondaryKeyword = secondaryKeyword;
        }

        public String getSecondaryValue()
        {
            return _secondaryValue;
        }

        public void setSecondaryValue(String secondaryValue)
        {
            _secondaryValue = secondaryValue;
        }
    }

    public static class ValidateCompensation implements Serializable
    {
        String _minimumEventCount;
        boolean _haltScript;

        public static ValidateCompensation readValidation(Element el)
        {
            if (el == null)
                return null;

            ValidateCompensation validate = new ValidateCompensation();
            validate.setMinimumEventCount(el.getAttribute("minimumEventCount"));
            validate.setHaltScript("1".equals(el.getAttribute("haltScript")));
            return validate;
        }

        public String getMinimumEventCount()
        {
            return _minimumEventCount;
        }

        public void setMinimumEventCount(String minimumEventCount)
        {
            _minimumEventCount = minimumEventCount;
        }

        public boolean isHaltScript()
        {
            return _haltScript;
        }

        public void setHaltScript(boolean haltScript)
        {
            _haltScript = haltScript;
        }

    }

    public static class ParameterDefinition implements Serializable
    {
        String _parameterName;
        String _searchKeyword;
        String _searchValue;
        String _positiveGate;
        String _negativeGate;

        public static ParameterDefinition readParameter(Element el)
        {
            if (el == null)
                return null;

            ParameterDefinition parameter = new ParameterDefinition();
            parameter.setParameterName(el.getAttribute("parameterName"));
            parameter.setSearchKeyword(el.getAttribute("searchKeyword"));
            parameter.setSearchValue(el.getAttribute("searchValue"));
            parameter.setPositiveGate(el.getAttribute("positiveGate"));
            parameter.setNegativeGate(el.getAttribute("negativeGate"));
            return parameter;
        }

        public String getParameterName()
        {
            return _parameterName;
        }

        public void setParameterName(String parameterName)
        {
            _parameterName = parameterName;
        }

        public String getSearchKeyword()
        {
            return _searchKeyword;
        }

        public void setSearchKeyword(String searchKeyword)
        {
            _searchKeyword = searchKeyword;
        }

        public String getSearchValue()
        {
            return _searchValue;
        }

        public void setSearchValue(String searchValue)
        {
            _searchValue = searchValue;
        }

        public String getPositiveGate()
        {
            return _positiveGate;
        }

        public void setPositiveGate(String positiveGate)
        {
            _positiveGate = positiveGate;
        }

        public String getNegativeGate()
        {
            return _negativeGate;
        }

        public void setNegativeGate(String negativeGate)
        {
            _negativeGate = negativeGate;
        }

    }
}
