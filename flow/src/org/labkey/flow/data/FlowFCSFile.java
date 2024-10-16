/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryRowReference;
import org.labkey.api.security.User;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.query.FlowTableType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class FlowFCSFile extends FlowWell
{
    // Represents an FCS file selected by the user for import that has no corresponding FCS file imported in the database.
    // This can happen when there are no FCS files to be imported or when a directory of FCS files will be imported at the same time as the workspace.
    // Unfortuantely, since we use Jackson serialization to save/restore the job state, we can't check UNMAPPED for object equality
    // and instead rely on the <code>isUnresolved</code> method to check for a null ExpData.
    public static final FlowFCSFile UNMAPPED = new FlowFCSFile();
    static {
        UNMAPPED.setEntityId("UNMAPPED");
    }

    public final boolean isUnmapped()
    {
        return null == getData() && "UNMAPPED".equals(getEntityId());
    }

    // For serialization
    protected FlowFCSFile() {}

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

    public Date getFileDate()
    {
        return (Date)getProperty(FlowProperty.FileDate);
    }

    public void setFileDate(User user, Date date) throws Exception
    {
        setProperty(user, FlowProperty.FileDate.getPropertyDescriptor(), date);
    }

    @Override
    public QueryRowReference getQueryRowReference()
    {
        return new QueryRowReference(getContainer(), FlowSchema.SCHEMAKEY, FlowTableType.FCSFiles.name(), FieldKey.fromParts("RowId"), getRowId());
    }
}
