package org.labkey.ms2.protein.query;

import org.labkey.api.query.UserSchema;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
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
    private final boolean _includeSequences;

    public static void register()
    {
        DefaultSchema.registerProvider(SCHEMA_WITHOUT_SEQUENCES_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), false);
            }
        });

        DefaultSchema.registerProvider(SCHEMA_WITH_SEQUENCES_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new CustomAnnotationSchema(schema.getUser(), schema.getContainer(), true);
            }
        });
    }

    private Map<String, CustomAnnotationSet> _annotationSets;

    public CustomAnnotationSchema(User user, Container container, boolean includeSequences)
    {
        super(includeSequences ? SCHEMA_WITH_SEQUENCES_NAME : SCHEMA_WITHOUT_SEQUENCES_NAME, user, container, ProteinManager.getSchema());
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
    
    public TableInfo getTable(String name, String alias)
    {
        CustomAnnotationSet annotationSet = getAnnotationSets().get(name);
        if (annotationSet == null)
            return super.getTable(name, alias);

        return new CustomAnnotationTable(annotationSet, _includeSequences);
    }
}
