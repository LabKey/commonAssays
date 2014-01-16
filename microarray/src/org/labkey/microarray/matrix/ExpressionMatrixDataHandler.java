/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.query.FieldKey;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionMatrixDataHandler extends AbstractExperimentDataHandler
{
    private static final String PROBE_ID_COLUMN_NAME = "ID_REF";

    private static final Logger LOG = Logger.getLogger(ExpressionMatrixDataHandler.class);

    @Override
    public DataType getDataType()
    {
        return ExpressionMatrixAssayProvider.DATA_TYPE;
    }
    
    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if (!dataFile.exists())
        {
            log.warn("Could not find file " + dataFile.getAbsolutePath() + " on disk for data with LSID " + data.getLSID());
            return;
        }
        ExpRun expRun = data.getRun();
        if (expRun == null)
        {
            throw new ExperimentException("Could not load ExpressionMatrix file " + dataFile.getAbsolutePath() + " because it is not owned by an experiment run");
        }

        try
        {
            ExpProtocol protocol = expRun.getProtocol();
            AssayProvider provider = AssayService.get().getProvider(expRun);
            Domain runDomain = provider.getRunDomain(protocol);

            if (runDomain == null)
            {
                throw new ExperimentException("Could not find run domain for protocol with LSID " + protocol.getLSID());
            }

            Map<String, String> runProps = getRunPropertyValues(expRun, runDomain);
            TabLoader loader = new TabLoader(dataFile, true);
            List<Map<String, Object>> rowsMap = loader.load();
            Map<String, Integer> samplesMap = ensureSamples(info.getContainer(), rowsMap);
            insertExpressionMatrixData(samplesMap, rowsMap, runProps, data.getRowId());
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file " + dataFile.getName(), e);
        }
        catch (ExperimentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ExperimentException(e);
        }
    }

    private Map<String, Integer> ensureSamples(Container container, List<Map<String, Object>> loaderData) throws ExperimentException
    {
        Map<String, Integer> samplesMap = new HashMap<>();

        for (String name : loaderData.get(0).keySet())
        {
            if (!name.equals(PROBE_ID_COLUMN_NAME))
            {
                TableInfo sampleSetTableInfo = DbSchema.get(ExpSchema.SCHEMA_NAME).getTable("Material");
                SimpleFilter sampleSetFilter = new SimpleFilter();
                sampleSetFilter.addCondition(FieldKey.fromParts("Name"), name);
                sampleSetFilter.addCondition(FieldKey.fromParts("Container"), container);
                TableSelector sampleTableSelector = new TableSelector(sampleSetTableInfo, sampleSetFilter, null);
                Map<String, Object>[] sampleSetResults = sampleTableSelector.getMapArray();

                if (sampleSetResults == null || sampleSetResults.length < 1)
                {
                    throw new ExperimentException("No sample found with sample name=" + name);
                }

                samplesMap.put(name, (Integer) sampleSetResults[0].get("RowId"));
            }
        }

        return samplesMap;
    }

    private void insertExpressionMatrixData(Map<String, Integer> samplesMap, List<Map<String, Object>> loaderData,
                                            Map<String, String> runProps, Integer dataRowId) throws ExperimentException
    {
        assert MicroarrayUserSchema.getSchema().getScope().isTransactionActive() : "Should be invoked in the context of an existing transaction";
        PreparedStatement statement = null;
        try
        {
            Connection connection = MicroarrayUserSchema.getSchema().getScope().getConnection();
            statement = connection.prepareStatement("INSERT INTO microarray." +
                    ExpressionMatrixProtocolSchema.FEATURE_DATA_TABLE_NAME + " (DataId, SampleId, FeatureId, \"Value\") " +
                    "VALUES (?, ?, ?, ?)");
            int rowCount = 0;

            // Grab the probe name to rowId mapping for this run's annotation set
            Map<String, Integer> probeIds = getProbeIds(runProps);

            for (Map<String, Object> row : loaderData)
            {
                Object probeObject = row.get(PROBE_ID_COLUMN_NAME);
                String probeName = probeObject == null ? null : probeObject.toString();

                if (probeName == null || StringUtils.trimToNull(probeName) == null)
                {
                    throw new ExperimentException("Probe ID (ID_REF) must be present and cannot be blank");
                }

                Integer featureId = probeIds.get(probeName);
                if (featureId == null)
                {
                    throw new ExperimentException("Unable to find a feature/probe with name '" + probeName + "'");
                }

                // All of the column headers are sample names except for the probe id column.
                for (String sampleName : row.keySet())
                {
                    if (sampleName.equals(PROBE_ID_COLUMN_NAME) || row.get(sampleName) == null)
                        continue;

                    statement.setInt(1, dataRowId);
                    statement.setInt(2, samplesMap.get(sampleName));
                    statement.setInt(3, featureId);
                    statement.setDouble(4, ((Number) row.get(sampleName)).doubleValue());
                    statement.executeUpdate();
                }

                if (++rowCount % 5000 == 0)
                {
                    LOG.info("Imported " + rowCount + " of " + loaderData.size() + " rows");
                }
            }
            LOG.info("Imported " + rowCount + " of " + loaderData.size() + " rows");
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (statement != null) { try { statement.close(); } catch (SQLException ignored) {} }
        }
    }

    private Map<String, Integer> getProbeIds(Map<String, String> runProps)
    {
        SimpleFilter featureFilter = new SimpleFilter();
        featureFilter.addCondition(FieldKey.fromParts("FeatureAnnotationSetId"), Integer.parseInt(runProps.get("featureSet")));
        TableInfo tableInfo = MicroarrayUserSchema.getSchema().getTable(MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION);
        TableSelector featureAnnotationSelector = new TableSelector(tableInfo, PageFlowUtil.set("RowId", "ProbeId"), featureFilter, null);
        Map<String, Integer> result = new HashMap<>();
        for (Map<String, Object> probeRow : featureAnnotationSelector.getMapArray())
        {
            result.put((String) probeRow.get("ProbeId"), (Integer) probeRow.get("RowId"));
        }
        return result;
    }

    private Map<String, String> getRunPropertyValues(ExpRun run, Domain domain)
    {
        Map<String, String> runPropValues = new HashMap<>();
        for (DomainProperty runProp : domain.getProperties())
        {
            Object value = run.getProperty(runProp);
            if (value != null)
                runPropValues.put(runProp.getName(), value.toString());
        }
        return runPropValues;
    }

    @Override
    public ActionURL getContentURL(Container container, ExpData data)
    {
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        SqlExecutor executor = new SqlExecutor(MicroarrayUserSchema.getSchema());
        SQLFragment deleteDataSql = new SQLFragment("DELETE FROM " + MicroarrayUserSchema.SCHEMA_NAME);
        deleteDataSql.append("." + ExpressionMatrixProtocolSchema.FEATURE_DATA_TABLE_NAME);
        deleteDataSql.append(" WHERE DataId = ?").add(data.getRowId());
        executor.execute(deleteDataSql);
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }
}
