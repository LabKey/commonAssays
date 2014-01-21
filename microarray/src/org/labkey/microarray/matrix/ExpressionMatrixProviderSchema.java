/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

package org.labkey.microarray.matrix;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayProviderSchema;

import java.util.HashSet;
import java.util.Set;

public class ExpressionMatrixProviderSchema { }
/*
public class ExpressionMatrixProviderSchema extends AssayProviderSchema
{
    //public static final String SCHEMA_NAME = "geomicroarray";

    public ExpressionMatrixProviderSchema(User user, Container container, AssayProvider provider, @Nullable Container targetStudy, boolean hidden)
    {
        super(user, container, provider, targetStudy);
        _hidden = hidden;
    }

    public Set<String> getTableNames()
    {
        Set<String> names = new HashSet<>();
        
        names.add(FEATURE_ANNOTATION_SET_TABLE_NAME);
        names.add(FEATURE_ANNOTATION_TABLE_NAME);

        return names;
    }

    public Set<String> getVisibleTableNames()
    {
        return getTableNames();
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public static TableInfo getTableInfoFeatureAnnotationSet()
    {
        return getSchema().getTable(FEATURE_ANNOTATION_SET_TABLE_NAME);
    }

    public static TableInfo getTableInfoFeatureAnnotation()
    {
        return getSchema().getTable(FEATURE_ANNOTATION_TABLE_NAME);
    }

    public TableInfo createTable(String name)
    {
        if (FEATURE_ANNOTATION_SET_TABLE_NAME.equalsIgnoreCase(name))
        {
            return new org.labkey.geomicroarray.query.FeatureAnnotationSetTable(this);
        }

        if (FEATURE_ANNOTATION_TABLE_NAME.equalsIgnoreCase(name))
        {
            return new GEOMicroarrayFeatureAnnotationTable(this);
        }

        return super.createTable(name);
    }
}
*/
