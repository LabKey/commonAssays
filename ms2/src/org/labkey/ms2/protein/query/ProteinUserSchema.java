/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.Sets;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.OrganismTableInfo;
import org.labkey.ms2.query.SequencesTableInfo;

import java.util.Set;

/**
 * User: kevink
 * Date: 4/20/15
 */
public class ProteinUserSchema extends UserSchema
{
    public static final String NAME = "protein";
    public static final String ANNOTATION_TABLE_NAME = TableType.Annotations.name();
    public static final String FASTA_FILE_TABLE_NAME = TableType.FastaFiles.name();
    public static final String SEQUENCES_TABLE_NAME = TableType.Sequences.name();



    public ProteinUserSchema(User user, Container container)
    {
        super(NAME, "Protein annotation, gene ontology, and sequence tables", user, container, ProteinManager.getSchema());
    }

    public static void register(MS2Module module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            @Override
            public boolean isAvailable(DefaultSchema schema, Module module)
            {
                // Publish schema if any ProteomicsModule is active in the container
                for (Module m : schema.getContainer().getActiveModules(schema.getUser()))
                {
                    if (m instanceof ProteomicsModule)
                        return true;
                }
                return false;
            }

            @Nullable
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new ProteinUserSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public enum TableType
    {
        Annotations {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createAnnotationsTable(name);
            }
        },
        AnnotationTypes {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createAnnotationTypesTable(name);
            }
        },
        GoGraphPath {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createGoGraphPath(name);
            }
        },
        GoTerm {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createGoTerm(name);
            }
        },
        GoTerm2Term {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createGoTerm2Term(name);
            }
        },
        GoTermDefinition {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createGoTermDefinition(name);
            }
        },
        GoTermSynonym {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createGoTermSynonym(name);
            }
        },
        InfoSources {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createInfoSourcesTable(name);
            }
        },
        Organisms {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createOrganisms(name);
            }
        },
        Sequences {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createSequences(name);
            }
        },
        FastaSequences {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createFastaSequencesTable(name);
            }
        },
        FastaFiles {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createFastaFileTable(name);
            }
        };

        public abstract TableInfo createTable(ProteinUserSchema schema, String name);
    }

    private TableInfo createFastaSequencesTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoFastaSequences());
        table.init();
        table.getColumn("SeqId").setFk(new QueryForeignKey(this, null, TableType.Sequences.name(), null, null));
        table.getColumn("FastaId").setFk(new QueryForeignKey(this, null, TableType.FastaFiles.name(), null, null));
        return table;
    }

    @Nullable
    @Override
    protected TableInfo createTable(String name)
    {
        for (TableType tableType : TableType.values())
        {
            if (tableType.name().equalsIgnoreCase(name))
            {
                return tableType.createTable(this, tableType.name());
            }
        }

        return null;
    }

    @Override
    public Set<String> getTableNames()
    {
        Set<String> result = Sets.newCaseInsensitiveHashSet();
        for (TableType tableType : TableType.values())
        {
            result.add(tableType.name());
        }
        return result;
    }

    private TableInfo createAnnotationTypesTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoAnnotationTypes());
        table.init();
        table.getColumn("SourceId").setFk(new QueryForeignKey(this, null, TableType.InfoSources.name(), null, null));
        return table;
    }

    private TableInfo createInfoSourcesTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoInfoSources());
        table.init();
        return table;
    }

    protected TableInfo createAnnotationsTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoAnnotations());
        table.init();
        table.getColumn("AnnotTypeId").setFk(new QueryForeignKey(this, null, TableType.AnnotationTypes.name(), null, null));
        table.getColumn("AnnotSourceId").setFk(new QueryForeignKey(this, null, TableType.InfoSources.name(), null, null));
        return table;
    }

    protected TableInfo createGoGraphPath(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoGoGraphPath());
        table.init();
        return table;
    }

    protected TableInfo createGoTerm(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoGoTerm());
        table.init();
        return table;
    }

    protected TableInfo createGoTerm2Term(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoGoTerm2Term());
        table.init();
        table.getColumn("term1id").setFk(new QueryForeignKey(this, null, TableType.GoTerm.name(), null, null));
        table.getColumn("term2id").setFk(new QueryForeignKey(this, null, TableType.GoTerm.name(), null, null));
        return table;
    }

    protected TableInfo createGoTermDefinition(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoGoTermDefinition());
        table.init();
        return table;
    }

    protected TableInfo createGoTermSynonym(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoGoTermSynonym());
        table.init();
        return table;
    }

    protected TableInfo createOrganisms(String name)
    {
        return new OrganismTableInfo(this);
    }

    protected SequencesTableInfo<ProteinUserSchema> createSequences(String name)
    {
        return new SequencesTableInfo<>(this);
    }

    protected TableInfo createFastaFileTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoFastaFiles());
        table.init();
        ColumnInfo shortName = table.addWrapColumn("ShortName", table.getRealTable().getColumn("FileName"));
        shortName.setLabel("FASTA Name");

        shortName.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new DataColumn(colInfo)
                {
                    @Override
                    public Object getValue(RenderContext ctx)
                    {
                        Object result = super.getValue(ctx);
                        if (result != null)
                        {
                            String s = result.toString().replace('\\', '/');
                            int index = s.lastIndexOf('/');
                            if (index != -1)
                            {
                                return s.substring(index + 1);
                            }
                        }
                        return super.getValue(ctx);
                    }

                    @NotNull
                    @Override
                    public String getFormattedValue(RenderContext ctx)
                    {
                        return PageFlowUtil.filter(getDisplayValue(ctx));
                    }

                    @Override
                    public Object getDisplayValue(RenderContext ctx)
                    {
                        return getValue(ctx);
                    }
                };
            }
        });
        return table;
    }

}
