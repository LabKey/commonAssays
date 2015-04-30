/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.ms2.matrix;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.matrix.AbstractMatrixDataHandler;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.query.MS2Schema;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProteinExpressionMatrixDataHandler extends AbstractMatrixDataHandler
{
    public static final String PROTEIN_SEQ_ID_COLUMN_NAME = "Molecule Identifier";
    private static final Logger LOG = Logger.getLogger(ProteinExpressionMatrixDataHandler.class);

    public ProteinExpressionMatrixDataHandler()
    {
        super(PROTEIN_SEQ_ID_COLUMN_NAME, MS2Schema.SCHEMA_NAME, ProteinExpressionMatrixProtocolSchema.PROTEIN_SEQ_DATA_TABLE_NAME);
    }

    @Override
    public DbSchema getDbSchema()
    {
        return MS2Manager.getSchema();
    }

    @Override
    public DataType getDataType()
    {
        return ProteinExpressionMatrixAssayProvider.DATA_TYPE;
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
            if (provider == null)
            {
                throw new ExperimentException("Could not find assay provider for protocol with LSID " + protocol.getLSID());
            }

            Domain runDomain = provider.getRunDomain(protocol);
            if (runDomain == null)
            {
                throw new ExperimentException("Could not find run domain for protocol with LSID " + protocol.getLSID());
            }

            Map<String, String> runProps = getRunPropertyValues(expRun, runDomain);

            DataLoader loader = createLoader(dataFile, PROTEIN_SEQ_ID_COLUMN_NAME);

            ColumnDescriptor[] cols = loader.getColumns();
            List<String> columnNames = new ArrayList<>(cols.length);
            for (ColumnDescriptor col : cols)
                columnNames.add(col.getColumnName());

            Map<String, Integer> samplesMap = ensureSamples(info.getContainer(), info.getUser(), columnNames, PROTEIN_SEQ_ID_COLUMN_NAME);

            insertMatrixData(info.getContainer(), info.getUser(), samplesMap, loader, runProps, data.getRowId());
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

    @Override
    public void insertMatrixData(Container c, User user,
                                 Map<String, Integer> samplesMap, DataLoader loader,
                                 Map<String, String> runProps, Integer dataRowId) throws ExperimentException
    {
        assert MS2Manager.getSchema().getScope().isTransactionActive() : "Should be invoked in the context of an existing transaction";
        PreparedStatement statement = null;

        try
        {
            Connection connection = MS2Manager.getSchema().getScope().getConnection();

            statement = connection.prepareStatement("INSERT INTO ms2." +
                    MS2Manager.getTableInfoExpressionData().getName() + " (DataId, SampleId, SeqId, Value) " +
                    "VALUES (?, ?, ?, ?)");
            int rowCount = 0;

            //Grab the protein name and rowId mapping for this run's annotation set
            String proteinSetString = runProps.get(ProteinExpressionMatrixAssayProvider.PROTEIN_SEQUENCE_SET_PROPERTY_NAME);

            if (proteinSetString == null)
            {
                throw new ExperimentException("Could not find " + ProteinExpressionMatrixAssayProvider.PROTEIN_SEQUENCE_SET_PROPERTY_NAME + " property value");
            }

            int proteinSet;
            try
            {
                proteinSet = Integer.parseInt(proteinSetString);
            }
            catch (NumberFormatException e)
            {
                throw new ExperimentException("Illegal " + ProteinExpressionMatrixAssayProvider.PROTEIN_SEQUENCE_SET_PROPERTY_NAME + " value:" + proteinSetString);
            }

            Map<String, Integer> seqIds = MS2Manager.getFastaFileSeqIds(proteinSet);
            Map<String, List> partialSeqIds = new HashMap<>(); //map of {"partial Ids", list of "full ids" }
            storeFastaSeqIdsToMatchExprMatrixSeqIdFormat(seqIds, partialSeqIds);

            for (Map<String, Object> row : loader)
            {
                Object seqIdObject = row.get(PROTEIN_SEQ_ID_COLUMN_NAME);
                String seqIdName = seqIdObject == null ? null : seqIdObject.toString();

                if (StringUtils.isEmpty(StringUtils.trimToNull(seqIdName)))
                {
                    throw new ExperimentException("Sequence ID (Molecular Identifier) must be present and cannot be blank");
                }

                List seqIdsList = partialSeqIds.get(seqIdName); //get list of "full ids"

                //if fasta file does not have the matching seq Id as the experiment expression file, then do not allow the import
                if (seqIdsList == null)
                {
                    throw new ExperimentException("Unable to find Protein '" + seqIdName + "' in the selected Fasta/Uniprot file.");
                }

                //if there are more than one "full ids" containing a partial id in fasta file
                if (seqIdsList.size() > 1)
                {
                    throw new ExperimentException("More than one protein with id '" + seqIdName + "' found in the selected Fasta/Uniprot file. Unable to choose the correct protein.");
                }

                String seqIdString = (String) seqIdsList.get(0);
                Integer seqId = seqIds.get(seqIdString); //get a matching seqId from stored sequences/content of fasta file

                //All the col names are condition names (ConditionA, ConditionB, etc.) except for the Molecular Identifier
                for (String sampleName : row.keySet())
                {
                    if (sampleName.equals(PROTEIN_SEQ_ID_COLUMN_NAME) || row.get(sampleName) == null)
                        continue;

                    statement.setInt(1, dataRowId);
                    statement.setInt(2, samplesMap.get(sampleName));
                    statement.setInt(3, seqId);
                    statement.setDouble(4, ((Number) row.get(sampleName)).doubleValue());
                    statement.executeUpdate();
                }

                if (++rowCount % 5000 == 0)
                {
                    LOG.info("Imported " + rowCount + " rows ...");
                }
            }
            LOG.info("Imported " + rowCount + " rows.");
        }
        catch(SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException ignored)
                {
                }
            }
        }
    }

    private void storeFastaSeqIdsToMatchExprMatrixSeqIdFormat(Map<String, Integer> seqIds, Map<String, List> substringsSeqId)
    {
        for(String seqIdFasta : seqIds.keySet())
        {
            String[] fastaSeqHeaderItems = seqIdFasta.split("\\|");
            String seqIdentiferMatchingExprMatrix = fastaSeqHeaderItems[2];
            List vals;

            if(substringsSeqId.containsKey(seqIdentiferMatchingExprMatrix))
                vals = substringsSeqId.get(seqIdentiferMatchingExprMatrix);
            else
                vals = new LinkedList();

            vals.add(seqIdFasta);
            substringsSeqId.put(seqIdentiferMatchingExprMatrix, vals);
        }
    }

}
