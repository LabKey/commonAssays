/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.api.exp.Lsid;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;

import java.util.Date;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public class CustomAnnotationSet
{
    private int _customAnnotationSetId;
    private String _container;
    private String _name;
    private int _createdBy;
    private Date _created;
    private int _modifiedBy;
    private Date _modified;
    private String _customAnnotationType;
    private String _lsid;
    public static final String TYPE = "CustomProteinAnnotationSet";

    public String getContainer()
    {
        return _container;
    }

    public void setContainer(String container)
    {
        _container = container;
    }

    public Date getCreated()
    {
        return _created;
    }

    public void setCreated(Date created)
    {
        _created = created;
    }

    public int getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(int createdBy)
    {
        _createdBy = createdBy;
    }

    public int getCustomAnnotationSetId()
    {
        return _customAnnotationSetId;
    }

    public void setCustomAnnotationSetId(int customAnnotationSetId)
    {
        _customAnnotationSetId = customAnnotationSetId;
    }
    
    public String getCustomAnnotationType()
    {
        return _customAnnotationType;
    }

    public void setCustomAnnotationType(String customAnnotationType)
    {
        _customAnnotationType = customAnnotationType;
    }

    public Date getModified()
    {
        return _modified;
    }

    public void setModified(Date modified)
    {
        _modified = modified;
    }

    public int getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(int modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getLsid()
    {
        return _lsid;
    }

    public void setLsid(String lsid)
    {
        _lsid = lsid;
    }

    public Container lookupContainer()
    {
        return ContainerManager.getForId(getContainer());
    }

    public CustomAnnotationType lookupCustomAnnotationType()
    {
        return CustomAnnotationType.valueOf(getCustomAnnotationType());
    }
}
