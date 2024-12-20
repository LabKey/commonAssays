/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ChangePropertyDescriptorException;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.LsidManager;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.assay.transform.DataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.assay.actions.AssayRunUploadForm;
import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayPipelineProvider;
import org.labkey.api.assay.AssayProtocolSchema;
import org.labkey.api.assay.AssayRunCreator;
import org.labkey.api.assay.AssayRunDatabaseContext;
import org.labkey.api.assay.AssayRunUploadContext;
import org.labkey.api.assay.AssayTableMetadata;
import org.labkey.api.assay.AssayUrls;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.study.assay.StudyParticipantVisitResolverType;
import org.labkey.api.study.assay.ThawListResolverType;
import org.labkey.api.assay.pipeline.AssayRunAsyncContext;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.luminex.query.LuminexProtocolSchema;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the Luminex assay type, creates default batch/run/analyte domains, sets up header links.
 */
public class LuminexAssayProvider extends AbstractAssayProvider
{
    public static final String ASSAY_DOMAIN_ANALYTE = ExpProtocol.ASSAY_DOMAIN_PREFIX + "Analyte";
    public static final String ASSAY_DOMAIN_CUSTOM_DATA = ExpProtocol.ASSAY_DOMAIN_PREFIX + "LuminexData";
    public static final String ASSAY_DOMAIN_EXCEL_RUN = ExpProtocol.ASSAY_DOMAIN_PREFIX + "ExcelRun";
    public static final String LUMINEX_DATA_ROW_LSID_PREFIX = "LuminexDataRow";
    public static final String NAME = "Luminex";

    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", (AssayDataType) ExperimentService.get().getDataType(LuminexDataHandler.NAMESPACE), ModuleLoader.getInstance().getModule(LuminexModule.class));
        setMaxFileInputs(100);  // no specific requirement for this, can be changed easily
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public LuminexProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new LuminexProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public void registerLsidHandler()
    {
        super.registerLsidHandler();
        LsidManager.get().registerHandler(LUMINEX_DATA_ROW_LSID_PREFIX, new LsidManager.ExpObjectLsidHandler()
        {
            @Override
            public ExpData getObject(Lsid lsid)
            {
                try
                {
                    return getDataForDataRow(Integer.parseInt(lsid.getObjectId()), null);
                }
                catch (NumberFormatException ignored) {}
                return null;
            }

            @Override
            @Nullable
            public ActionURL getDisplayURL(Lsid lsid)
            {
                ExpData data = getObject(lsid);
                if (data == null)
                    return null;
                ExpRun expRun = data.getRun();
                if (expRun == null)
                    return null;
                ExpProtocol protocol = expRun.getProtocol();
                if (protocol == null)
                    return null;
                return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(expRun.getContainer(), protocol, expRun.getRowId());
            }
        });
    }

    @Override
    public Long getResultRowCount(List<? extends ExpProtocol> protocols)
    {
        return new TableSelector(LuminexProtocolSchema.getTableInfoDataRow()).getRowCount();
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new AssayTableMetadata(
                this,
                protocol,
                null,
                FieldKey.fromParts("Data", "Run"),
                FieldKey.fromParts("RowId"));
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
            addResultsDomain(protocol);
            // Clear the cache so we can find the domain we just created
            protocol.setObjectProperties(null);
            return getDomainByPrefix(protocol, ASSAY_DOMAIN_CUSTOM_DATA);
        }
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

    @Override
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

    @Override
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        List<Pair<Domain, Map<DomainProperty, Object>>> result = super.createDefaultDomains(c, user);

        Domain analyteDomain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ASSAY_DOMAIN_ANALYTE), "Analyte Properties");
        analyteDomain.setDescription("Define the analyte fields for this assay design. The user will be prompted to enter these fields for each of the analytes in the file they upload.");
        addProperty(analyteDomain, "StandardName", "Standard Name", PropertyType.STRING);

        addProperty(analyteDomain, "AnalyteType", "Analyte Type", PropertyType.STRING);
        addProperty(analyteDomain, "WeightingMethod", "Weighting Method", PropertyType.STRING);

        addProperty(analyteDomain, "BeadManufacturer", "Bead Manufacturer", PropertyType.STRING);
        addProperty(analyteDomain, "BeadDist", "Bead Dist", PropertyType.STRING);
        addProperty(analyteDomain, "BeadCatalogNumber", "Bead Catalog Number", PropertyType.STRING);

        result.add(new Pair<>(analyteDomain, Collections.emptyMap()));

        Domain excelRunDomain = PropertyService.get().createDomain(c, getPresubstitutionLsid(ASSAY_DOMAIN_EXCEL_RUN), "Excel File Run Properties");
        excelRunDomain.setDescription("Define the Excel file run fields for this assay design. When the user uploads a Luminex data file, the server will try to find these fields in the header and footer of the spreadsheet, and does not prompt the user to enter them.");
        addProperty(excelRunDomain, "FileName", "File Name", PropertyType.STRING);
        addProperty(excelRunDomain, "AcquisitionDate", "Acquisition Date", PropertyType.DATE_TIME);
        addProperty(excelRunDomain, "ReaderSerialNumber", "Reader Serial Number", PropertyType.STRING);
        addProperty(excelRunDomain, "PlateID", "Plate ID", PropertyType.STRING);
        addProperty(excelRunDomain, "RP1PMTvolts", "RP1 PMT (Volts)", PropertyType.DOUBLE);
        addProperty(excelRunDomain, "RP1Target", "RP1 Target", PropertyType.STRING);
        result.add(new Pair<>(excelRunDomain, Collections.emptyMap()));

        Domain resultDomain = createResultsDomain(c, getPresubstitutionLsid(ASSAY_DOMAIN_CUSTOM_DATA), "Data Fields");
        result.add(new Pair<>(resultDomain, Collections.emptyMap()));

        return result;
    }

    private Domain createResultsDomain(Container c, String domainURI, String name)
    {
        Domain resultDomain = PropertyService.get().createDomain(c, domainURI, name);
        resultDomain.setDescription("Define the additional results fields for this assay design. The user will not be prompted for these fields but instead they can be populated by the assay's associated transformation script(s), if any.");
        return resultDomain;
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView(HtmlString.unsafe("Data files must be in the multi-sheet BioPlex Excel file format. "
            + "<span style=\"font-style: italic;\">Multiple files must share the same standard curve.</span>)"));
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, LuminexUploadWizardAction.class);
    }


    public Set<ExpData> getDatasForResultRows(Collection<Integer> rowIds, ExpProtocol protocol, ResolverCache cache)
    {
        Set<ExpData> result = new HashSet<>();
        List<Integer> dataRowIds = new TableSelector(LuminexProtocolSchema.getTableInfoDataRow(), Collections.singleton("DataId"),
                new SimpleFilter(new SimpleFilter.InClause(FieldKey.fromParts("RowId"), rowIds)), null).getArrayList(Integer.class);
        for (Integer dataRowId : dataRowIds)
        {
            ExpData data = cache.getDataById(dataRowId);
            if (data != null)
            {
                result.add(data);
            }
        }
        return result;
    }

    @Override
    protected String getSourceLSID(String runLSID, int dataId, int resultRowId)
    {
        return new Lsid(LUMINEX_DATA_ROW_LSID_PREFIX, Integer.toString(dataId)).toString();
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        return Arrays.asList(new StudyParticipantVisitResolverType(), new ThawListResolverType());
    }

    @Override
    public String getDescription()
    {
        return "Imports data in the multi-sheet BioPlex Excel file format.";
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        return new LuminexDataExchangeHandler();
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return new AssayPipelineProvider(LuminexModule.class,
                new PipelineProvider.FileTypesEntryFilter(getDataType().getFileType()),
                this, "Import Luminex");
    }

    private void addResultsDomain(ExpProtocol protocol)
    {
        String domainURI = new Lsid(ASSAY_DOMAIN_CUSTOM_DATA, "Folder-" + protocol.getContainer().getRowId(), protocol.getName()).toString();
        Domain domain = createResultsDomain(protocol.getContainer(), domainURI, protocol.getName() + " Data Fields");
        try
        {
            domain.save(null);

            ObjectProperty prop = new ObjectProperty(protocol.getLSID(), protocol.getContainer(), domainURI, domainURI);
            OntologyManager.insertProperties(protocol.getContainer(), protocol.getLSID(), prop);
        }
        catch (ChangePropertyDescriptorException | ValidationException e)
        {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public List<NavTree> getHeaderLinks(ViewContext viewContext, ExpProtocol protocol, ContainerFilter containerFilter)
    {
        List<NavTree> result = super.getHeaderLinks(viewContext, protocol, containerFilter);

        // Override the Manage assay design > Set default values > Analyte Properties action url for the Luminex assays
        NavTree manageAssayDesignTree = findTreeByText(result, MANAGE_ASSAY_DESIGN_LINK);
        if (manageAssayDesignTree != null && manageAssayDesignTree.hasChildren())
        {
            NavTree setDefaultValuesTree = findTreeByText(manageAssayDesignTree.getChildren(), SET_DEFAULT_VALUES_LINK);
            if (setDefaultValuesTree != null && setDefaultValuesTree.hasChildren())
            {
                String name = protocol.getName() + " Analyte Properties";
                NavTree analytePropsTree = findTreeByText(setDefaultValuesTree.getChildren(), name);
                if (analytePropsTree != null)
                {
                    ActionURL analytePropsUrl = new ActionURL(LuminexController.SetAnalyteDefaultValuesAction.class, viewContext.getContainer());
                    analytePropsUrl.addParameter("rowId", protocol.getRowId());
                    analytePropsUrl.addReturnURL(viewContext.getActionURL());
                    analytePropsTree.setHref(analytePropsUrl.toString());
                }
            }
        }

        String currentRunId = viewContext.getRequest().getParameter("Data.Data/Run/RowId~eq");

        // add header link for the Excluded Data Report
        ActionURL url = getExcludedReportUrl(viewContext.getContainer(), protocol, containerFilter, currentRunId);
        result.add(new NavTree("view excluded data", PageFlowUtil.addLastFilterParameter(url, AssayProtocolSchema.getLastFilterScope(protocol))));

        // add header menu for the QC Report
        NavTree qcReportMenu = new NavTree("view qc report");

        // add a URL for the titration report
        url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(viewContext.getContainer(), protocol, LuminexController.TitrationQcReportAction.class);
        if (containerFilter != null && containerFilter.getType() != null)
        {
            url.addParameter("AnalyteTitration." + QueryParam.containerFilterName, containerFilter.getType().name());
        }
        if (null != currentRunId)
        {
            url.addParameter("AnalyteTitration.Titration/Run/RowId~eq", currentRunId);
        }
        // just show titrations that are either standards or qc controls
        qcReportMenu.addChild(new NavTree("view titration qc report", PageFlowUtil.addLastFilterParameter(url, AssayProtocolSchema.getLastFilterScope(protocol))));

        // add a URL for the single point control report
        url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(viewContext.getContainer(), protocol, LuminexController.SinglePointControlQcReportAction.class);
        if (containerFilter != null && containerFilter.getType() != null)
        {
            url.addParameter("AnalyteSinglePointControl." + QueryParam.containerFilterName, containerFilter.getType().name());
        }
        if (null != currentRunId)
        {
            url.addParameter("AnalyteSinglePointControl.SinglePointControl/Run/RowId~eq", currentRunId);
        }
        qcReportMenu.addChild(new NavTree("view single point control qc report", PageFlowUtil.addLastFilterParameter(url, AssayProtocolSchema.getLastFilterScope(protocol))));

        // add link to view Levey-Jennings reports
        qcReportMenu.addChild("view levey-jennings reports", new ActionURL(LuminexController.LeveyJenningsMenuAction.class, viewContext.getContainer()).addParameter("rowId", protocol.getRowId()));

        // add link to manage guide sets
        qcReportMenu.addChild("view guide sets", new ActionURL(LuminexController.ManageGuideSetAction.class, viewContext.getContainer()).addParameter("rowId", protocol.getRowId()));

        result.add(qcReportMenu);

        return result;
    }

    private NavTree findTreeByText(Collection<NavTree> trees, String childText)
    {
        if (trees != null && !trees.isEmpty())
        {
            for (NavTree tree : trees)
            {
                if (tree.getText() != null && tree.getText().equalsIgnoreCase(childText))
                    return tree;
            }
        }
        return null;
    }

    public static ActionURL getExcludedReportUrl(Container container, ExpProtocol protocol, ContainerFilter containerFilter, String currentRunId)
    {
        return getExcludedReportUrl(container, protocol, null==containerFilter ? null : containerFilter.getType(), currentRunId);
    }

    public static ActionURL getExcludedReportUrl(Container container, ExpProtocol protocol, ContainerFilter.Type containerFilterType, String currentRunId)
    {
        // add header link for the Excluded Data Report
        ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(container, protocol, LuminexController.ExcludedDataAction.class);
        if (containerFilterType != null)
        {
            url.addParameter(protocol.getName() + " RunExclusion." + QueryParam.containerFilterName, containerFilterType.name());
            url.addParameter(protocol.getName() + " WellExclusion." + QueryParam.containerFilterName, containerFilterType.name());
            url.addParameter(protocol.getName() + " SinglepointUnknownExclusion." + QueryParam.containerFilterName, containerFilterType.name());
            url.addParameter(protocol.getName() + " TitrationExclusion." + QueryParam.containerFilterName, containerFilterType.name());
        }
        if (null != currentRunId)
        {
            url.addParameter("RunExclusion.RunId~eq", currentRunId);
            url.addParameter("WellExclusion.DataId/Run/RowId~eq", currentRunId);
            url.addParameter("SinglepointUnknownExclusion.DataId/Run/RowId~eq", currentRunId);
            url.addParameter("TitrationExclusion.DataId/Run/RowId~eq", currentRunId);
        }

        return url;
    }

    @Override
    public void deleteProtocol(ExpProtocol protocol, User user, @Nullable final String auditUserComment) throws ExperimentException
    {
        // Clear out the guide sets that are FK'd to the protocol
        SQLFragment deleteGuideSetSQL = new SQLFragment("DELETE FROM ");
        deleteGuideSetSQL.append(LuminexProtocolSchema.getTableInfoGuideSet());
        deleteGuideSetSQL.append(" WHERE ProtocolId = ?");
        deleteGuideSetSQL.add(protocol.getRowId());
        new SqlExecutor(LuminexProtocolSchema.getSchema()).execute(deleteGuideSetSQL);

        super.deleteProtocol(protocol, user, auditUserComment);
    }

    @Override
    public AssayRunDatabaseContext createRunDatabaseContext(ExpRun run, User user, HttpServletRequest request)
    {
        return new LuminexRunDatabaseContext(run, user, request);
    }

    @Override
    public AssayRunAsyncContext<?> createRunAsyncContext(AssayRunUploadContext<?> context) throws ExperimentException
    {
        return new LuminexRunAsyncContext((LuminexRunContext)context);
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        return new LuminexRunCreator(this);
    }

    @Override
    public ReRunSupport getReRunSupport()
    {
        return ReRunSupport.ReRunAndDelete;
    }

    @Override
    public boolean supportsBackgroundUpload()
    {
        return true;
    }

    public static Domain getExcelRunDomain(ExpProtocol protocol)
    {
        return AbstractAssayProvider.getDomainByPrefix(protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN);
    }

    @Override
    protected void moveAssayResults(List<ExpRun> runs, ExpProtocol protocol, Container sourceContainer, Container targetContainer, User user, AssayMoveData assayMoveData)
    {
        TableInfo dataRowTable = LuminexProtocolSchema.getTableInfoDataRow();
        SQLFragment updateSql = new SQLFragment("UPDATE ").append(dataRowTable)
                .append(" SET container = ").appendValue(targetContainer.getEntityId())
                .append(" WHERE dataid IN ( SELECT rowId FROM ")
                .append(ExperimentService.get().getTinfoData())
                .append(" WHERE runid ");
        List<Integer> runRowIds = runs.stream().map(ExpRun::getRowId).toList();
        dataRowTable.getSchema().getSqlDialect().appendInClauseSql(updateSql, runRowIds);
        updateSql.append(")");

        int updateDataRow = new SqlExecutor(dataRowTable.getSchema()).execute(updateSql);

        Map<String, Integer> updateCounts = assayMoveData.counts();
        updateCounts.put("dataRow", updateCounts.getOrDefault("dataRow", 0) + updateDataRow);
    }
}
