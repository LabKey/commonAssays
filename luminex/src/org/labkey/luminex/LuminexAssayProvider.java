package org.labkey.luminex;

import org.labkey.api.study.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.*;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QuerySchema;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

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

    public AssayDataCollector[] getDataCollectors()
    {
        return new AssayDataCollector[] { new FileUploadDataCollector() };
    }

    public PropertyDescriptor[] getRunPropertyColumns(Protocol protocol)
    {
        List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>(Arrays.asList(super.getRunPropertyColumns(protocol)));
        result.addAll(Arrays.asList(getPropertiesForDomainPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN)));

        return result.toArray(new PropertyDescriptor[result.size()]);
    }

    public TableInfo createDataTable(QuerySchema schema, String alias, Protocol protocol)
    {
        return new LuminexSchema(schema.getUser(), schema.getContainer()).createDataRowTable(alias);
    }

    public PropertyDescriptor[] getRunDataColumns(Protocol protocol)
    {
        throw new UnsupportedOperationException();
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

        LuminexSchema luminexSchema = new LuminexSchema(schema.getUser(), schema.getContainer());
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


}
