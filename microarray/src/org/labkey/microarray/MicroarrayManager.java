package org.labkey.microarray;

import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.etl.DataIterator;
import org.labkey.api.etl.DataIteratorBuilder;
import org.labkey.api.etl.DataIteratorContext;
import org.labkey.api.etl.SimpleTranslator;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.ContainerUtil;
import org.labkey.microarray.controllers.FeatureAnnotationSetController;
import org.labkey.microarray.matrix.ExpressionMatrixProtocolSchema;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MicroarrayManager
{
    private static final MicroarrayManager _instance = new MicroarrayManager();

    private MicroarrayManager()
    {
        // prevent external construction with a private default constructor
    }

    public static MicroarrayManager get()
    {
        return _instance;
    }


    private static TableInfo getAnnotationSetQueryTableInfo(User user, Container container)
    {
        MicroarrayUserSchema schema = new MicroarrayUserSchema(user, container);
        return schema.getTable(MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET);
    }

    private static TableInfo getAnnotationQueryTableInfo(User user, Container container)
    {
        MicroarrayUserSchema schema = new MicroarrayUserSchema(user, container);
        return schema.getTable(MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION, false);
    }

    private static TableInfo getAnnotationSetSchemaTableInfo()
    {
        DbSchema schema = MicroarrayUserSchema.getSchema();
        return schema.getTable(MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET);
    }

    private static TableInfo getAnnotationSchemaTableInfo()
    {
        DbSchema schema = MicroarrayUserSchema.getSchema();
        return schema.getTable(MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION);
    }

    public int deleteFeatureAnnotationSet(int... rowId)
    {
        DbScope scope = MicroarrayUserSchema.getSchema().getScope();

        try (DbScope.Transaction tx = scope.ensureTransaction())
        {
            // Delete all annotations first.
            TableInfo annotationSchemaTableInfo = getAnnotationSchemaTableInfo();
            SimpleFilter filter = new SimpleFilter();
            filter.addInClause(FieldKey.fromParts("FeatureAnnotationSetId"), Arrays.asList(rowId));
            int rowsDeleted = Table.delete(annotationSchemaTableInfo, filter);

            // Then delete annotation set.
            TableInfo annotationSetSchemaTableInfo = getAnnotationSetSchemaTableInfo();
            filter = new SimpleFilter();
            filter.addInClause(FieldKey.fromParts("RowId"), Arrays.asList(rowId));
            Table.delete(annotationSetSchemaTableInfo, filter);

            tx.commit();
            return rowsDeleted;
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private Integer insertFeatureAnnotationSet(User user, Container container, String name, String vendor, String description, BatchValidationException errors)
            throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        MicroarrayUserSchema schema = new MicroarrayUserSchema(user, container);
        QueryUpdateService featureSetUpdateService = schema.getAnnotationSetTable().getUpdateService();

        if (featureSetUpdateService != null)
        {
            Map<String, Object> row = new CaseInsensitiveHashMap<>();
            row.put("Name", name);
            row.put("Vendor", vendor);
            row.put("Description", description);
            row.put("Container", container);

            List<Map<String, Object>> results = featureSetUpdateService.insertRows(user, container, Collections.singletonList(row), errors, null);
            return (Integer) results.get(0).get("RowId");
        }

        return null;
    }

    private Integer insertFeatureAnnotations(User user, Container container, Integer featureSetRowId, DataLoader loader, BatchValidationException errors) throws SQLException
    {
        QueryUpdateService queryUpdateService = getAnnotationQueryTableInfo(user, container).getUpdateService();

        if (queryUpdateService != null)
        {
            DataIteratorContext dataIteratorContext = new DataIteratorContext(errors);
            DataIterator dataIterator = loader.getDataIterator(dataIteratorContext);
            // TODO should create a custom DataIteratorBuider to wrap this custom iterator
            SimpleTranslator translator = new SimpleTranslator(dataIterator, dataIteratorContext);

            for (int i = 1; i <= dataIterator.getColumnCount(); i++)
            {
                ColumnInfo colInfo = dataIterator.getColumnInfo(i);
                String alias = colInfo.getColumnName().replace("_", "");
                int aliasIndex = translator.addColumn(i);
                translator.addAliasColumn(alias, aliasIndex);
            }

            translator.addConstantColumn("featureannotationsetid", JdbcType.INTEGER, featureSetRowId);

            return queryUpdateService.importRows(user, container, new DataIteratorBuilder.Wrapper(translator), errors, null);
        }

        return -1;
    }

    public Integer createFeatureAnnotationSet(User user, Container c, FeatureAnnotationSetController.FeatureAnnotationSetForm form, DataLoader loader, BatchValidationException errors)
            throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        // Creates feature annotation set AND inserts all feature annotations from TSV
        Integer rowId = insertFeatureAnnotationSet(user, c, form.getName(), form.getVendor(), form.getDescription(), errors);

        if (!errors.hasErrors() && rowId != null)
            return insertFeatureAnnotations(user, c, rowId, loader, errors);

        return -1;
    }

    public void delete(Container container)
    {
        ContainerUtil.purgeTable(getAnnotationSchemaTableInfo(), container, "Container");
        ContainerUtil.purgeTable(getAnnotationSetSchemaTableInfo(), container, "Container");
    }

    public List<Map> getDistinctSamples(Container container) throws SQLException
    {
        SQLFragment frag = new SQLFragment("SELECT SampleId, Name FROM ");
        frag.append("(SELECT DISTINCT SampleId FROM ");
        frag.append(ExpressionMatrixProtocolSchema.getTableInfoFeatureData(), "f");
        frag.append(", ");
        frag.append(ExperimentService.get().getTinfoData(), "d");
        frag.append(" WHERE f.DataId = d.RowId AND d.container=?").add(container);
        frag.append(") as fd, ");
        frag.append(ExperimentService.get().getTinfoMaterial(), "m");
        frag.append(" WHERE fd.SampleId = m.RowId");

        SqlSelector selector = new SqlSelector(MicroarrayUserSchema.getSchema(), frag);

        return selector.getArrayList(Map.class);
    }

}
