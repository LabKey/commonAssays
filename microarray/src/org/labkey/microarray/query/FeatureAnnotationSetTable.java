/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

package org.labkey.microarray.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.util.GUID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// class for BOTH FeatureAnnotationSet and FeatureAnnotation

public class FeatureAnnotationSetTable extends SimpleUserSchema.SimpleTable<MicroarrayUserSchema>
{
    @Override
    protected ContainerFilter getDefaultContainerFilter()
    {
        return ContainerFilter.Type.CurrentPlusProjectAndShared.create(getUserSchema());
    }

    FeatureAnnotationSetTable(MicroarrayUserSchema s, TableInfo t, ContainerFilter cf)
    {
        super(s, t, cf);
    }

    @Override
    protected void _setContainerFilter(@NotNull ContainerFilter filter)
    {
        super._setContainerFilter(filter);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        super.applyContainerFilter(new ProjectSharedContainerFilterWrapper(filter));
    }

    class ProjectSharedContainerFilterWrapper extends ContainerFilter
    {
        final ContainerFilter _inner;

        @Override
        public String getCacheKey()
        {
            return getDefaultCacheKey(_container, _user) + _inner.getCacheKey();
        }

        @Override
        public @Nullable Collection<GUID> getIds()
        {
            Set<GUID> ret = new HashSet<>();
            Collection<GUID> ids = _inner.getIds();
            if (null != ids)
                ret.addAll(ids);
            ret.add(ContainerManager.getSharedContainer().getEntityId());
            Container project = _container.getProject();
            if (null != project)
                ret.add(project.getEntityId());
            return ret;
        }

        @Override
        public @Nullable Type getType()
        {
            return _inner.getType();
        }

        ProjectSharedContainerFilterWrapper(ContainerFilter cf)
        {
            super(null, null);
            _inner = cf;
        }
    }
}