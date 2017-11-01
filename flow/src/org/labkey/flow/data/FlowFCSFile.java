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

package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class FlowFCSFile extends FlowWell
{
    public FlowFCSFile(ExpData data)
    {
        super(data);
    }

    static public List<FlowFCSFile> fromWellIds(int... ids)
    {
        List<FlowFCSFile> wells = new ArrayList<>(ids.length);
        List<FlowDataObject> flowobjs = fromRowIds(ids);
        for (FlowDataObject flowobj : flowobjs)
            if (flowobj instanceof FlowFCSFile)
                wells.add((FlowFCSFile)flowobj);
        return wells;
    }

    static public List<FlowFCSFile> fromName(Container container, String name)
    {
        return (List) FlowDataObject.fromName(container, FlowDataType.FCSFile, name);
    }

    static public List<FlowFCSFile> getOriginal(Container container)
    {
        List<FlowFCSFile> files = fromName(container, null);
        ListIterator<FlowFCSFile> iter = files.listIterator();
        while (iter.hasNext())
            if (!iter.next().isOriginalFCSFile())
                iter.remove();
        return files;
    }

    /**
     * Returns true if this FlowFCSFile well corresponds to an actual FCS file
     * instead of an FCS file created by the external analysis import process used to attach extra keywords.
     *
     * @return true if this well is original and was not created during external analysis import.
     * @see org.labkey.flow.data.FlowWell#getOriginalFCSFile()
     */
    public boolean isOriginalFCSFile()
    {
//        Boolean value = (Boolean)getProperty(FlowProperty.ExtraKeywordsFCSFile);
//        if (value != null)
//            return !value.booleanValue();

        // UNDONE: Ideally we should add a column to flow.object to idenfity these wells.
        // For now, use the fake data URI created by the external analysis import.
        ExpData expData = getData();
        String url = expData.getDataFileUrl();
        if (url != null && !url.endsWith("/attributes.flowdata.xml"))
            return true;

        return false;
    }

    public String getOriginalSourceFile()
    {
        return (String)getProperty(FlowProperty.OriginalSourceFile);
    }

}
