package org.labkey.luminex;

import org.labkey.api.study.assay.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.*;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.data.*;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;

import javax.servlet.ServletException;
import java.util.*;
import java.sql.SQLException;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends AbstractAssayProvider
{
    public static final String ASSAY_DOMAIN_ANALYTE = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Analyte";
    public static final String ASSAY_DOMAIN_EXCEL_RUN = ExpProtocol.ASSAY_DOMAIN_PREFIX + "ExcelRun";

    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", LuminexExcelDataHandler.LUMINEX_DATA_LSID_PREFIX);
    }

    public String getName()
    {
        return "Luminex";
    }

    protected void registerLsidHandler()
    {
        super.registerLsidHandler();
        LsidManager.get().registerHandler("LuminexDataRow", new LsidManager.LsidHandler()
        {
            public ExpData getObject(Lsid lsid)
            {
                return getDataForDataRow(lsid.getObjectId());
            }

            public String getDisplayURL(Lsid lsid)
            {
                ExpData data = getDataForDataRow(lsid.getObjectId());
                if (data == null)
                    return null;
                ExpRun expRun = data.getRun();
                if (expRun == null)
                    return null;
                ExpProtocol protocol = expRun.getProtocol();
                if (protocol == null)
                    return null;
                ViewURLHelper dataURL = getAssayDataURL(expRun.getContainer(), protocol, expRun.getRowId());
                return dataURL.getLocalURIString();
            }
        });
    }

    public PropertyDescriptor[] getRunPropertyColumns(ExpProtocol protocol)
    {
        List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>(Arrays.asList(super.getRunPropertyColumns(protocol)));
        result.addAll(Arrays.asList(getPropertiesForDomainPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN)));

        return result.toArray(new PropertyDescriptor[result.size()]);
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, ExpProtocol protocol)
    {
        return new LuminexSchema(schema.getUser(), schema.getContainer(), protocol).createDataRowTable(alias);
    }

    public PropertyDescriptor[] getRunDataColumns(ExpProtocol protocol)
    {
        throw new UnsupportedOperationException();
    }


    protected Domain createUploadSetDomain(Container c, User user)
    {
        Domain uploadSetDomain = super.createUploadSetDomain(c, user);
        Container lookupContainer = c.getProject();
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);

        ListDefinition speciesList = lists.get("LuminexSpecies");
        if (speciesList == null)
        {
            speciesList = ListService.get().createList(lookupContainer, "LuminexSpecies");
            DomainProperty nameProperty = addProperty(speciesList.getDomain(), "Name", PropertyType.STRING);
            nameProperty.setPropertyURI(speciesList.getDomain().getTypeURI() + "#Name");
            speciesList.setKeyName("SpeciesID");
            speciesList.setTitleColumn(nameProperty.getName());
            speciesList.setKeyType(ListDefinition.KeyType.Varchar);
            speciesList.setDescription("Species for Luminex assays");
            try
            {
                speciesList.save(user);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        DomainProperty speciesProperty = addProperty(uploadSetDomain, "Species", PropertyType.STRING);
//        speciesProperty.setRequired(true);
        speciesProperty.setLookup(new Lookup(lookupContainer, "lists", speciesList.getName()));

        ListDefinition labList = lists.get("LuminexLabs");
        if (labList == null)
        {
            labList = ListService.get().createList(lookupContainer, "LuminexLabs");
            DomainProperty nameProperty = addProperty(labList.getDomain(), "Name", PropertyType.STRING);
            nameProperty.setPropertyURI(labList.getDomain().getTypeURI() + "#Name");
            labList.setKeyName("LabID");
            labList.setTitleColumn(nameProperty.getName());
            labList.setKeyType(ListDefinition.KeyType.Varchar);
            labList.setDescription("Labs performing Luminex assays");
            try
            {
                labList.save(user);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        DomainProperty labProperty = addProperty(uploadSetDomain, "Lab ID", PropertyType.STRING);
//        labProperty.setRequired(true);
        labProperty.setLookup(new Lookup(lookupContainer, "lists", labList.getName()));

        addProperty(uploadSetDomain, "Analysis Software", PropertyType.STRING);

        return uploadSetDomain;
    }

    protected Domain createRunDomain(Container c, User user)
    {
        Domain runDomain = super.createRunDomain(c, user);
        addProperty(runDomain, "Replaces Previous File", PropertyType.BOOLEAN);
        addProperty(runDomain, "Date file was modified", PropertyType.DATE_TIME);
        addProperty(runDomain, "Specimen Type", PropertyType.STRING);
        addProperty(runDomain, "Additive", PropertyType.STRING);
        addProperty(runDomain, "Derivative", PropertyType.STRING);

        return runDomain;
    }

    public List<Domain> createDefaultDomains(Container c, User user)
    {
        List<Domain> result = super.createDefaultDomains(c, user);

        Container lookupContainer = c.getProject();
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);


        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ASSAY_DOMAIN_ANALYTE + ".Folder-${Container.RowId}:${AssayName}", "Analyte Properties");
        analyteDomain.setDescription("The user will be prompted to enter these properties for each of the analytes in the file they upload. This is the third and final step of the upload process.");
        DomainProperty standardNameProp = addProperty(analyteDomain, "Standard Name", PropertyType.STRING);

        ListDefinition standardList = lists.get("LuminexStandardAnalytes");
        if (standardList == null)
        {
            standardList = ListService.get().createList(lookupContainer, "LuminexStandardAnalytes");
            DomainProperty nameProperty = addProperty(standardList.getDomain(), "Name", PropertyType.STRING);
            nameProperty.setPropertyURI(standardList.getDomain().getTypeURI() + "#Name");
            standardList.setKeyName(nameProperty.getName());
            standardList.setKeyType(ListDefinition.KeyType.Varchar);
            standardList.setDescription("Standard data about analytes, including a standard name");
            standardList.setTitleColumn("Name");
            try
            {
                standardList.save(user);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        standardNameProp.setLookup(new Lookup(c.getProject(), "lists", standardList.getName()));

        addProperty(analyteDomain, "Units of Concentration", PropertyType.STRING);

        ListDefinition isotypeList = lists.get("LuminexIsotypes");
        if (isotypeList == null)
        {
            isotypeList = ListService.get().createList(lookupContainer, "LuminexIsotypes");
            DomainProperty nameProperty = addProperty(isotypeList.getDomain(), "Name", PropertyType.STRING);
            nameProperty.setPropertyURI(isotypeList.getDomain().getTypeURI() + "#Name");
            isotypeList.setKeyName("IsotypeID");
            isotypeList.setTitleColumn(nameProperty.getName());
            isotypeList.setKeyType(ListDefinition.KeyType.Varchar);
            isotypeList.setDescription("Isotypes for Luminex assays");
            try
            {
                isotypeList.save(user);

                ListItem agItem = isotypeList.createListItem();
                agItem.setKey("Ag");
                agItem.setProperty(nameProperty, "Antigen");
                agItem.save(user);

                ListItem abItem = isotypeList.createListItem();
                abItem.setKey("Ab");
                abItem.setProperty(nameProperty, "Antibody");
                abItem.save(user);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        addProperty(analyteDomain, "Isotype", PropertyType.STRING).setLookup(new Lookup(lookupContainer, "lists", isotypeList.getName()));

        addProperty(analyteDomain, "Analyte Type", PropertyType.STRING);
        addProperty(analyteDomain, "Weighting Method", PropertyType.STRING);
        
        addProperty(analyteDomain, "Bead Manufacturer", PropertyType.STRING);
        addProperty(analyteDomain, "Bead Dist", PropertyType.STRING);
        addProperty(analyteDomain, "Bead Catalog Number", PropertyType.STRING);
        
        result.add(analyteDomain);

        Domain excelRunDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ASSAY_DOMAIN_EXCEL_RUN + ".Folder-${Container.RowId}:${AssayName}", "Excel File Run Properties");
        excelRunDomain.setDescription("When the user uploads a Luminex data file, the server will try to find these properties in the header and footer of the spreadsheet, and does not prompt the user to enter them. This is part of the second step of the upload process.");
        addProperty(excelRunDomain, "File Name", PropertyType.STRING);
        addProperty(excelRunDomain, "Acquisition Date", PropertyType.DATE_TIME);
        addProperty(excelRunDomain, "Reader Serial Number", PropertyType.STRING);
        addProperty(excelRunDomain, "Plate ID", PropertyType.STRING);
        addProperty(excelRunDomain, "RP1 PMT (Volts)", PropertyType.DOUBLE);
        addProperty(excelRunDomain, "RP1 Target", PropertyType.STRING);
        result.add(excelRunDomain);

        return result;
    }

    public Map<String, TableInfo> getTableInfos(ExpProtocol protocol, QuerySchema schema)
    {
        Map<String, TableInfo> result = super.getTableInfos(protocol, schema);

        LuminexSchema luminexSchema = new LuminexSchema(schema.getUser(), schema.getContainer(), protocol);
        for (String tableName : luminexSchema.getTableNames())
        {
            result.put(tableName, luminexSchema.getTable(tableName, null));
        }
        
        return result;
    }

    public boolean shouldShowDataDescription(ExpProtocol protocol)
    {
        return false;
    }
    
    public ViewURLHelper getUploadWizardURL(Container container, ExpProtocol protocol)
    {
        ViewURLHelper url = new ViewURLHelper("Luminex", "luminexUploadWizard.view", container);
        url.addParameter("rowId", protocol.getRowId());
        return url;
    }

    public ExpData getDataForDataRow(Object dataRowId)
    {
        LuminexDataRow dataRow = Table.selectObject(LuminexSchema.getTableInfoDataRow(), dataRowId, LuminexDataRow.class);
        if (dataRow == null)
        {
            return null;
        }
        return ExperimentService.get().getExpData(dataRow.getDataId());
    }

    public FieldKey getRunIdFieldKeyFromDataRow()
    {
        return FieldKey.fromParts("Data", "Run", "RowId");
    }

    public Set<FieldKey> getParticipantIDDataKeys()
    {
        return Collections.singleton(FieldKey.fromParts("ParticipantID"));
    }

    public Set<FieldKey> getVisitIDDataKeys()
    {
        return Collections.singleton(FieldKey.fromParts("VisitID"));
    }

    public FieldKey getDataRowIdFieldKey()
    {
        return FieldKey.fromParts("RowId");
    }

    public ViewURLHelper publish(User user, ExpProtocol protocol, Container study, Set<AssayPublishKey> dataKeys, List<String> errors)
    {
        try
        {
            SimpleFilter filter = new SimpleFilter();
            List<Object> ids = new ArrayList<Object>();
            for (AssayPublishKey dataKey : dataKeys)
            {
                ids.add(dataKey.getDataId());
            }
            filter.addInClause("RowId", ids);
            LuminexDataRow[] luminexDataRows = Table.select(LuminexSchema.getTableInfoDataRow(), Table.ALL_COLUMNS, filter, null, LuminexDataRow.class);

            Map<String, Object>[] dataMaps = new Map[luminexDataRows.length];

            Map<Integer, Analyte> analytes = new HashMap<Integer, Analyte>();
            List<PropertyDescriptor> types = new ArrayList<PropertyDescriptor>();

            // Map from data id to experiment run source
            Map<Integer, ExpRun> runs = new HashMap<Integer, ExpRun>();

            // Map from run to run properties
            Map<ExpRun, Map<String, ObjectProperty>> runProperties = new HashMap<ExpRun, Map<String, ObjectProperty>>();

            PropertyDescriptor[] runPDs = getRunPropertyColumns(protocol);
            PropertyDescriptor[] uploadSetPDs = getUploadSetColumns(protocol);

            List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
            pds.addAll(Arrays.asList(runPDs));
            pds.addAll(Arrays.asList(uploadSetPDs));

            int index = 0;
            for (LuminexDataRow luminexDataRow : luminexDataRows)
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                addProperty(study, "RowId", luminexDataRow.getRowId(), dataMap, types);
                addProperty(study, "ConcInRangeString", luminexDataRow.getConcInRangeString(), dataMap, types);
                addProperty(study, "ConcInRange", luminexDataRow.getConcInRange(), dataMap, types);
                addProperty(study, "ConcInRangeOORIndicator", luminexDataRow.getConcInRangeOORIndicator(), dataMap, types);
                addProperty(study, "ExpConc", luminexDataRow.getExpConc(), dataMap, types);
                addProperty(study, "FI", luminexDataRow.getFi(), dataMap, types);
                addProperty(study, "FIString", luminexDataRow.getFiString(), dataMap, types);
                addProperty(study, "FIOORIndicator", luminexDataRow.getFiOORIndicator(), dataMap, types);
                addProperty(study, "FIBackground", luminexDataRow.getFiBackground(), dataMap, types);
                addProperty(study, "FIBackgroundString", luminexDataRow.getFiBackgroundString(), dataMap, types);
                addProperty(study, "FIBackgroundOORIndicator", luminexDataRow.getFiBackgroundOORIndicator(), dataMap, types);
                addProperty(study, "ObsConcString", luminexDataRow.getObsConcString(), dataMap, types);
                addProperty(study, "ObsConc", luminexDataRow.getObsConc(), dataMap, types);
                addProperty(study, "ObsConcOORIndicator", luminexDataRow.getObsConcOORIndicator(), dataMap, types);
                addProperty(study, "ObsOverExp", luminexDataRow.getObsOverExp(), dataMap, types);
                addProperty(study, "StdDev", luminexDataRow.getStdDev(), dataMap, types);
                addProperty(study, "StdDevString", luminexDataRow.getStdDevString(), dataMap, types);
                addProperty(study, "StdDevOORIndicator", luminexDataRow.getStdDevOORIndicator(), dataMap, types);
                addProperty(study, "Type", luminexDataRow.getType(), dataMap, types);
                addProperty(study, "Well", luminexDataRow.getWell(), dataMap, types);
                addProperty(study, "Dilution", luminexDataRow.getDilution(), dataMap, types);
                addProperty(study, "DataRowGroup", luminexDataRow.getDataRowGroup(), dataMap, types);
                addProperty(study, "Ratio", luminexDataRow.getRatio(), dataMap, types);
                addProperty(study, "SamplingErrors", luminexDataRow.getSamplingErrors(), dataMap, types);
                addProperty(study, "Outlier", luminexDataRow.isOutlier(), dataMap, types);
                addProperty(study, "SourceLSID", new Lsid("LuminexDataRow", Integer.toString(luminexDataRow.getRowId())).toString(), dataMap, types);

                Analyte analyte = analytes.get(luminexDataRow.getAnalyteId());
                if (analyte == null)
                {
                    analyte = Table.selectObject(LuminexSchema.getTableInfoAnalytes(), luminexDataRow.getAnalyteId(), Analyte.class);
                    analytes.put(analyte.getRowId(), analyte);
                }
                addProperty(study, "Analyte Name", analyte.getName(), dataMap, types);
                addProperty(study, "Analyte FitProb", analyte.getFitProb(), dataMap, types);
                addProperty(study, "Analyte RegressionType", analyte.getRegressionType(), dataMap, types);
                addProperty(study, "Analyte ResVar", analyte.getResVar(), dataMap, types);
                addProperty(study, "Analyte StdCurve", analyte.getStdCurve(), dataMap, types);
                addProperty(study, "Analyte MinStandardRecovery", analyte.getMinStandardRecovery(), dataMap, types);
                addProperty(study, "Analyte MaxStandardRecovery", analyte.getMaxStandardRecovery(), dataMap, types);

                ExpRun run = runs.get(luminexDataRow.getDataId());
                if (run == null)
                {
                    ExpData data = ExperimentService.get().getExpData(luminexDataRow.getDataId());
                    run = data.getRun();
                    runs.put(luminexDataRow.getDataId(), run);
                }
                addProperty(study, "Run Name", run.getName(), dataMap, types);
                addProperty(study, "Run Comments", run.getComment(), dataMap, types);
                addProperty(study, "Run CreatedOn", run.getCreated(), dataMap, types);
                User createdBy = run.getCreatedBy();
                addProperty(study, "Run CreatedBy", createdBy == null ? null : createdBy.getDisplayName(), dataMap, types);

                Map<String, ObjectProperty> props = runProperties.get(run);
                if (props == null)
                {
                    props = OntologyManager.getPropertyObjects(run.getContainer().getId(), run.getLSID());
                    runProperties.put(run, props);
                }
                for (PropertyDescriptor pd : pds)
                {
                    ObjectProperty prop = props.get(pd.getPropertyURI());
                    if (prop != null && !TARGET_STUDY_PROPERTY_NAME.equals(pd.getName()))
                    {
                        PropertyDescriptor publishPD = pd.clone();
                        publishPD.setName("Run " + pd.getName());
                        addProperty(publishPD, prop.value(), dataMap, types);
                    }
                }

                for (AssayPublishKey dataKey : dataKeys)
                {
                    if (((Integer)dataKey.getDataId()).intValue() == luminexDataRow.getRowId())
                    {
                        dataMap.put("ParticipantID", dataKey.getParticipantId());
                        dataMap.put("SequenceNum", dataKey.getVisitId());
                        break;
                    }
                }

                dataMaps[index++] = dataMap;
            }
            return AssayPublishService.get().publishAssayData(user, study, protocol.getName(), dataMaps, types, errors);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (ServletException e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Collections.<ParticipantVisitResolverType>singletonList(new SpecimenIDLookupResolverType());
    }
}
