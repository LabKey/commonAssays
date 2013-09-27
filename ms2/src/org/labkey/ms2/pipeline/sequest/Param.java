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

package org.labkey.ms2.pipeline.sequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 9:35:09 AM
 */
public abstract class Param implements Comparable<Param>
{

    private int sortOrder;
    private String value;
    private String name;
    protected List<String> inputXmlLabels;
    private IInputXMLConverter converter;
    private IParamsValidator validator;


    Param(
        int sortOrder,
        String value,
        String name,
        List<String> inputXmlLabels,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        this.sortOrder = sortOrder;
        this.value = value;
        this.name = name;
        this.inputXmlLabels = inputXmlLabels;
        this.converter = converter;
        this.validator = validator;
    }

    Param(
        int sortOrder,
        String value,
        String name,
        IInputXMLConverter converter,
        IParamsValidator validator)
    {
        this.sortOrder = sortOrder;
        this.value = value;
        this.name = name;
        this.inputXmlLabels = new ArrayList<>();
        this.converter = converter;
        this.validator = validator;
    }

    public void setSortOrder(int sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder()
    {
        return sortOrder;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public Param setInputXmlLabels(String... inputXmlLabel)
    {
        inputXmlLabels.clear();
        inputXmlLabels.addAll(Arrays.asList(inputXmlLabel));
        return this;
    }

    public List<String> getInputXmlLabels()
    {
        return inputXmlLabels;
    }

    public void setValidator(IParamsValidator validator)
    {
        this.validator = validator;
    }

    public IParamsValidator getValidator()
    {
        return validator;
    }

    public String validate()
    {
        if (validator == null) return "";
        return validator.validate(this);
    }

    public void setConverter(IInputXMLConverter converter)
    {
        this.converter = converter;
    }

    public IInputXMLConverter getConverter()
    {
        return converter;
    }

    public String convert(String commentPrefix)
    {
        return converter.convert(this, commentPrefix);
    }

    public int compareTo(Param o)
    {
        if (o.getSortOrder() > this.getSortOrder()) return -1;
        if (o.getSortOrder() == this.getSortOrder()) return getName().compareTo(o.getName());
        return 1;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + " - " + getName() + ": " + getValue(); 
    }

    public String findValue(Map<String, String> params)
    {
        for (String label : inputXmlLabels)
        {
            if (params.containsKey(label))
            {
                return params.get(label);
            }
        }
        return null;
    }
}
