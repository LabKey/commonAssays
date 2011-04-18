/*
 * Copyright (c) 2005-2009 LabKey Corporation
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
    String _strPatternCanon;
    Pattern _patternCanon;

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
        _strPatternCanon = DataFrame.canonicalFieldName(pattern);
        _patternCanon = Pattern.compile(_strPatternCanon);
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
        String valueCanon = DataFrame.canonicalFieldName(value);
        return _pattern.matcher(value).matches() ||
            _pattern.matcher(valueCanon).matches() ||
            _patternCanon.matcher(valueCanon).matches();
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
