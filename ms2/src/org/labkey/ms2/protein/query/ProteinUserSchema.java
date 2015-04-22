package org.labkey.ms2.protein.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.Sets;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.Module;
import org.labkey.api.protein.ProteomicsModule;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.ms2.MS2Module;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.query.SequencesTableInfo;

import java.util.Set;

/**
 * User: kevink
 * Date: 4/20/15
 */
public class ProteinUserSchema extends UserSchema
{
    public static final String NAME = "protein";

    ProteinUserSchema(User user, Container container)
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
        Sequences {
            @Override
            public TableInfo createTable(ProteinUserSchema schema, String name)
            {
                return schema.createSequences(name);
            }
        };

        public abstract TableInfo createTable(ProteinUserSchema schema, String name);
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
        return Sets.newCaseInsensitiveHashSet(
                TableType.Annotations.name(),
                TableType.GoGraphPath.name(),
                TableType.GoTerm.name(),
                TableType.GoTerm2Term.name(),
                TableType.GoTermDefinition.name(),
                TableType.GoTermSynonym.name(),
                TableType.Sequences.name()
        );
    }

    protected TableInfo createAnnotationsTable(String name)
    {
        SimpleUserSchema.SimpleTable<ProteinUserSchema> table = new SimpleUserSchema.SimpleTable<>(this, ProteinManager.getTableInfoAnnotations());
        table.init();
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

    protected SequencesTableInfo<ProteinUserSchema> createSequences(String name)
    {
        return new SequencesTableInfo<>(this);
    }

}
