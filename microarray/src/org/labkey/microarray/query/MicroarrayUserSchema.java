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
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewContext;
import org.labkey.microarray.MicroarrayModule;
import org.labkey.microarray.view.FeatureAnnotationSetQueryView;
import org.springframework.validation.BindException;

import java.util.Set;

public class MicroarrayUserSchema extends SimpleUserSchema
{
    public static final String SCHEMA_NAME = "Microarray";
    public static final String SCHMEA_DESCR = "Contains data about Microarray assay runs";
    public static final String TABLE_FEATURE_ANNOTATION_SET = "FeatureAnnotationSet";
    public static final String TABLE_FEATURE_ANNOTATION = "FeatureAnnotation";

    public MicroarrayUserSchema(User user, Container container)
    {
        super(SCHEMA_NAME, SCHMEA_DESCR, user, container, getSchema());
    }

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module) {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MicroarrayUserSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    @Override
    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_FEATURE_ANNOTATION_SET);
        hs.add(TABLE_FEATURE_ANNOTATION);
        return hs;
    }

    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (getTableNames().contains(name))
        {
            SchemaTableInfo tableInfo = getSchema().getTable(name);
            if (name.equalsIgnoreCase(TABLE_FEATURE_ANNOTATION_SET))
            {
                return new FeatureAnnotationSetTable(this, tableInfo, cf).init();
            }
            if (name.equalsIgnoreCase(TABLE_FEATURE_ANNOTATION))
            {
                return new FeatureAnnotationSetTable(this, tableInfo, cf).init();
            }
            else
            {
                return new SimpleTable<>(this, tableInfo, cf).init();
            }
        }

        return null;
    }

    @Override
    @NotNull
    public QueryView createView(ViewContext context, @NotNull QuerySettings settings, BindException errors)
    {
        String queryName = settings.getQueryName();
        if (queryName != null)
        {
            if (queryName.equalsIgnoreCase(TABLE_FEATURE_ANNOTATION_SET))
            {
                return new FeatureAnnotationSetQueryView(this, settings, errors);
            }
        }

        QueryView ret = super.createView(context, settings, errors);
        if (TABLE_FEATURE_ANNOTATION.equalsIgnoreCase(queryName))
        {
            // NOTE FeatureAnnotationSetQueryView handles this for TABLE_FEATURE_ANNOTATION_SET
            ret.setAllowableContainerFilterTypes(ContainerFilter.Type.CurrentPlusProjectAndShared);
        }
        return ret;
    }

    public TableInfo getAnnotationSetTable()
    {
        return getTable(TABLE_FEATURE_ANNOTATION_SET, null);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(MicroarrayModule.DB_SCHEMA_NAME, DbSchemaType.Module);
    }
}
