/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.data;

import org.labkey.api.view.ActionURL;
import org.labkey.api.exp.api.ExpData;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;

import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.log.LogController;

public class FlowLog extends FlowDataObject
{
    static public FlowLog fromLogId(int id) throws SQLException
    {
        FlowObject o = FlowDataObject.fromRowId(id);
        if (o instanceof FlowLog)
            return (FlowLog)o;
        return null;
    }

    public FlowLog(ExpData data)
    {
        super(data);
    }

    public Map<FlowParam,Object> getParams(ActionURL url)
    {
        EnumMap<FlowParam,Object> ret = new EnumMap(FlowParam.class);
        ret.put(FlowParam.logId, getRowId());
        return ret;
    }

    public ActionURL urlShow()
    {
        return urlFor(LogController.Action.showLog);
    }

    public String getText() throws SQLException
    {

        String ret = null; //(String) getProperty();
        if (ret == null)
        {
            return "";
        }
        return ret;
    }

    static public String cleanValue(Object value)
    {
        if (value == null)
            return "";
        return StringUtils.replaceChars(value.toString(), "\n\t", " ");
    }

    static public String[] parseHeaderLine(String line)
    {
        return StringUtils.splitPreserveAllTokens(line, '\t');
    }

    static public int parseHeaders(StringBuffer value, List<String> headers)
    {
        int eol = value.indexOf("\n");
        if (eol < 0)
            eol = value.length();
        headers.addAll(Arrays.asList(parseHeaderLine(value.substring(0, eol))));
        return eol;
    }

    static public void append(StringBuffer buf, Map<? extends Object, Object> valuesIn)
    {
        LinkedHashMap<String,String> values = new LinkedHashMap();
        for (Map.Entry<? extends Object,Object> entry : valuesIn.entrySet())
        {
            values.put(cleanValue(entry.getKey()), cleanValue(entry.getValue()));
        }
        if (buf.length() == 0)
        {
            buf.append(StringUtils.join(values.keySet().iterator(), "\t"));
            buf.append("\n");
            buf.append(StringUtils.join(values.values().iterator(), "\t"));
        }
        else
        {
            List<String> headers = new ArrayList();
            int eol = parseHeaders(buf, headers);
            List<String> lstValues = new ArrayList();
            for (String header: headers)
            {
                String value = values.remove(header);
                if (value == null)
                {
                    value = "";
                }
                lstValues.add(value);
            }
            if (values.size() != 0)
            {
                for (Map.Entry<String,String> entry : values.entrySet())
                {
                    headers.add(entry.getKey());
                    lstValues.add(entry.getValue());
                }
                buf.replace(0, eol, StringUtils.join(headers.iterator(), "\t"));
            }
            buf.append("\n");
            buf.append(StringUtils.join(lstValues.iterator(), "\t"));
        }
    }

    public void append(Map<Object,Object> valuesIn) throws SQLException
    {
        StringBuffer buf = new StringBuffer(getText());
        append(buf, valuesIn);
        setProperty(null, FlowProperty.LogText.getPropertyDescriptor(), buf.toString());
    }

    public String[] getHeadersAndEntries(List<String[]> rows) throws SQLException
    {
        String text = getText();
        if (text == null || text.length() == 0)
            return new String[0];
        int eol = text.indexOf("\n");
        if (eol < 0)
        {
            eol = text.length();
        }
        String[] headers = parseHeaderLine(text.substring(0, eol));
        String[] lines;

        if (eol < text.length())
        {
            lines = StringUtils.splitPreserveAllTokens(text.substring(eol + 1), '\n');
        }
        else
        {
            lines = new String[0];
        }
        for (String line : lines)
        {
            rows.add(StringUtils.splitPreserveAllTokens(line, '\t'));
        }
        return headers;
    }

    public void append(String text) throws Exception
    {
        String oldLog = (String) getProperty(FlowProperty.LogText);
        String newLog;
        if (oldLog == null)
        {
            newLog = text;
        }
        else
        {
            newLog = oldLog + "\n" + text;
        }

        setProperty(null, FlowProperty.LogText.getPropertyDescriptor(), newLog);
    }

    public void addParams(Map<FlowParam,Object> map)
    {
        map.put(FlowParam.logId, getRowId());
    }

    public LogType getLogType()
    {
        try
        {
            return LogType.valueOf(getName());
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
