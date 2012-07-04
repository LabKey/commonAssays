/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.ms2.protein.query;

import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.CustomAnnotationSet;

import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Apr 3, 2007
 */
public class CustomAnnotationSchema extends UserSchema
{
    public static final String SCHEMA_WITHOUT_SEQUENCES_NAME = "CustomProteinAnnotations";
    public static final String SCHEMA_WITH_SEQUENCES_NAME = "CustomProteinAnnotationsWithSequences";
    public static final String SCHEMA_DESCR = "Contains data about custom protein annotations.";
    private final boolean _includeSequences;

    public static void register()
    {
        DefaultSchema.registerProvider(SCHEMA_WITHOUT_SEQUENCES_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                if (schema.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MS2Module.MS2_MODULE_NAME)))
                    return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), false);
                return null;
            }
        });

        DefaultSchema.registerProvider(SCHEMA_WITH_SEQUENCES_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                if (schema.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MS2Module.MS2_MODULE_NAME)))
                    return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), true);
                return null;
            }
        });
    }

    private Map<String, CustomAnnotationSet> _annotationSets;

    public CustomAnnotationSchema(User user, Container container, boolean includeSequences)
    {
        super(includeSequences ? SCHEMA_WITH_SEQUENCES_NAME : SCHEMA_WITHOUT_SEQUENCES_NAME, SCHEMA_DESCR, user, container, ProteinManager.getSchema());
        _includeSequences = includeSequences;
        _annotationSets = ProteinManager.getCustomAnnotationSets(container, true);
    }

    protected Map<String, CustomAnnotationSet> getAnnotationSets()
    {
        return _annotationSets;
    }

    public Set<String> getTableNames()
    {
        return getAnnotationSets().keySet();
    }
    
    public TableInfo createTable(String name)
    {
        CustomAnnotationSet annotationSet = getAnnotationSets().get(name);
        if (annotationSet == null)
            return super.getTable(name);

        return new CustomAnnotationTable(annotationSet, this, _includeSequences);
    }
}
