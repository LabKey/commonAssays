/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.api.protein;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.dialect.SqlDialect;

import java.util.function.Function;

public class ProteinSchema
{
    private static final String SCHEMA_NAME = "prot";

    private static Function<SimpleFilter, Selector> _validForFastaDeleteSelectorProvider = filter -> new TableSelector(ProteinSchema.getTableInfoFastaFiles(), filter, null);
    private static String _invalidForFastaDeleteReason = "don't exist";

    public static String getSchemaName()
    {
        return SCHEMA_NAME;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoFastaFiles()
    {
        return getSchema().getTable("FastaFiles");
    }

    public static TableInfo getTableInfoFastaSequences()
    {
        return getSchema().getTable("FastaSequences");
    }

    public static TableInfo getTableInfoAnnotInsertions()
    {
        return getSchema().getTable("AnnotInsertions");
    }

    public static TableInfo getTableInfoCustomAnnotation()
    {
        return getSchema().getTable("CustomAnnotation");
    }

    public static TableInfo getTableInfoCustomAnnotationSet()
    {
        return getSchema().getTable("CustomAnnotationSet");
    }

    public static TableInfo getTableInfoAnnotations()
    {
        return getSchema().getTable("Annotations");
    }

    public static TableInfo getTableInfoAnnotationTypes()
    {
        return getSchema().getTable("AnnotationTypes");
    }

    public static TableInfo getTableInfoIdentifiers()
    {
        return getSchema().getTable("Identifiers");
    }

    public static TableInfo getTableInfoIdentTypes()
    {
        return getSchema().getTable("IdentTypes");
    }

    public static TableInfo getTableInfoOrganisms()
    {
        return getSchema().getTable("Organisms");
    }

    public static TableInfo getTableInfoInfoSources()
    {
        return getSchema().getTable("InfoSources");
    }

    public static TableInfo getTableInfoSequences()
    {
        return getSchema().getTable("Sequences");
    }

    public static TableInfo getTableInfoFastaLoads()
    {
        return getSchema().getTable("FastaLoads");
    }

    public static TableInfo getTableInfoSprotOrgMap()
    {
        return getSchema().getTable("SprotOrgMap");
    }

    public static TableInfo getTableInfoGoTerm()
    {
        return getSchema().getTable("GoTerm");
    }

    public static TableInfo getTableInfoGoTerm2Term()
    {
        return getSchema().getTable("GoTerm2Term");
    }

    public static TableInfo getTableInfoGoGraphPath()
    {
        return getSchema().getTable("GoGraphPath");
    }

    public static TableInfo getTableInfoGoTermDefinition()
    {
        return getSchema().getTable("GoTermDefinition");
    }

    public static TableInfo getTableInfoGoTermSynonym()
    {
        return getSchema().getTable("GoTermSynonym");
    }

    public static void registerValidForFastaDeleteSelectorProvider(Function<SimpleFilter, Selector> validForFastaDeleteSelectorProvider)
    {
        _validForFastaDeleteSelectorProvider = validForFastaDeleteSelectorProvider;
    }

    public static Function<SimpleFilter, Selector> getValidForFastaDeleteSelectorProvider()
    {
        return _validForFastaDeleteSelectorProvider;
    }

    public static void registerInvalidForFastaDeleteReason(String invalidForFastaDeleteReason)
    {
        _invalidForFastaDeleteReason = invalidForFastaDeleteReason;
    }

    public static String getInvalidForFastaDeleteReason()
    {
        return _invalidForFastaDeleteReason;
    }
}
