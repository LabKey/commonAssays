/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.microarray;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.dataiterator.DataIterator;
import org.labkey.api.dataiterator.DataIteratorBuilder;
import org.labkey.api.dataiterator.DataIteratorContext;
import org.labkey.api.dataiterator.SimpleTranslator;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.ContainerUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.microarray.controllers.FeatureAnnotationSetController;
import org.labkey.microarray.matrix.ExpressionMatrixProtocolSchema;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MicroarrayManager
{
    private static final MicroarrayManager _instance = new MicroarrayManager();
    private static final Logger LOG = Logger.getLogger(MicroarrayManager.class);

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

    public long featureAnnotationSetCount(Container c)
    {
        TableSelector selector = new TableSelector(getAnnotationSetSchemaTableInfo(), SimpleFilter.createContainerFilter(c), null);
        return selector.getRowCount();
    }

    public int deleteFeatureAnnotationSet(int... rowId)
    {
        DbScope scope = MicroarrayUserSchema.getSchema().getScope();

        Integer[] ids = ArrayUtils.toObject(rowId);

        try (DbScope.Transaction tx = scope.ensureTransaction())
        {
            // Delete all annotations first.
            TableInfo annotationSchemaTableInfo = getAnnotationSchemaTableInfo();
            SimpleFilter filter = new SimpleFilter();
            filter.addInClause(FieldKey.fromParts("FeatureAnnotationSetId"), Arrays.asList(ids));
            int rowsDeleted = Table.delete(annotationSchemaTableInfo, filter);

            // Then delete annotation set.
            TableInfo annotationSetSchemaTableInfo = getAnnotationSetSchemaTableInfo();
            filter = new SimpleFilter();
            filter.addInClause(FieldKey.fromParts("RowId"), Arrays.asList(ids));
            Table.delete(annotationSetSchemaTableInfo, filter);

            tx.commit();
            return rowsDeleted;
        }
    }

    private Integer insertFeatureAnnotationSet(User user, Container container, String name, String vendor, String description, String comment, BatchValidationException errors)
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
            row.put("Comment", comment);
            row.put("Container", container);

            List<Map<String, Object>> results = featureSetUpdateService.insertRows(user, container, Collections.singletonList(row), errors, null, null);
            return (Integer) results.get(0).get("RowId");
        }

        return null;
    }

    private Integer insertFeatureAnnotations(User user, Container container, Integer featureSetRowId, DataLoader loader, BatchValidationException errors) throws SQLException, BatchValidationException
    {
        QueryUpdateService queryUpdateService = getAnnotationQueryTableInfo(user, container).getUpdateService();

        if (queryUpdateService != null)
        {
            DataIteratorContext dataIteratorContext = new DataIteratorContext(errors);
            DataIterator dataIterator = loader.getDataIterator(dataIteratorContext);
            if (dataIterator == null)
            {
                throw dataIteratorContext.getErrors();
            }
            // TODO should create a custom DataIteratorBuider to wrap this custom iterator
            SimpleTranslator translator = new SimpleTranslator(dataIterator, dataIteratorContext);
            translator.selectAll();
            translator.addConstantColumn("featureannotationsetid", JdbcType.INTEGER, featureSetRowId);

            return queryUpdateService.importRows(user, container, new DataIteratorBuilder.Wrapper(translator), errors, null, null);
        }

        return -1;
    }

    /** Creates feature annotation set AND inserts all feature annotations from TSV */
    public Integer createFeatureAnnotationSet(User user, Container c, FeatureAnnotationSetController.FeatureAnnotationSetForm form, DataLoader loader, BatchValidationException errors)
            throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        Integer rowId = insertFeatureAnnotationSet(user, c, form.getName(), form.getVendor(), form.getDescription(), form.getComment(), errors);

        if (!errors.hasErrors() && rowId != null)
            return insertFeatureAnnotations(user, c, rowId, loader, errors);

        return -1;
    }

    /**
     * Get feature annotation set by name if it is in scope (current, project, and shared container).
     */
    @Nullable
    public Integer getFeatureAnnotationSet(Container c, User user, String featureSetName)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("Name"), featureSetName);

        // The container filter matches the assay's featureSet run property lookup
        ContainerFilter cf = new ContainerFilter.CurrentPlusProjectAndShared(user);
        filter.addClause(cf.createFilterClause(MicroarrayUserSchema.getSchema(), FieldKey.fromParts("container"), c));

        TableSelector featureAnnotationSelector = new TableSelector(getAnnotationSetSchemaTableInfo(), PageFlowUtil.set("RowId"), filter, null);
        List<Integer> rowIds = featureAnnotationSelector.getArrayList(Integer.class);
        // TODO: Order results by container depth
        if (rowIds.size() > 0)
            return rowIds.get(0);

        return null;
    }

    /**
     * Get feature annotation set by id if it is in scope (current, project, and shared container).
     */
    @Nullable
    public Integer getFeatureAnnotationSet(Container c, User user, int id)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("RowId"), id);

        // The container filter matches the assay's featureSet run property lookup
        ContainerFilter cf = new ContainerFilter.CurrentPlusProjectAndShared(user);
        filter.addClause(cf.createFilterClause(MicroarrayUserSchema.getSchema(), FieldKey.fromParts("container"), c));

        TableSelector featureAnnotationSelector = new TableSelector(getAnnotationSetSchemaTableInfo(), PageFlowUtil.set("RowId"), filter, null);
        List<Integer> rowIds = featureAnnotationSelector.getArrayList(Integer.class);
        // TODO: Order results by container depth
        if (rowIds.size() > 0)
            return rowIds.get(0);

        return null;
    }

    /**
     * Ensure the run property 'featureSet' actually exists and possibly import a new feature annotation set.
     *
     * If the featureSet value is an integer, we check that the feature annotation set is in scope (current, project, and shared containers).
     * If the 'featureSet' property is a name, we try to find the feature annotation set by name in scope (current, project, and shared containers).
     * If the 'featureSet' property is a string path, we try to find the feature annotation set by looking for a tsv file to import relative to current directory or relative to the pipeline root.
     *
     */
    public Integer ensureFeatureAnnotationSet(@Nullable Logger logger, @NotNull Container c, @NotNull User user, @Nullable File runDir, @NotNull String featureSet)
    {
        if (logger == null)
            logger = LOG;

        // First, try parsing the featureSet as an integer id
        try
        {
            int id = Integer.parseInt(featureSet);
            Integer resolvedId = MicroarrayManager.get().getFeatureAnnotationSet(c, user, id);
            if (resolvedId != null)
            {
                logger.info("Resolved featureSet by id: " + resolvedId);
                return resolvedId;
            }
        }
        catch (NumberFormatException ex)
        {
            // ok
        }

        // Next, try finding the feature annotation set by name
        Integer id = MicroarrayManager.get().getFeatureAnnotationSet(c, user, featureSet);
        if (id != null)
        {
            logger.info("Resolved featureSet by name: " + featureSet + " -> " + id);
            return id;
        }

        // Finally, try to load a feature annotation set file from either the runDir or the pipeline root.
        File featureSetFile = getPipelineFile(logger, c, runDir, featureSet);
        if (featureSetFile == null)
            return null;

        // Use the feature set if we find an existing one that matches the file's base name
        String baseName = FileUtil.getBaseName(featureSetFile.getName());
        Integer existingSet = getFeatureAnnotationSet(c, user, baseName);
        if (existingSet != null)
        {
            logger.info("Found existing feature annotation set by name: " + baseName);
            return existingSet;
        }

        try (DbScope.Transaction tx = MicroarrayUserSchema.getSchema().getScope().ensureTransaction())
        {
            BatchValidationException errors = new BatchValidationException();
            TabLoader loader = new TabLoader(featureSetFile, true);
            Map<String, String> comments = loader.getComments();
            String vendor = "unknown";
            String description = "";
            if (comments != null && !comments.isEmpty())
            {
                vendor = comments.get("vendor");
                description = comments.get("description");
            }

            // CONSIDER: Insert feature annotation set into same container as assay definition
            Integer newSetId = insertFeatureAnnotationSet(user, c, baseName, vendor, description, null, errors);
            if (!errors.hasErrors() && newSetId != null && newSetId > 0)
            {
                int rowsInserted = insertFeatureAnnotations(user, c, newSetId, loader, errors);
                if (rowsInserted <= 0)
                    throw new ExperimentException("Expression matrix file '" + featureSet + "' has no rows");

                tx.commit();
                logger.info("Created new feature annotation set '" + baseName + "' in current container");
                return newSetId;
            }

            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static File getPipelineFile(@NotNull Logger logger, @NotNull Container c, @Nullable File runDir, @NotNull String featureSet)
    {
        PipeRoot root = PipelineService.get().findPipelineRoot(c);
        if (root == null)
            return null;

        // First, look for a feature annotation set file relative to the runPath
        if (runDir != null)
        {
            String runPath = root.relativePath(runDir);
            if (runPath != null)
            {
                File file = root.resolvePath(runPath + File.separator + featureSet);
                if (file != null && file.canRead())
                {
                    logger.info("Resolved featureSet as file relative to runDir: " + root.relativePath(file));
                    return file;
                }
            }
        }

        // Next, look for a feature annotation set file relative to the pipeline root
        File file = root.resolvePath(featureSet);
        if (file != null && file.canRead())
        {
            logger.info("Resolved featureSet as file relative to pipeline root: " + root.relativePath(file));
            return file;
        }

        return null;
    }


    public Map<String, Integer> getFeatureAnnotationSetFeatureIds(int featureSetRowId)
    {
        SimpleFilter featureFilter = new SimpleFilter();
        featureFilter.addCondition(FieldKey.fromParts("FeatureAnnotationSetId"), featureSetRowId);

        TableSelector featureAnnotationSelector = new TableSelector(getAnnotationSchemaTableInfo(), PageFlowUtil.set("FeatureId", "RowId"), featureFilter, null);
        return featureAnnotationSelector.fillValueMap(new CaseInsensitiveHashMap<Integer>());
    }

    public void delete(Container container)
    {
        // Purge microarray.FeatureData table first
        TableInfo featureData = ExpressionMatrixProtocolSchema.getTableInfoFeatureData();
        SQLFragment deleteFrag = new SQLFragment();
        deleteFrag.append("DELETE FROM ").append(featureData).append(" WHERE featureid IN (");
        deleteFrag.append(" SELECT rowid FROM ").append(MicroarrayManager.getAnnotationSchemaTableInfo(), "a").append(" WHERE a.container = ?");
        deleteFrag.add(container.getEntityId());
        deleteFrag.append(")");
        new SqlExecutor(featureData.getSchema()).execute(deleteFrag);

        ContainerUtil.purgeTable(getAnnotationSchemaTableInfo(), container, "Container");
        ContainerUtil.purgeTable(getAnnotationSetSchemaTableInfo(), container, "Container");
    }

    // Issue 21134: filter by assay container and protocol
    public List<Map> getDistinctSamples(ExpProtocol protocol) throws SQLException
    {
        SQLFragment frag = new SQLFragment("SELECT SampleId, Name FROM ");
        frag.append("(SELECT DISTINCT SampleId FROM ");
        frag.append(ExpressionMatrixProtocolSchema.getTableInfoFeatureData(), "f");
        frag.append(", ");
        frag.append(ExperimentService.get().getTinfoData(), "d");
        frag.append(", ");
        frag.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        frag.append(" WHERE f.DataId = d.RowId\n");
        frag.append("   AND d.RunId = r.RowId\n");
        frag.append("   AND d.container=?\n").add(protocol.getContainer());
        frag.append("   AND r.ProtocolLSID = ?\n").add(protocol.getLSID());
        frag.append(") as fd, ");
        frag.append(ExperimentService.get().getTinfoMaterial(), "m");
        frag.append(" WHERE fd.SampleId = m.RowId");

        SqlSelector selector = new SqlSelector(MicroarrayUserSchema.getSchema(), frag);

        return selector.getArrayList(Map.class);
    }

}
