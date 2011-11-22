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

import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.*;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;

import javax.servlet.http.HttpServletRequest;
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

        // Render any PDF outputswe found direct download links since they should be plots of standard curves
        ColumnInfo curvesColumn = result.addColumn("Curves", ExpRunTable.Column.Name);
        curvesColumn.setWidth("30");
        curvesColumn.setReadOnly(true);
        curvesColumn.setShownInInsertView(false);
        curvesColumn.setShownInUpdateView(false);
        curvesColumn.setDescription("Link to titration curves in PDF format. Available if assay design is configured to generate them.");
        List<FieldKey> visibleColumns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        visibleColumns.add(Math.min(visibleColumns.size(), 3), curvesColumn.getFieldKey());
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
                        else if (pdfs.size() > 1)
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

                            out.write("<a onclick=\"return showHelpDiv(this, 'Titration Curves', " + PageFlowUtil.jsString(PageFlowUtil.filter(sb.toString())) + ");\">");
                            out.write("<img src=\"" + AppProps.getInstance().getContextPath() + "/_images/sigmoidal_curve.png\" />");
                            out.write("</a>");
                        }
                    }
                };
            }
        };
        curvesColumn.setDisplayColumnFactory(factory);

        return result;
    }

    @Override
    public Domain getResultsDomain(ExpProtocol protocol)
    {
        try
        {
            return getDomainByPrefix(protocol, ASSAY_DOMAIN_CUSTOM_DATA);
        }
        catch (IllegalArgumentException e)
        {
            // This means we couldn't find the results domain.

            // We tried to add it during the the upgrade, but try it again in case we imported an old XAR file or similar
            addResultsDomain(null, protocol);
            // Clear the cache so we can find the domain we just created
            protocol.setObjectProperties(null);
            return getDomainByPrefix(protocol, ASSAY_DOMAIN_CUSTOM_DATA);
        }
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
        addProperty(runDomain, "Isotype", PropertyType.STRING).setShownInUpdateView(false);
        addProperty(runDomain, "Conjugate", PropertyType.STRING).setShownInUpdateView(false);
        addProperty(runDomain, "TestDate", "Test Date", PropertyType.DATE_TIME, "Date the assay was run");
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

        Domain analyteDomain = PropertyService.get().createDomain(c, "urn:lsid:" + XarContext.LSID_AUTHORITY_SUBSTITUTION + ":" + ASSAY_DOMAIN_ANALYTE + ".Folder-" + XarContext.CONTAINER_ID_SUBSTITUTION + ":${AssayName}", "Analyte Properties");
        analyteDomain.setDescription("The user will be prompted to enter these properties for each of the analytes in the file they upload. This is the third and final step of the upload process.");
        addProperty(analyteDomain, "StandardName", "Standard Name", PropertyType.STRING);

        addProperty(analyteDomain, "UnitsOfConcentration", "Units of Concentration", PropertyType.STRING);

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
        return new HtmlView("Data files must be in the multi-sheet BioPlex Excel file format. "
            + "<EM>(multiple files must share the same standard curve)</EM>");
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
            addResultsDomain(user, protocol);
        }
    }

    private void addResultsDomain(User user, ExpProtocol protocol)
    {
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

    @Override
    public ResultsQueryView createResultsQueryView(final ViewContext context, final ExpProtocol protocol)
    {
        UserSchema schema = QueryService.get().getUserSchema(context.getUser(), context.getContainer(), AssaySchema.NAME);
        String name = AssayService.get().getResultsTableName(protocol);
        QuerySettings settings = schema.getSettings(context, name, name);
        return new ResultsQueryView(protocol, context, settings)
        {
            @Override
            protected DataRegion createDataRegion()
            {
                ResultsDataRegion rgn = new LuminexResultsDataRegion(_provider);
                initializeDataRegion(rgn);
                return rgn;
            }

            @Override
            public DataView createDataView()
            {
                DataView result = super.createDataView();
                String runId = context.getRequest().getParameter(result.getDataRegion().getName() + ".Data/Run/RowId~eq");

                // if showing controls and user is viewing data results for a single run, add the Exclude Analytes button to button bar
                if (showControls() && runId != null)
                {
                    ActionButton excludeAnalytes = new ActionButton("excludeAnalytes", "Exclude Analytes");
                    excludeAnalytes.setScript("LABKEY.requiresScript('luminex/AnalyteExclusionPanel.js');"
                            + "LABKEY.requiresCss('luminex/Exclusion.css');"
                            + "analyteExclusionWindow('" + protocol.getName() + "', " + runId + ");");
                    excludeAnalytes.setDisplayPermission(UpdatePermission.class);

                    // todo: move the JS and CSS inclusion to the page level

                    result.getDataRegion().getButtonBar(DataRegion.MODE_GRID).add(excludeAnalytes);
                }
                return result;
            }
        };
    }

    @Override
    public List<NavTree> getHeaderLinks(ViewContext viewContext, ExpProtocol protocol, ContainerFilter containerFilter)
    {
        List<NavTree> result = super.getHeaderLinks(viewContext, protocol, containerFilter);

        String currentRunId = viewContext.getRequest().getParameter(protocol.getName() + " Data.Data/Run/RowId~eq");

        // add header link for the Excluded Data Report
        ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(viewContext.getContainer(), protocol, LuminexController.ExcludedDataAction.class);
        if (containerFilter != null && containerFilter != ContainerFilter.EVERYTHING)
        {
            url.addParameter(protocol.getName() + " WellExclusion." + QueryParam.containerFilterName, containerFilter.getType().name());
            url.addParameter(protocol.getName() + " RunExclusion." + QueryParam.containerFilterName, containerFilter.getType().name());
        }
        if (null != currentRunId)
        {
            url.addParameter(protocol.getName() + " WellExclusion.DataId/Run/RowId~eq", currentRunId);
            url.addParameter(protocol.getName() + " RunExclusion.RunId~eq", currentRunId);
        }
        result.add(new NavTree("view excluded data", PageFlowUtil.addLastFilterParameter(url)));

        // add header link for the QC Report
        url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(viewContext.getContainer(), protocol, LuminexController.QcReportAction.class);
        if (containerFilter != null && containerFilter != ContainerFilter.EVERYTHING)
        {
            url.addParameter(protocol.getName() + " AnalyteTitration." + QueryParam.containerFilterName, containerFilter.getType().name());
        }
        if (null != currentRunId)
        {
            url.addParameter(protocol.getName() + " AnalyteTitration.Titration/Run/RowId~eq", currentRunId);
        }
        // just show titrations that are either standards or qc controls
        url.addParameter(protocol.getName() + " AnalyteTitration.Titration/Unknown~eq", "false");
        result.add(new NavTree("view qc report", PageFlowUtil.addLastFilterParameter(url)));

        return result;
    }

    @Override
    public void deleteProtocol(ExpProtocol protocol, User user) throws ExperimentException
    {
        try
        {
            // Clear out the guide sets that are FK'd to the protocol
            SQLFragment deleteGuideSetSQL = new SQLFragment("DELETE FROM ");
            deleteGuideSetSQL.append(LuminexSchema.getTableInfoGuideSet());
            deleteGuideSetSQL.append(" WHERE ProtocolId = ?");
            deleteGuideSetSQL.add(protocol.getRowId());
            Table.execute(LuminexSchema.getSchema(), deleteGuideSetSQL);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }

        super.deleteProtocol(protocol, user);
    }

    @Override
    public AssayRunDatabaseContext createRunDatabaseContext(ExpRun run, User user, HttpServletRequest request)
    {
        return new LuminexRunDatabaseContext(run, user, request);
    }
}
