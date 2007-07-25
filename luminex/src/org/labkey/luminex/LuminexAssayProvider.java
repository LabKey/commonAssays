package org.labkey.luminex;

import org.labkey.api.study.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.*;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.PropertyForeignKey;

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

    public List<Domain> createDefaultDomains(Container c)
    {
        List<Domain> result = super.createDefaultDomains(c);

        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:${LSIDAuthority}:" + ASSAY_DOMAIN_ANALYTE + ".Folder-${Container.RowId}:${AssayName}", "Analyte Properties");
        addProperty(analyteDomain, "Name", PropertyType.STRING);
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

    protected Domain createDataDomain(Container c)
    {
        Domain result = super.createDataDomain(c);

        DomainProperty analyteProp = addProperty(result, "Analyte", PropertyType.INTEGER);
        analyteProp.setLookup(new Lookup(c, "assay", DefaultAssayProvider.ASSAY_NAME_SUBSTITUTION + " Analytes"));

        addProperty(result, "Type", PropertyType.STRING);
        addProperty(result, "Well", PropertyType.STRING);
        addProperty(result, "Outlier", PropertyType.BOOLEAN);
        addProperty(result, "Description", PropertyType.STRING);
        addProperty(result, "FI", PropertyType.DOUBLE);
        addProperty(result, "FI - Bkgd", PropertyType.DOUBLE);
        addProperty(result, "Std Dev", PropertyType.DOUBLE);
        addProperty(result, "%CV", PropertyType.DOUBLE);
        addProperty(result, "Obs Conc", PropertyType.DOUBLE);
        addProperty(result, "Exp Conc", PropertyType.DOUBLE);
        addProperty(result, "(Obs/Exp) * 100", PropertyType.DOUBLE);
        addProperty(result, "Conc in Range", PropertyType.DOUBLE);

        return result;
    }

    public Map<String, TableInfo> getTableInfos(Protocol protocol, QuerySchema schema)
    {
        Map<String, TableInfo> result = super.getTableInfos(protocol, schema);
        
        for (String uri : protocol.retrieveObjectProperties().keySet())
        {
            Lsid lsid = new Lsid(uri);
            if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE))
            {
                FilteredTable table = new FilteredTable(OntologyManager.getTinfoObject(), schema.getContainer());

                List<FieldKey> visibleColumns = new ArrayList<FieldKey>();
                ColumnInfo col = table.wrapColumn("Properties", table.getRealTable().getColumn("ObjectId"));
                col.setIsUnselectable(true);
                PropertyDescriptor[] pds = OntologyManager.getPropertiesForType(uri, schema.getContainer());
                PropertyForeignKey fk = new PropertyForeignKey(pds, schema);
                ColumnInfo[] cols = fk.getLookupTableInfo().getColumns();
                FieldKey keyProp = new FieldKey(null, "Properties");
                for (ColumnInfo lookupCol : cols)
                    visibleColumns.add(new FieldKey(keyProp, lookupCol.getName()));
                col.setFk(fk);
                table.addColumn(col);

                table.setDefaultVisibleColumns(visibleColumns);

                result.put(protocol + " Analytes", table);
                break;
            }
        }

        return result;
    }

    public boolean shouldShowDataDescription(Protocol protocol)
    {
        return false;
    }
}
