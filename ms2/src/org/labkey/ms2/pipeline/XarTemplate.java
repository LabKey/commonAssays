/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.SimpleInputColumn;
import org.labkey.api.data.TextAreaInputColumn;
import org.labkey.api.util.CsvSet;
import org.labkey.api.util.PageFlowUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: migra
 * Date: Dec 5, 2005
 * Time: 3:53:03 PM
 */
public class XarTemplate
{
    private String xarText = null;
    private String name = null;

    public XarTemplate(File xarFile)
    {
        xarText = PageFlowUtil.getFileContentsAsString(xarFile);
    }

    public XarTemplate(String name, String xarText)
    {
        this.xarText = xarText;
        this.name = name;
    }

    private static Set<String> defaultTokens = new CsvSet("RUN_NAME,RUN_FILENAME,PROTOCOL_NAME");
    public List<DisplayColumn> getSubstitutionFields()
    {
        List<DisplayColumn> colList = new ArrayList<DisplayColumn>();
        Set<String> tokens = getTokens(xarText);

        for (String s : tokens)
        {
            if (!defaultTokens.contains(s))
            {
                String propName = s;
                if (MassSpecProtocolFactory.isUpperCaseTokenName(s))
                    propName = MassSpecProtocolFactory.upperCaseTokenNameToPropName(s);

                String title = ColumnInfo.captionFromName(propName);
                DisplayColumn col;
                if (title.contains("Description"))
                    col = new TextAreaInputColumn(propName, null);
                else
                    col = SimpleInputColumn.create(propName, null);
                col.setCaption(title);
                colList.add(col);
            }
        }

        return colList;
    }

    private static Pattern tokenPat = Pattern.compile("@@\\w*@@");
    public static Set<String> getTokens(String str)
    {
        Set<String> tokens = new LinkedHashSet<String>();

        Matcher m = tokenPat.matcher(str);

        while(m.find())
        {
            String tok = str.substring(m.start() + 2, m.end() - 2);
            tokens.add(tok);
        }

        return tokens;
    }

    public static Set<String> getTokens(File file)
    {
        return getTokens(PageFlowUtil.getFileContentsAsString(file));
    }

    public Set<String> getTokens()
    {
        return getTokens(xarText);
    }


    public String getName()
    {
        return name;
    }
}
