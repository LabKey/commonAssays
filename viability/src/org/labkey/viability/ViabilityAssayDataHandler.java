/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

package org.labkey.viability;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * User: kevink
 * Date: Sep 16, 2009
 */
public abstract class ViabilityAssayDataHandler extends AbstractAssayTsvDataHandler
{
    public static final String NAMESPACE = "ViabilityAssayData";
    // Used during 9.3 development, but deprecated for the more specific static DATA_TYPE in derived classes.
    protected static final DataType OLD_DATA_TYPE = new DataType(NAMESPACE);

    public static abstract class Parser
    {
        protected Domain _runDomain;
        protected Domain _resultsDomain;
        protected File _dataFile;

        protected Map<DomainProperty, Object> _runData;
        protected List<Map<String, Object>> _resultData;

        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            _runDomain = runDomain;
            _resultsDomain = resultsDomain;
            _dataFile = dataFile;
        }

        protected void parse() throws ExperimentException
        {
            try
            {
                _parse();
                postProcess();
            }
            catch (IOException e)
            {
                throw new ExperimentException(e);
            }
            finally
            {
                if (_runData == null)
                    _runData = Collections.emptyMap();
                if (_resultData == null)
                    _resultData = Collections.emptyList();
            }
        }

        protected abstract void _parse() throws IOException, ExperimentException;

        protected boolean shouldSplitPoolID()
        {
            return true;
        }

        // NOTE: Remove this postprocessing when 12017 is implemented.
        protected void postProcess() throws ExperimentException
        {
            if (_resultData == null)
                return;

            for (ListIterator<Map<String, Object>> it = _resultData.listIterator(); it.hasNext();)
            {
                Map<String, Object> row = it.next();

                String poolID = String.valueOf(row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
                if (poolID == null || poolID.length() == 0)
                    throw new ExperimentException(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME + " required");

                Object participantID = row.get(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
                Object visitID = row.get(AbstractAssayProvider.VISITID_PROPERTY_NAME);
                if (participantID == null && visitID == null)
                {
                    row = new HashMap<>(row);

                    // At a minimum, set the ParticipantID to the PoolID
                    String ptid = poolID;
                    row.put(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME, ptid);

                    // Try to split the PoolID into ParticipantID and VisitID
                    if (shouldSplitPoolID())
                    {
                        int sep = poolID.lastIndexOf('-');
                        if (sep == -1)
                            sep = poolID.lastIndexOf('V');
                        if (sep == -1)
                            sep = poolID.lastIndexOf('v');

                        String visit = null;
                        if (sep > 0)
                        {
                            ptid = poolID.substring(0, sep).trim();
                            int dot = ptid.lastIndexOf('.');
                            if (dot > 0)
                                ptid = ptid.substring(0, dot).trim();
                            visit = poolID.substring(sep+1).trim();
                        }

                        if (ptid != null && ptid.length() > 0)
                        {
                            row.put(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME, ptid);
                        }

                        if (visit != null && visit.length() > 0)
                        {
                            try
                            {
                                Double visitNum = Double.parseDouble(visit);
                                row.put(AbstractAssayProvider.VISITID_PROPERTY_NAME, visitNum);
                            }
                            catch (NumberFormatException nfe)
                            {
                                // ignore
                            }
                        }
                    }

                    it.set(row);
                }
            }

        }

        protected Object convert(DomainProperty dp, String value) throws ExperimentException
        {
            PropertyDescriptor pd = dp.getPropertyDescriptor();
            Class type = pd.getPropertyType().getJavaType();
            try
            {
                return ConvertUtils.convert(value, type);
            }
            catch (ConversionException ex)
            {
                throw new ExperimentException("Failed to convert property '" + dp.getName() + "' from '" + value + "' to a " + type.getSimpleName());
            }
        }

        public Map<DomainProperty, Object> getRunData() throws ExperimentException
        {
            if (_runData == null)
                parse();
            return _runData;
        }

        public List<Map<String, Object>> getResultData() throws ExperimentException
        {
            if (_resultData == null)
                parse();
            return _resultData;
        }

    }

    public abstract Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile);

    public static Parser createParser(File dataFile, ExpProtocol protocol) throws ExperimentException
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);
        Domain runDomain = provider.getRunDomain(protocol);
        Domain resultsDomain = provider.getResultsDomain(protocol);

        return createParser(dataFile, runDomain, resultsDomain);
    }

    public static Parser createParser(File dataFile, Domain runDomain, Domain resultsDomain) throws ExperimentException
    {
        ViabilityAssayDataHandler.Parser parser;
        String fileName = dataFile.getName().toLowerCase();
        if (fileName.endsWith(".tsv") || fileName.endsWith(".txt"))
            parser = new ViabilityTsvDataHandler.Parser(runDomain, resultsDomain, dataFile);
        else if (fileName.endsWith(".csv"))
            parser = new GuavaDataHandler.Parser(runDomain, resultsDomain, dataFile);
        else
            throw new ExperimentException("Don't know how to parse uploaded file: " + fileName);

        return parser;
    }

    @Override
    public DataType getDataType()
    {
        return OLD_DATA_TYPE;
    }

    protected boolean allowEmptyData()
    {
        return true;
    }

    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

    public ActionURL getContentURL(ExpData data)
    {
        return null;
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        // Don't bother calling super, since we aren't storing data in StorageProvisioner backed tables
        Container c = datas.get(0).getContainer();
        ViabilityManager.deleteAll(datas, c);
    }

    // check file data: all rows must have PoolID
    public static void validateData(List<Map<String, Object>> rows, boolean requireSpecimens) throws ExperimentException
    {
        if (rows == null || rows.size() == 0)
            throw new ExperimentException("No rows found.");

        for (ListIterator<Map<String, Object>> it = rows.listIterator(); it.hasNext();)
        {
            Map<String, Object> row = it.next();
            String poolID = String.valueOf(row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            if (poolID == null || poolID.length() == 0)
                throw new ExperimentException(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME + " required");

            if (requireSpecimens)
            {
                Object obj = row.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME);
                if (!(obj instanceof String[]))
                    throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + " required");
                String[] specimenIDs = (String[]) obj;
                if (specimenIDs.length == 0)
                    throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + " required");
                for (int i = 0; i < specimenIDs.length; i++)
                {
                    String specimenID = specimenIDs[i];
                    if (specimenID == null || specimenID.length() == 0)
                        throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + "[" + i + "] is empty or null.");

                    // XXX: check all specimens come from the same study.
                    // XXX: check all specimens match the given Participant
                }
            }
        }
    }

    @Override
    protected List<Map<String, Object>> convertPropertyNamesToURIs(List<Map<String, Object>> dataMaps, Domain domain)
    {
        // XXX: pass data thru untouched for now.
        return dataMaps;
    }

    @Override
    protected void insertRowData(ExpData data, User user, Container container, ExpRun run, ExpProtocol protocol, AssayProvider provider, Domain dataDomain, List<Map<String, Object>> fileData, TableInfo tableInfo) throws SQLException, ValidationException
    {
        // Find the target study property on the batch, run, or result domains.
        // If the target study is on the batch or run domain, get the value from the ExpRun or the ExpExperiment.
        // If the target study is on the result domain, pass the DomainProperty to splitBaseFromExtra()
        String batchOrRunTargetStudy = null;
        DomainProperty resultLevelTargetStudyProperty = null;
        Pair<ExpProtocol.AssayDomainTypes, DomainProperty> targetStudyPair = provider.findTargetStudyProperty(protocol);
        if (targetStudyPair != null)
        {
            DomainProperty targetStudyProperty = targetStudyPair.getValue();
            if (targetStudyPair.getKey() == ExpProtocol.AssayDomainTypes.Batch)
            {
                ExpExperiment experiment = AssayService.get().findBatch(run);
                if (experiment != null)
                    batchOrRunTargetStudy = (String)experiment.getProperty(targetStudyProperty);
            }
            else if (targetStudyPair.getKey() == ExpProtocol.AssayDomainTypes.Run)
            {
                batchOrRunTargetStudy = (String)run.getProperty(targetStudyProperty);
            }
            else if (targetStudyPair.getKey() == ExpProtocol.AssayDomainTypes.Result)
            {
                resultLevelTargetStudyProperty = targetStudyProperty;
            }
        }

        Map<String, PropertyDescriptor> importMap = new HashMap<>();
        for (DomainProperty prop : dataDomain.getProperties())
        {
            importMap.put(prop.getName(), prop.getPropertyDescriptor());
        }

        int rowIndex = 0;
        for (Map<String, Object> row : fileData)
        {
            Pair<Map<String, Object>, Map<PropertyDescriptor, Object>> pair = splitBaseFromExtra(row, importMap, resultLevelTargetStudyProperty);
            Map<String, Object> base = pair.first;
            Map<PropertyDescriptor, Object> extra = pair.second;

            // If available, copy the target study value from the run or batch.
            // If the property is on the result domain, it's already in the base map.
            if (batchOrRunTargetStudy != null)
            {
                base.put("targetStudy", batchOrRunTargetStudy);
            }

            ViabilityResult result = ViabilityResult.fromMap(base, extra);
            assert result.getRunID() == 0;
            assert result.getDataID() == 0;
            assert result.getObjectID() == 0;
            result.setRunID(run.getRowId());
            result.setDataID(data.getRowId());
            result.setContainer(container.getId());
            result.setProtocolID(protocol.getRowId());

            ViabilityManager.saveResult(user, container, result, rowIndex++);
        }

        ViabilityManager.updateSpecimenAggregates(user, container, provider, protocol, run);
    }

    private Pair<Map<String, Object>, Map<PropertyDescriptor, Object>> splitBaseFromExtra(Map<String, Object> row, Map<String, PropertyDescriptor> importMap, DomainProperty resultLevelTargetStudyProperty)
    {
        Map<String, Object> base = new CaseInsensitiveHashMap<>();
        Map<PropertyDescriptor, Object> extra = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet())
        {
            if (ViabilityAssayProvider.RESULT_DOMAIN_PROPERTIES.containsKey(entry.getKey()) ||
                (resultLevelTargetStudyProperty != null && resultLevelTargetStudyProperty.getName().equals(entry.getKey())))
            {
                base.put(entry.getKey(), entry.getValue());
            }
            else
            {
                PropertyDescriptor pd = importMap.get(entry.getKey());
                if (pd != null)
                    extra.put(pd, entry.getValue());
            }
        }

        return new Pair<>(base, extra);
    }



    public static class TestCase extends Assert
    {
        private File getViabilitySampleDirectory() throws IOException
        {
            File viabilityFiles = JunitUtil.getSampleData(null, "viability");
            assertTrue("Expected to find viability test files", null != viabilityFiles && viabilityFiles.exists());

            return viabilityFiles;
        }

        // NOTE: Running this test from within IntelliJ will fail due to ConvertHelper not registering
        // NOTE: its converters causing the ViabilityTsvDataHandler's TabLoader to infer types incorrectly.
        @Test
        public void testTsv() throws Exception
        {
            ViabilityTsvDataHandler.Parser parser = new ViabilityTsvDataHandler.Parser(null, null, new File(getViabilitySampleDirectory(), "simple.tsv"));

            List<Map<String, Object>> rows = parser.getResultData();
            assertEquals("Expected 3 rows", 3, rows.size());

            Map<String, Object> row = rows.get(0);
            assertEquals(8, row.size());
            assertEquals(1, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("1235-5", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("1235", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(null, row.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME));
            assertEquals(5.0, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertEquals(900, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(1000, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));

            row = rows.get(rows.size()-1);
            assertEquals(8, row.size());
            assertEquals(3, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("1234-7", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("1234", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(7.0, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertEquals(Arrays.asList("foobar", "vial1", "vial2"), row.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME));
            assertEquals(900, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(1100, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));
        }

        @Test
        public void testGuava() throws Exception
        {
            GuavaDataHandler.Parser parser = new GuavaDataHandler.Parser(null, null, new File(getViabilitySampleDirectory(), "small.VIA.csv"));

            List<Map<String, Object>> rows = parser.getResultData();
            assertEquals("Expected 7 rows", 7, rows.size());

            Map<String, Object> row = rows.get(0);
            assertEquals(7, row.size());
            assertEquals(1, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("160450533-5", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("160450533", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(5.0, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertTrue(Math.abs(0.845 - (Double)row.get("Viability")) < 0.0001);
            assertEquals(31268270.5, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(37003872.5, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));

            row = rows.get(rows.size()-1);
            assertEquals(7, row.size());
            assertEquals(34, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("159401872v5", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("159401872", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertEquals(5.0, row.get(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertTrue(Math.abs(0.954 - (Double)row.get("Viability")) < 0.0001);
            assertEquals(25878380.0, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(27126184.0, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));
        }

        @Test
        public void testExpressPlus() throws Exception
        {
            GuavaDataHandler.Parser parser = new GuavaDataHandler.Parser(null, null, new File(getViabilitySampleDirectory(), "122810.EP5.CSV"));

            List<Map<String, Object>> rows = parser.getResultData();
            assertEquals("Expected 16 rows", 16, rows.size());

            Map<String, Object> row = rows.get(0);
            assertEquals(7, row.size());
            assertEquals(1, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            assertEquals("B01", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("B01", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
            assertFalse(row.containsKey(ViabilityAssayProvider.VISITID_PROPERTY_NAME));
            assertTrue(Math.abs(0.4542 - (Double)row.get("Viability")) < 0.0001);
            assertEquals(9043848.8, row.get(ViabilityAssayProvider.VIABLE_CELLS_PROPERTY_NAME));
            assertEquals(1.9911600176133864E7, row.get(ViabilityAssayProvider.TOTAL_CELLS_PROPERTY_NAME));
            assertEquals(10.0, row.get("OriginalVolume"));

            // Last row
            row = rows.get(rows.size()-1);
            assertEquals(16, row.get(ViabilityAssayProvider.SAMPLE_NUM_PROPERTY_NAME));
            // NOTE: no splitting of PoolD into participant-visit
            assertEquals("C-04", row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME));
            assertEquals("C-04", row.get(ViabilityAssayProvider.PARTICIPANTID_PROPERTY_NAME));
        }
    }
}
