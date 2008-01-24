package org.labkey.elispot;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.elispot.plate.ElispotPlateReaderService;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 8, 2008
 */
public class ElispotDataHandler extends AbstractExperimentDataHandler
{
    public static final DataType ELISPOT_DATA_TYPE = new DataType("ElispotAssayData");

    public static final String ELISPOT_DATA_LSID_PREFIX = "ElispotAssayData";
    public static final String ELISPOT_DATA_ROW_LSID_PREFIX = "ElispotAssayDataRow";
    public static final String ELISPOT_PROPERTY_LSID_PREFIX = "ElispotProperty";
    public static final String ELISPOT_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        PlateTemplate template = provider.getPlateTemplate(container, protocol);

        for (ObjectProperty property : run.getObjectProperties().values())
        {
            if (ElispotAssayProvider.READER_PROPERTY_NAME.equals(property.getName()))
            {
                ListDefinition list = ElispotPlateReaderService.getPlateReaderList(info.getContainer());
                if (list != null)
                {
                    DomainProperty prop = list.getDomain().getPropertyByName(ElispotPlateReaderService.READER_TYPE_PROPERTY);
                    ListItem item = list.getListItem(property.getStringValue());
                    if (item != null && prop != null)
                    {
                        Object value = item.getProperty(prop);
                        ElispotPlateReaderService.I reader = ElispotPlateReaderService.getPlateReader(String.valueOf(value));
                        if (reader != null)
                        {
                            double[][] cellValues = reader.loadFile(template, dataFile);
                            insertPlateData(data, info, cellValues);
                            return;
                        }
                    }
                }
            }
        }
        throw new ExperimentException("Unable to load data file: Plate reader type not found");
    }

    private void insertPlateData(ExpData data, ViewBackgroundInfo info, double[][] cellValues) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        PlateTemplate template = provider.getPlateTemplate(container, protocol);

        Plate plate = PlateService.get().createPlate(template, cellValues);
        boolean ownTransaction = !ExperimentService.get().isTransactionActive();
        try {
            if (ownTransaction)
            {
                ExperimentService.get().beginTransaction();
            }

            try
            {
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                List<? extends WellGroup> antigens = plate.getWellGroups(WellGroup.Type.ANTIGEN);

                Map<String, ExpMaterial> materialMap = new HashMap<String, ExpMaterial>();
                for (ExpMaterial material : run.getMaterialInputs().keySet())
                    materialMap.put(material.getName(), material);

                for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                {
                    for (Position pos : group.getPositions())
                    {
                        results.clear();
                        Well well = plate.getWell(pos.getRow(), pos.getColumn());

                        // find the antigen group associated with this well
                        WellGroup antigen = getAntigenGroup(antigens, pos);

                        ExpMaterial material = materialMap.get(group.getName());
                        if (material != null)
                        {
                            Lsid dataRowLsid = new Lsid(data.getLSID());
                            dataRowLsid.setNamespacePrefix(ELISPOT_DATA_ROW_LSID_PREFIX);
                            dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + pos.getRow() + ':' + pos.getColumn());

                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID(), PropertyType.STRING));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "SFU", well.getValue(), PropertyType.DOUBLE, "0.0"));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "WellgroupName", group.getName(), PropertyType.STRING));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "WellLocation", pos.toString(), PropertyType.STRING));

                            OntologyManager.ensureObject(container.getId(), dataRowLsid.toString(),  data.getLSID());
                            OntologyManager.insertProperties(container.getId(), results.toArray(new ObjectProperty[results.size()]), dataRowLsid.toString());
                        }
                    }
                }
            }
            finally
            {
                if (ownTransaction)
                {
                    ExperimentService.get().rollbackTransaction();
                }
            }
        }
        catch (SQLException se)
        {
            throw new ExperimentException(se);
        }
    }

    private WellGroup getAntigenGroup(List<? extends WellGroup> groups, Position pos)
    {
        for (WellGroup group : groups)
        {
            if (group.contains(pos))
                return group;
        }
        return null;
    }

    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(ELISPOT_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container.getId(), propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    public URLHelper getContentURL(Container container, ExpData data) throws ExperimentException
    {
        return null;
    }

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        try {
            OntologyManager.deleteOntologyObject(container.getId(), data.getLSID());
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (ELISPOT_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
