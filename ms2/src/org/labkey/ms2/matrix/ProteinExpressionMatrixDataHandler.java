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
import org.labkey.api.reader.TabLoader;
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

            try (TabLoader loader = createTabLoader(dataFile, PROTEIN_SEQ_ID_COLUMN_NAME))
            {
                ColumnDescriptor[] cols = loader.getColumns();
                List<String> columnNames = new ArrayList<>(cols.length);
                for (ColumnDescriptor col : cols)
                    columnNames.add(col.getColumnName());

                Map<String, Integer> samplesMap = ensureSamples(info.getContainer(), info.getUser(), columnNames, PROTEIN_SEQ_ID_COLUMN_NAME);

//                boolean importValues = true;
//                if (runProps.containsKey(ProteinExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName()))
//                {
//                    String importValuesStr = runProps.get(ProteinExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName());
//                    if (importValuesStr != null)
//                        importValues = Boolean.valueOf(importValuesStr);
//                }
//
//                if (importValues)
                insertMatrixData(info.getContainer(), info.getUser(), samplesMap, loader, runProps, data.getRowId());
            }
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

            for (Map<String, Object> row : loader)
            {
                Object seqIdObject = row.get(PROTEIN_SEQ_ID_COLUMN_NAME);
                String seqIdName = seqIdObject == null ? null : seqIdObject.toString();

                if (StringUtils.isEmpty(StringUtils.trimToNull(seqIdName)))
                {
                    throw new ExperimentException("Sequence ID (Molecular Identifier) must be present and cannot be blank");
                }

                Integer seqId = seqIds.get(seqIdName); //get a matching seqId from the fasta file

                //if fasta file does not have the matching seq Id as the experiment expression file, then do not allow the import
                if (seqId == null)
                {
                    throw new ExperimentException("Unable to find Protein '" + seqIdName + "' in the selected Fasta/Uniprot file.");
                }

                //All the col names are condition names (ConditionA, ConditionB, etc.) except for the Molecular Identifier/Seq Id col.
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
        catch (SQLException e)
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

}
