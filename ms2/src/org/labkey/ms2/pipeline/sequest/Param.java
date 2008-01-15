package org.labkey.ms2.pipeline.sequest;

import java.util.ArrayList;
import java.util.List;

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
    private List<String> inputXmlLabels;
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
        this.inputXmlLabels = new ArrayList<String>();
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
        for (String anInputXmlLabel : inputXmlLabel)
        {
            this.inputXmlLabels.add(anInputXmlLabel);
        }
        return this;
    }

    public void setInputXmlLabels(List<String> inputXmlLabels)
    {
        this.inputXmlLabels = inputXmlLabels;
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

    public String convert()
    {
        return converter.convert(this);
    }

    public int compareTo(Param o)
    {
        if (o.getSortOrder() > this.getSortOrder()) return -1;
        if (o.getSortOrder() == this.getSortOrder()) return 0;
        return 1;
    }
}
