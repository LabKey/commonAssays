package org.labkey.flow.analysis.model;

import org.fhcrc.cpas.flow.script.xml.CriteriaDef;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class SampleCriteria
{
    String _keyword;
    String _strPattern;
    Pattern _pattern;

    static public SampleCriteria fromCriteriaDef(CriteriaDef criteria)
    {
        SampleCriteria ret = new SampleCriteria();
        ret.setKeyword(criteria.getKeyword());
        ret.setPattern(criteria.getPattern());
        return ret;
    }

    static public SampleCriteria readCriteria(Element el)
    {
        SampleCriteria ret = new SampleCriteria();
        ret.setKeyword(el.getAttribute("keyword"));
        ret.setPattern(el.getAttribute("pattern"));
        return ret;
    }

    static public SampleCriteria readChildCriteria(Element el)
    {
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i ++)
        {
            Node child = nl.item(i);
            if (child instanceof Element && "criteria".equals(child.getNodeName()))
                return readCriteria((Element) child);
        }
        return null;
    }

    public void setKeyword(String keyword)
    {
        _keyword = keyword;
    }

    public void setPattern(String pattern)
    {
        _strPattern = pattern;
        _pattern = Pattern.compile(pattern);
    }

    public String getKeyword()
    {
        return _keyword;
    }

    public String getPattern()
    {
        return _strPattern;
    }

    public boolean matches(FCSKeywordData fcs)
    {
        String value = fcs.getKeyword(_keyword);
        if (value == null)
            value = "";
        return _pattern.matcher(value).matches();
    }

    public FCSKeywordData find(List<FCSKeywordData> fcsRefs)
    {
        for (FCSKeywordData fcsRef : fcsRefs)
        {
            if (matches(fcsRef))
                return fcsRef;
        }
        return null;
    }

    public String toString()
    {
        return "Keyword '" + _keyword + "' matches '" + _strPattern + "'";
    }
}
