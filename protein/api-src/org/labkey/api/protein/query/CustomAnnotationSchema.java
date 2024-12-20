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

package org.labkey.api.protein.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.protein.annotation.CustomAnnotationSet;
import org.labkey.api.protein.annotation.CustomAnnotationSetManager;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;

import java.util.Map;
import java.util.Set;

public class CustomAnnotationSchema extends UserSchema
{
    public static final String SCHEMA_WITHOUT_SEQUENCES_NAME = "CustomProteinAnnotations";
    public static final String SCHEMA_WITH_SEQUENCES_NAME = "CustomProteinAnnotationsWithSequences";
    public static final String SCHEMA_DESCR = "Contains data about custom protein annotations.";

    private final boolean _includeSequences;

    public static void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_WITHOUT_SEQUENCES_NAME, new DefaultSchema.SchemaProvider(module) {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), false);
            }
        });

        DefaultSchema.registerProvider(SCHEMA_WITH_SEQUENCES_NAME, new DefaultSchema.SchemaProvider(module) {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), true);
            }
        });
    }

    private final Map<String, CustomAnnotationSet> _annotationSets;

    public CustomAnnotationSchema(User user, Container container, boolean includeSequences)
    {
        super(includeSequences ? SCHEMA_WITH_SEQUENCES_NAME : SCHEMA_WITHOUT_SEQUENCES_NAME, SCHEMA_DESCR, user, container, ProteinSchema.getSchema());
        _includeSequences = includeSequences;
        _annotationSets = CustomAnnotationSetManager.getCustomAnnotationSets(container, true);
    }

    protected Map<String, CustomAnnotationSet> getAnnotationSets()
    {
        return _annotationSets;
    }

    @Override
    public Set<String> getTableNames()
    {
        return getAnnotationSets().keySet();
    }
    
    @Override
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        CustomAnnotationSet annotationSet = getAnnotationSets().get(name);
        if (annotationSet != null)
            return new CustomAnnotationTable(annotationSet, this, cf, _includeSequences);

        return null;
    }
}
