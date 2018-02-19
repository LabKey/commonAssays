/*
 * Copyright (c) 2009-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.Position;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: klum
 * Date: May 29, 2009
 */
public abstract class AbstractElispotDataHandler extends AbstractExperimentDataHandler
{
    public static final String ELISPOT_DATA_ROW_LSID_PREFIX = "ElispotAssayDataRow";
    public static final String ELISPOT_PROPERTY_LSID_PREFIX = "ElispotProperty";
    public static final String ELISPOT_ANTIGEN_PROPERTY_LSID_PREFIX = "ElispotAntigenProperty";
    public static final String ELISPOT_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String ELISPOT_ANTIGEN_ROW_LSID_PREFIX = "ElispotAssayAntigenRow";
    public static final String ELISPOT_ANTIGEN_LSID_PREFIX = "ElispotAssayAntigen";

    public static final String SFU_PROPERTY_NAME = "SpotCount";
    public static final String NORMALIZED_SFU_PROPERTY_NAME = "NormalizedSpotCount";
    public static final String WELLGROUP_PROPERTY_NAME = "WellgroupName";
    public static final String WELLGROUP_LOCATION_PROPERTY = "WellgroupLocation";
    public static final String WELL_ROW_PROPERTY = "WellRow";
    public static final String WELL_COLUMN_PROPERTY = "WellColumn";
    public static final String ANTIGEN_WELLGROUP_PROPERTY_NAME = "AntigenWellgroupName";
    public static final String BACKGROUND_WELL_PROPERTY = "Background";

    public static final String ANALYTE_PROPERTY_NAME = "Analyte";
    public static final String CYTOKINE_PROPERTY_NAME = "Cytokine";
    public static final String ACTIVITY_PROPERTY_NAME = "Activity";
    public static final String INTENSITY_PROPERTY_NAME = "Intensity";
    public static final String SPOT_SIZE_PROPERTY_NAME = "SpotSize";


    public interface ElispotDataFileParser
    {
        List<Map<String, Object>> getResults() throws ExperimentException;
    }

    public abstract ElispotDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException;

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());

        ElispotDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);
        importData(data, run, protocol, parser.getResults());
    }

    protected void importData(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> inputData) throws ExperimentException
    {
        try {
            List<? extends ExpData> runData = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
            assert(runData.size() == 1);

            for (Map<String, Object> row : inputData)
            {
                if (!row.containsKey(WELL_ROW_PROPERTY) || !row.containsKey(WELL_COLUMN_PROPERTY))
                    throw new ExperimentException("The row must contain values for well row and column locations : " + WELL_ROW_PROPERTY + ", " + WELL_COLUMN_PROPERTY);

                int rowPos = ConvertHelper.convert(row.get(WELL_ROW_PROPERTY), Integer.class);
                int colPos = ConvertHelper.convert(row.get(WELL_COLUMN_PROPERTY), Integer.class);
                String dataRowLsid = ElispotDataHandler.getDataRowLsid(runData.get(0).getLSID(), rowPos, colPos).toString();

                Map<String, Object> runDataFields = new HashMap<>();
                runDataFields.put("ObjectId", 0);
                runDataFields.put("ObjectUri", dataRowLsid);
                runDataFields.put("RunId", run.getRowId());
                runDataFields.put(ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, row.get(ELISPOT_INPUT_MATERIAL_DATA_PROPERTY));
                runDataFields.put(WELLGROUP_PROPERTY_NAME, row.get(WELLGROUP_PROPERTY_NAME));
                runDataFields.put(WELLGROUP_LOCATION_PROPERTY, row.get(WELLGROUP_LOCATION_PROPERTY));
                runDataFields.put(ANALYTE_PROPERTY_NAME, row.get(ANALYTE_PROPERTY_NAME));
                runDataFields.put(CYTOKINE_PROPERTY_NAME, row.get(CYTOKINE_PROPERTY_NAME));
                runDataFields.put(SFU_PROPERTY_NAME, row.get(SFU_PROPERTY_NAME));
                runDataFields.put(INTENSITY_PROPERTY_NAME, row.get(INTENSITY_PROPERTY_NAME));
                runDataFields.put(ACTIVITY_PROPERTY_NAME, row.get(ACTIVITY_PROPERTY_NAME));
                runDataFields.put(SPOT_SIZE_PROPERTY_NAME, row.get(SPOT_SIZE_PROPERTY_NAME));

                ElispotManager.get().insertRunDataRow(null, runDataFields);
            }
        }
        catch (Exception ve)
        {
            throw new ExperimentException(ve.getMessage(), ve);
        }
    }

    public static Lsid getDataRowLsid(String dataLsid, Position pos)
    {
        return getDataRowLsid(dataLsid, pos.getRow(), pos.getColumn());
    }

    public static Lsid getDataRowLsid(String dataLsid, int row, int col)
    {
        Lsid.LsidBuilder dataRowLsid = new Lsid.LsidBuilder(dataLsid);
        dataRowLsid.setNamespacePrefix(ELISPOT_DATA_ROW_LSID_PREFIX);
        dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + row + ':' + col);

        return dataRowLsid.build();
    }

    public static Lsid getAntigenRowLsid(String dataLsid, String sampleName)
    {
        assert (sampleName != null);

        Lsid.LsidBuilder dataRowLsid = new Lsid.LsidBuilder(dataLsid);
        dataRowLsid.setNamespacePrefix(ELISPOT_ANTIGEN_ROW_LSID_PREFIX);
        dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + sampleName);

        return dataRowLsid.build();
    }

    public static Lsid getAntigenLsid(String antigenWellgroupName, String sampleName, int runId, String assayName, String analyte)
    {
        assert (antigenWellgroupName != null);
        assert (sampleName != null);
        Lsid lsid = new Lsid.LsidBuilder(ELISPOT_ANTIGEN_LSID_PREFIX, assayName, "Run" + runId + "-" + antigenWellgroupName + "-" +
                             sampleName + "-" + (null != analyte ? analyte : "")).build();
        return lsid;
    }

    protected static ObjectProperty getObjectProperty(Container container, ExpProtocol protocol, String objectURI, String propertyName, Object value)
    {
        PropertyType type = PropertyType.STRING;
        String format = null;

        if (propertyName.equals(SFU_PROPERTY_NAME))
        {
            type = PropertyType.DOUBLE;
            format = "0.0";
        }
        else if (propertyName.equals(NORMALIZED_SFU_PROPERTY_NAME))
        {
            type = PropertyType.DOUBLE;
            format = "0.00";
        }
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, format);
    }

    public static ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    public static ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(ELISPOT_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    public static ObjectProperty getAntigenResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(ELISPOT_ANTIGEN_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    public ActionURL getContentURL(ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(data.getContainer(), p, run.getRowId());
        }
        return null;
    }

    @Override
    public void beforeDeleteData(List<ExpData> expDatas)
    {
        ElispotManager.get().deleteRunData(expDatas);
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        OntologyManager.deleteOntologyObject(data.getLSID(), container, true);
    }
}
