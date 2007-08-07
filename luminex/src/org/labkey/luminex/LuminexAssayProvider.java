package org.labkey.luminex;

import org.labkey.api.study.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.data.*;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;

import javax.servlet.ServletException;
import java.util.*;
import java.sql.SQLException;
import java.io.IOException;
import java.io.File;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends DefaultAssayProvider
{
    public static final String ASSAY_DOMAIN_ANALYTE = Protocol.ASSAY_DOMAIN_PREFIX + "Analyte";
    public static final String ASSAY_DOMAIN_EXCEL_RUN = Protocol.ASSAY_DOMAIN_PREFIX + "ExcelRun";

    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", LuminexExcelDataHandler.LUMINEX_DATA_LSID_PREFIX);
    }

    public String getName()
    {
        return "Luminex";
    }

    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles)
    {
        List<AssayDataCollector> result = new ArrayList<AssayDataCollector>();
        result.add(new PreviouslyUploadedDataCollector(uploadedFiles));
        result.add(new FileUploadDataCollector());
        return result;
    }

    public PropertyDescriptor[] getRunPropertyColumns(Protocol protocol)
    {
        List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>(Arrays.asList(super.getRunPropertyColumns(protocol)));
        result.addAll(Arrays.asList(getPropertiesForDomainPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN)));

        return result.toArray(new PropertyDescriptor[result.size()]);
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, Protocol protocol)
    {
        return new LuminexSchema(schema.getUser(), schema.getContainer(), protocol).createDataRowTable(alias);
    }

    public PropertyDescriptor[] getRunDataColumns(Protocol protocol)
    {
        throw new UnsupportedOperationException();
    }


    protected Domain createUploadSetDomain(Container c)
    {
        Domain uploadSetDomain = super.createUploadSetDomain(c);
        addProperty(uploadSetDomain, "Species", PropertyType.STRING);
        addProperty(uploadSetDomain, "Lab ID", PropertyType.STRING);
        addProperty(uploadSetDomain, "Units of Concentration", PropertyType.STRING);
        addProperty(uploadSetDomain, "Analyte Type", PropertyType.STRING);
        addProperty(uploadSetDomain, "Analysis Software", PropertyType.STRING);
        addProperty(uploadSetDomain, "Weighting Method", PropertyType.STRING);
        addProperty(uploadSetDomain, "Bead Manufacturer", PropertyType.STRING);
        addProperty(uploadSetDomain, "Bead Catalog Number", PropertyType.STRING);

        return uploadSetDomain;
    }

    protected Domain createRunDomain(Container c)
    {
        Domain runDomain = super.createRunDomain(c);
        addProperty(runDomain, "Isotype", PropertyType.STRING);
        addProperty(runDomain, "Replaces Previous File", PropertyType.BOOLEAN);
        addProperty(runDomain, "Date file was modified", PropertyType.DATE_TIME);
        addProperty(runDomain, "Specimen Type", PropertyType.STRING);
        addProperty(runDomain, "Additive", PropertyType.STRING);
        addProperty(runDomain, "Derivative", PropertyType.STRING);

        return runDomain;
    }

    public List<Domain> createDefaultDomains(Container c)
    {
        List<Domain> result = super.createDefaultDomains(c);

        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ASSAY_DOMAIN_ANALYTE + ".Folder-${Container.RowId}:${AssayName}", "Analyte Properties");
        result.add(analyteDomain);

        Domain excelRunDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ASSAY_DOMAIN_EXCEL_RUN + ".Folder-${Container.RowId}:${AssayName}", "Excel File Run Properties");
        addProperty(excelRunDomain, "File Name", PropertyType.STRING);
        addProperty(excelRunDomain, "Acquisition Date", PropertyType.DATE_TIME);
        addProperty(excelRunDomain, "Reader Serial Number", PropertyType.STRING);
        addProperty(excelRunDomain, "Plate ID", PropertyType.STRING);
        addProperty(excelRunDomain, "RP1 PMT (Volts)", PropertyType.DOUBLE);
        addProperty(excelRunDomain, "RP1 Target", PropertyType.STRING);
        result.add(excelRunDomain);

        return result;
    }

    public Map<String, TableInfo> getTableInfos(Protocol protocol, QuerySchema schema)
    {
        Map<String, TableInfo> result = super.getTableInfos(protocol, schema);

        LuminexSchema luminexSchema = new LuminexSchema(schema.getUser(), schema.getContainer(), protocol);
        for (String tableName : luminexSchema.getTableNames())
        {
            result.put(tableName, luminexSchema.getTable(tableName, null));
        }
        
        return result;
    }

    public boolean shouldShowDataDescription(Protocol protocol)
    {
        return false;
    }
    
    public ViewURLHelper getUploadWizardURL(Container container, Protocol protocol)
    {
        ViewURLHelper url = new ViewURLHelper("Luminex", "luminexUploadWizard.view", container);
        url.addParameter("rowId", protocol.getRowId());
        return url;
    }

    public Data getDataForDataRow(Object dataRowId)
    {
        LuminexDataRow dataRow = Table.selectObject(LuminexSchema.getTableInfoDataRow(), dataRowId, LuminexDataRow.class);
        if (dataRow == null)
        {
            return null;
        }
        try
        {
            return ExperimentService.get().getData(dataRow.getDataId());
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
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

    private PropertyDescriptor addPropertyDescriptor(String name, Container c, PropertyType type, Map<String, PropertyDescriptor> pds)
    {
        String uri = new Lsid("LuminexDataRowProperty", "Folder-" + c.getRowId(), name).toString();
        PropertyDescriptor pd = new PropertyDescriptor(uri, type.getTypeUri(), name, c);
        pds.put(pd.getName(), pd);
        return pd;
    }

    public List<String> publish(User user, Protocol protocol, Container study, Set<AssayPublishKey> dataKeys)
    {
        try
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
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
            Map<String, PropertyType> types = new HashMap<String, PropertyType>();

            // Map from data id to experiment run source
            Map<Integer, ExpRun> runs = new HashMap<Integer, ExpRun>();

            // Map from run to run properties
            Map<ExpRun, Map<String, ObjectProperty>> runProperties = new HashMap<ExpRun, Map<String, ObjectProperty>>();

            PropertyDescriptor[] runPDs = provider.getRunPropertyColumns(protocol);

            int index = 0;
            for (LuminexDataRow luminexDataRow : luminexDataRows)
            {
                Map<String, Object> dataMap = new HashMap<String, Object>();
                addProperty("RowId", luminexDataRow.getRowId(), dataMap, types);
                addProperty("ConcInRange", luminexDataRow.getConcInRange(), dataMap, types);
                addProperty("ConcInRangeOORIndicator", luminexDataRow.getConcInRangeOORIndicator(), dataMap, types);
                addProperty("ExpConc", luminexDataRow.getExpConc(), dataMap, types);
                addProperty("FI", luminexDataRow.getFi(), dataMap, types);
                addProperty("FIBackground", luminexDataRow.getFiBackground(), dataMap, types);
                addProperty("ObsConc", luminexDataRow.getObsConc(), dataMap, types);
                addProperty("ObsConcOORIndicator", luminexDataRow.getObsConcOORIndicator(), dataMap, types);
                addProperty("ObsOverExp", luminexDataRow.getObsOverExp(), dataMap, types);
                addProperty("StdDev", luminexDataRow.getStdDev(), dataMap, types);
                addProperty("Type", luminexDataRow.getType(), dataMap, types);
                addProperty("Well", luminexDataRow.getWell(), dataMap, types);
                addProperty("SourceLSID", new Lsid("LuminexDataRow", Integer.toString(luminexDataRow.getRowId())).toString(), dataMap, types);

                Analyte analyte = analytes.get(luminexDataRow.getAnalyteId());
                if (analyte == null)
                {
                    analyte = Table.selectObject(LuminexSchema.getTableInfoAnalytes(), luminexDataRow.getAnalyteId(), Analyte.class);
                    analytes.put(analyte.getRowId(), analyte);
                }
                addProperty("Analyte Name", analyte.getName(), dataMap, types);
                addProperty("Analyte FitProb", analyte.getFitProb(), dataMap, types);
                addProperty("Analyte RegressionType", analyte.getRegressionType(), dataMap, types);
                addProperty("Analyte ResVar", analyte.getResVar(), dataMap, types);
                addProperty("Analyte StdCurve", analyte.getStdCurve(), dataMap, types);
                addProperty("Analyte MinStandardRecovery", analyte.getMinStandardRecovery(), dataMap, types);
                addProperty("Analyte MaxStandardRecovery", analyte.getMaxStandardRecovery(), dataMap, types);

                ExpRun run = runs.get(luminexDataRow.getDataId());
                if (run == null)
                {
                    ExpData data = ExperimentService.get().getExpData(luminexDataRow.getDataId());
                    run = data.getRun();
                }
                addProperty("Run Name", run.getName(), dataMap, types);
                addProperty("Run Comments", run.getComment(), dataMap, types);
                addProperty("Run CreatedOn", run.getCreated(), dataMap, types);
                User createdBy = run.getCreatedBy();
                addProperty("Run CreatedBy", createdBy == null ? null : createdBy.getDisplayName(), dataMap, types);

                Map<String, ObjectProperty> props = runProperties.get(run);
                if (props == null)
                {
                    props = OntologyManager.getPropertyObjects(run.getContainer().getId(), run.getLSID());
                }
                for (PropertyDescriptor runPD : runPDs)
                {
                    ObjectProperty prop = props.get(runPD.getPropertyURI());
                    if (prop != null)
                    {
                        addProperty("Run " + prop.getName(), prop.value(), prop.getPropertyType(), dataMap, types);
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

            GenericAssayService.get().publishAssayData(user, study, protocol.getName(), dataMaps, types);
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

        return Collections.emptyList();

    }
}
