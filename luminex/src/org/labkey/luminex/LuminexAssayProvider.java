/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListItem;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.pipeline.PipelineProvider;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.*;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends AbstractAssayProvider
{
    public static final String ASSAY_DOMAIN_ANALYTE = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Analyte";
    public static final String ASSAY_DOMAIN_CUSTOM_DATA = ExpProtocol.ASSAY_DOMAIN_PREFIX + "LuminexData";
    public static final String ASSAY_DOMAIN_EXCEL_RUN = ExpProtocol.ASSAY_DOMAIN_PREFIX + "ExcelRun";
    public static final String LUMINEX_DATA_ROW_LSID_PREFIX = "LuminexDataRow";

    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", LuminexDataHandler.LUMINEX_DATA_TYPE, new AssayTableMetadata(
            null,
            FieldKey.fromParts("Data", "Run"),
            FieldKey.fromParts("RowId")));

        setMaxFileInputs(10);
    }

    public String getName()
    {
        return "Luminex";
    }

    @Override
    public AssaySchema getProviderSchema(User user, Container container, ExpProtocol protocol)
    {
        return new LuminexSchema(user, container, protocol);
    }

    protected void registerLsidHandler()
    {
        super.registerLsidHandler();
        LsidManager.get().registerHandler(LUMINEX_DATA_ROW_LSID_PREFIX, new LsidManager.ExpObjectLsidHandler()
        {
            public ExpData getObject(Lsid lsid)
            {
                return getDataForDataRow(lsid.getObjectId(), null);
            }

            public String getDisplayURL(Lsid lsid)
            {
                ExpData data = getDataForDataRow(lsid.getObjectId(), null);
                if (data == null)
                    return null;
                ExpRun expRun = data.getRun();
                if (expRun == null)
                    return null;
                ExpProtocol protocol = expRun.getProtocol();
                if (protocol == null)
                    return null;
                ActionURL dataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(expRun.getContainer(), protocol, expRun.getRowId());
                return dataURL.getLocalURIString();
            }
        });
    }

    @Override
    public ExpRunTable createRunTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        ExpRunTable result = super.createRunTable(schema, protocol);
//        result.addColumns(getDomainByPrefix(protocol, ASSAY_DOMAIN_EXCEL_RUN), null);

        /** RowId -> Name */
        final Map<FieldKey, FieldKey> pdfColumns = new HashMap<FieldKey, FieldKey>();
        TableInfo outputTable = result.getColumn(ExpRunTable.Column.Output).getFk().getLookupTableInfo();
        // Check for data outputs that are PDFs
        for (ColumnInfo columnInfo : outputTable.getColumns())
        {
            if (columnInfo.getName().toLowerCase().endsWith("pdf"))
            {
                pdfColumns.put(
                    FieldKey.fromParts(ExpRunTable.Column.Output.toString(), columnInfo.getName(), ExpDataTable.Column.RowId.toString()),
                    FieldKey.fromParts(ExpRunTable.Column.Output.toString(), columnInfo.getName(), ExpDataTable.Column.Name.toString()));
            }
        }

        // If we found any PDF outputs, render them as a direct download link since they should be plots of standard curves
        if (!pdfColumns.isEmpty())
        {
            ColumnInfo columnInfo = result.addColumn("Curves", ExpRunTable.Column.Name);
            List<FieldKey> visibleColumns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
            visibleColumns.add(Math.min(visibleColumns.size(), 3), columnInfo.getFieldKey());
            result.setDefaultVisibleColumns(visibleColumns);

            DisplayColumnFactory factory = new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new DataColumn(colInfo)
                    {
                        @Override
                        public void addQueryFieldKeys(Set<FieldKey> keys)
                        {
                            keys.addAll(pdfColumns.keySet());
                            keys.addAll(pdfColumns.values());
                        }

                        @Override
                        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                        {
                            Map<Integer, String> pdfs = new HashMap<Integer, String>();
                            for (Map.Entry<FieldKey, FieldKey> entry : pdfColumns.entrySet())
                            {
                                Number rowId = (Number)ctx.get(entry.getKey());
                                if (rowId != null)
                                {
                                    pdfs.put(rowId.intValue(), (String)ctx.get(entry.getValue()));
                                }
                            }

                            if (pdfs.size() == 1)
                            {
                                for (Map.Entry<Integer, String> entry : pdfs.entrySet())
                                {
                                    ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(schema.getContainer());
                                    url.addParameter("rowId", entry.getKey().toString());
                                    out.write("<a href=\"" + url + "\">");
                                    out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/_images/sigmoidal_curve.png\" />");
                                    out.write("</a>");
                                }
                            }
                            else if (pdfColumns.size() > 1)
                            {
                                StringBuilder sb = new StringBuilder();
                                for (Map.Entry<Integer, String> entry : pdfs.entrySet())
                                {
                                    ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowFileURL(schema.getContainer());
                                    url.addParameter("rowId", entry.getKey().toString());
                                    sb.append("<a href=\"");
                                    sb.append(url);
                                    sb.append("\">");
                                    sb.append(PageFlowUtil.filter(entry.getValue()));
                                    sb.append("</a><br/>");
                                }

                                out.write("<a onclick=\"return showHelpDiv(this, 'Standard Curves', " + PageFlowUtil.jsString(PageFlowUtil.filter(sb.toString())) + ");\">");
                                out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/_images/sigmoidal_curve.png\" />");
                                out.write("</a>");
                            }
                        }
                    };
                }
            };
            columnInfo.setDisplayColumnFactory(factory);
        }

        return result;
    }

    @Override
    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return getDomainByPrefix(protocol, ASSAY_DOMAIN_CUSTOM_DATA);
    }

    public LuminexDataTable createDataTable(AssaySchema schema, ExpProtocol protocol, boolean includeCopiedToStudyColumns)
    {
        LuminexSchema luminexSchema = new LuminexSchema(schema.getUser(), schema.getContainer(), protocol);
        luminexSchema.setTargetStudy(schema.getTargetStudy());
        LuminexDataTable table = luminexSchema.createDataRowTable();

        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, protocol, schema.getUser(), true);
        }
        return table;
    }

    @Override
    protected Pair<Domain, Map<DomainProperty, Object>> createBatchDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createBatchDomain(c, user);
        Domain domain = result.getKey();

        addProperty(domain, "Species", PropertyType.STRING);
        addProperty(domain, "LabID", "Lab ID", PropertyType.STRING);
        addProperty(domain, "AnalysisSoftware", "Analysis Software", PropertyType.STRING);

        return result;
    }

    protected Pair<Domain, Map<DomainProperty, Object>> createRunDomain(Container c, User user)
    {
        Pair<Domain, Map<DomainProperty, Object>> result = super.createRunDomain(c, user);
        Domain runDomain = result.getKey();
        addProperty(runDomain, "ReplacesPreviousFile", "Replaces Previous File", PropertyType.BOOLEAN);
        addProperty(runDomain, "DateModified", "Date file was modified", PropertyType.DATE_TIME);
        addProperty(runDomain, "SpecimenType", "Specimen Type", PropertyType.STRING);
        addProperty(runDomain, "Additive", PropertyType.STRING);
        addProperty(runDomain, "Derivative", PropertyType.STRING);

        return result;
    }

    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Container lookupContainer = c.getProject();
        Map<String, ListDefinition> lists = ListService.get().getLists(lookupContainer);

        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_ANALYTE + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":${AssayName}", "Analyte Properties");
        analyteDomain.setDescription("The user will be prompted to enter these properties for each of the analytes in the file they upload. This is the third and final step of the upload process.");
        addProperty(analyteDomain, "StandardName", "Standard Name", PropertyType.STRING);

        addProperty(analyteDomain, "UnitsOfConcentration", "Units of Concentration", PropertyType.STRING);

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

        addProperty(analyteDomain, "AnalyteType", "Analyte Type", PropertyType.STRING);
        addProperty(analyteDomain, "WeightingMethod", "Weighting Method", PropertyType.STRING);
        
        addProperty(analyteDomain, "BeadManufacturer", "Bead Manufacturer", PropertyType.STRING);
        addProperty(analyteDomain, "BeadDist", "Bead Dist", PropertyType.STRING);
        addProperty(analyteDomain, "BeadCatalogNumber", "Bead Catalog Number", PropertyType.STRING);
        
        result.add(new Pair<Domain, Map<DomainProperty, Object>>(analyteDomain, Collections.<DomainProperty, Object>emptyMap()));

        Domain excelRunDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_EXCEL_RUN + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":${AssayName}", "Excel File Run Properties");
        excelRunDomain.setDescription("When the user uploads a Luminex data file, the server will try to find these properties in the header and footer of the spreadsheet, and does not prompt the user to enter them. This is part of the second step of the upload process.");
        addProperty(excelRunDomain, "FileName", "File Name", PropertyType.STRING);
        addProperty(excelRunDomain, "AcquisitionDate", "Acquisition Date", PropertyType.DATE_TIME);
        addProperty(excelRunDomain, "ReaderSerialNumber", "Reader Serial Number", PropertyType.STRING);
        addProperty(excelRunDomain, "PlateID", "Plate ID", PropertyType.STRING);
        addProperty(excelRunDomain, "RP1PMTvolts", "RP1 PMT (Volts)", PropertyType.DOUBLE);
        addProperty(excelRunDomain, "RP1Target", "RP1 Target", PropertyType.STRING);
        result.add(new Pair<Domain, Map<DomainProperty, Object>>(excelRunDomain, Collections.<DomainProperty, Object>emptyMap()));

        Domain resultDomain = createResultsDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_CUSTOM_DATA + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":" + ASSAY_NAME_SUBSTITUTION, "Data Fields");
        result.add(new Pair<Domain, Map<DomainProperty, Object>>(resultDomain, Collections.<DomainProperty, Object>emptyMap()));

        return result;
    }

    private Domain createResultsDomain(Container c, String domainURI, String name)
    {
        Domain resultDomain = PropertyService.get().createDomain(c, domainURI, name);
        resultDomain.setDescription("Additional result/data properties populated by the assay's associated transformation script, if any.");
        return resultDomain;
    }

    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("Currently the only supported file type is the multi-sheet BioPlex Excel file format.");
    }

    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, LuminexUploadWizardAction.class);
    }
    
    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        // on Postgres 8.3, we must pass in an integer row ID; passing a string that happens to be all digits isn't
        // sufficient, since 8.3 no longer does implicit type casting in this situation.
        Integer dataRowIdInt = null;
        if (dataRowId instanceof Integer)
            dataRowIdInt = ((Integer) dataRowId).intValue();
        else if (dataRowId instanceof String)
        {
            try
            {
                dataRowIdInt = Integer.parseInt((String) dataRowId);
            }
            catch (NumberFormatException e)
            {
                // we'll error out below...
            }
        }
        if (dataRowIdInt == null)
            throw new IllegalArgumentException("Luminex data rows must have integer primary keys.  PK provided: " + dataRowId);
        LuminexDataRow dataRow = Table.selectObject(LuminexSchema.getTableInfoDataRow(), dataRowIdInt, LuminexDataRow.class);
        if (dataRow == null)
        {
            return null;
        }
        return ExperimentService.get().getExpData(dataRow.getData());
    }

    protected String getSourceLSID(String runLSID, int dataId)
    {
        return new Lsid(LUMINEX_DATA_ROW_LSID_PREFIX, Integer.toString(dataId)).toString();
    }

    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    public Set<String> getReservedPropertyNames(ExpProtocol protocol, Domain domain)
    {
        Set<String> result = super.getReservedPropertyNames(protocol, domain);

        if (isDomainType(domain, protocol, ASSAY_DOMAIN_ANALYTE))
        {
            result.add("Name");
            result.add("FitProb");
            result.add("Fit Prob");
            result.add("RegressionType");
            result.add("Regression Type");
            result.add("ResVar");
            result.add("Res Var");
            result.add("StdCurve");
            result.add("Std Curve");
            result.add("MinStandardRecovery");
            result.add("Min Standard Recovery");
            result.add("MaxStandardRecovery");
            result.add("Max Standard Recovery");
        }

        if (isDomainType(domain, protocol, ASSAY_DOMAIN_CUSTOM_DATA))
        {
            result.addAll(LuminexSchema.getTableInfoDataRow().getColumnNameSet());
        }

        return result;
    }

    public String getDescription()
    {
        return "Imports data in the multi-sheet BioPlex Excel file format.";
    }

    public DataExchangeHandler createDataExchangeHandler()
    {
        return new LuminexDataExchangeHandler();        
    }

    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(LuminexModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()), 
                this, "Import Luminex");
    }

    @Override
    public void upgradeAssayDefinitions(User user, ExpProtocol protocol, double targetVersion) throws SQLException
    {
        if (targetVersion == LuminexModule.LuminexUpgradeCode.ADD_RESULTS_DOMAIN_UPGRADE)
        {
            // 11.11 is when we started supporting custom results/data fields for Luminex
            // Add the domain to any existing assay designs
            String domainURI = new Lsid(ASSAY_DOMAIN_CUSTOM_DATA, "Folder-" + protocol.getContainer().getRowId(), protocol.getName()).toString();
            Domain domain = createResultsDomain(protocol.getContainer(), domainURI, protocol.getName() + " Data Fields");
            try
            {
                domain.save(user);

                ObjectProperty prop = new ObjectProperty(protocol.getLSID(), protocol.getContainer(), domainURI, domainURI);
                OntologyManager.insertProperties(protocol.getContainer(), protocol.getLSID(), prop);
            }
            catch (ChangePropertyDescriptorException e)
            {
                throw new UnexpectedException(e);
            }
            catch (ValidationException e)
            {
                throw new UnexpectedException(e);
            }
        }
    }
}
