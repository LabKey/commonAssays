/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

package org.labkey.flow.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpExperiment;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.IAssayDomainType;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.gwt.client.DefaultValueType;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.qc.DataExchangeHandler;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.study.TimepointType;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayProviderSchema;
import org.labkey.api.study.assay.AssayRunCreator;
import org.labkey.api.study.assay.AssayTableMetadata;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.flow.FlowModule;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.run.RunsForm;
import org.labkey.flow.query.FlowSchema;
import org.labkey.flow.script.FlowPipelineProvider;
import org.labkey.flow.view.FlowQueryView;
import org.labkey.flow.webparts.AnalysesWebPart;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 5/21/12
 */
public class FlowAssayProvider extends AbstractAssayProvider
{
    public static final String ASSAY_NAME = "Flow";

    public FlowAssayProvider()
    {
        // NOTE: Flow uses generic 'Protocol' and 'Run' LSID namespace prefixes so we can't
        // register LSID handlers in the same manner as the AbstractAssayProvider.
        // FlowAssayProvider.getPriority() will find the 'Flow' protocol in the container.
        //super(FlowProtocol.getProtocolLSIDPrefix(), FlowRun.getRunLSIDPrefix(), null);
        super(null, null, null, ModuleLoader.getInstance().getModule(FlowModule.NAME));
    }

    public static class FlowAssayTableMetadata extends AssayTableMetadata
    {
        final ICSMetadata _metadata;
        final boolean _relativeFromFCSFileTable;

        public FlowAssayTableMetadata(AssayProvider provider, ExpProtocol protocol, boolean relativeFromFCSFileTable)
        {
            super(provider, protocol, null, FieldKey.fromParts("Run"), FieldKey.fromParts("RowId"));

            FlowProtocol fp = new FlowProtocol(protocol);
            _metadata = fp.getICSMetadata();
            _relativeFromFCSFileTable = relativeFromFCSFileTable;
        }

        public FieldKey getSpecimenIDFieldKey()
        {
            if (_metadata != null && _metadata.getSpecimenIdColumn() != null)
            {
                if (_relativeFromFCSFileTable)
                    return FlowSchema.SPECIMENID_FIELDKEY;
                else
                    return FieldKey.fromParts(FlowSchema.FCSFILE_FIELDKEY, FlowSchema.SPECIMENID_FIELDKEY);
            }

            return super.getSpecimenIDFieldKey();
        }

        @Override
        public FieldKey getParticipantIDFieldKey()
        {
            if (_metadata != null && _metadata.getParticipantColumn() != null)
            {
                if (_relativeFromFCSFileTable)
                    return FlowSchema.PARTICIPANTID_FIELDKEY;
                else
                    return FieldKey.fromParts(FlowSchema.FCSFILE_FIELDKEY, FlowSchema.PARTICIPANTID_FIELDKEY);
            }

            return super.getParticipantIDFieldKey();
        }

        @Override
        public FieldKey getVisitIDFieldKey(TimepointType timepointType)
        {
            if (timepointType == TimepointType.DATE)
            {
                if (_metadata != null && _metadata.getDateColumn() != null)
                {
                    if (_relativeFromFCSFileTable)
                        return FlowSchema.DATE_FIELDKEY;
                    else
                        return FieldKey.fromParts(FlowSchema.FCSFILE_FIELDKEY, FlowSchema.DATE_FIELDKEY);
                }
            }
            else if (timepointType == TimepointType.VISIT)
            {
                if (_metadata != null && _metadata.getVisitColumn() != null)
                {
                    if (_relativeFromFCSFileTable)
                        return FlowSchema.VISITID_FIELDKEY;
                    else
                        return FieldKey.fromParts(FlowSchema.FCSFILE_FIELDKEY, FlowSchema.VISITID_FIELDKEY);
                }
            }

            // Either metadata has no visit or date FieldKey or timepointType is continuous.
            return null;
        }

        @Override
        public FieldKey getTargetStudyFieldKey()
        {
            if (_relativeFromFCSFileTable)
                return FlowSchema.TARGET_STUDY_FIELDKEY;
            else
                return FieldKey.fromParts(FlowSchema.FCSFILE_FIELDKEY, FlowSchema.TARGET_STUDY_FIELDKEY);
        }

    }

    @Override
    public String getName()
    {
        return ASSAY_NAME;
    }

    @Override
    public String getDescription()
    {
        return "Flow Description";
    }

    @Override
    public Domain getRunDomain(ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public Domain getBatchDomain(ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public Domain getResultsDomain(ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public AssayRunCreator getRunCreator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AssayDataCollector> getDataCollectors(Map<String, File> uploadedFiles, AssayRunUploadForm context)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExpProtocol createAssayDefinition(User user, Container container, String name, String description) throws ExperimentException
    {
        // UNDONE: could just call FlowProtocol.ensureForContainer() ?
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Pair<Domain, Map<DomainProperty, Object>>> createDefaultDomains(Container c, User user)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpView getDataDescriptionView(AssayRunUploadForm form)
    {
        return new HtmlView("Data files must be FCS file format.");
    }

    @Override
    @Nullable
    public Pair<ExpProtocol.AssayDomainTypes, DomainProperty> findTargetStudyProperty(ExpProtocol protocol)
    {
        // UNDONE: Get target study from the FlowProtocol metadata ?
        return null;
    }

    @Override
    public ExpData getDataForDataRow(Object dataRowId, ExpProtocol protocol)
    {
        if (dataRowId == null)
            return null;

        Integer id = ConvertHelper.convert(dataRowId, Integer.class);
        if (id == null)
            return null;

        FlowDataObject fdo = FlowDataObject.fromRowId(id);
        if (fdo == null)
            return null;

        return fdo.getData();
    }

    @Override
    protected String getSourceLSID(String runLSID, int dataId)
    {
        // SourceLSID is used by assay to render links back to the original data for rows
        // that have been copied into a study dataset.
        // AbstractAssayProvider uses ExpRun's LSID, but FlowAssayProvider uses the ExpData's LSID.
        // We use the ExpData LSID because flow runs use the generic experiment LSID namespace prefix of 'Run'
        // and wouldn't easily resolve to a flow run type.
        // Flow's ExpData LSID do have a unique namespace prefix of, e.g. "Flow-FCSAnalysis", so LSIDManager can resolve them.
        FlowDataObject fdo = FlowDataObject.fromRowId(dataId);
        return fdo.getLSID();
    }

    @Override
    public void registerLsidHandler()
    {
        // Do not register a run LSID handler.
        // Flow ExpData LSID handlers are registered in the FlowDataType constructor.
    }

    @Override
    public Priority getPriority(ExpProtocol protocol)
    {
        if (ExpProtocol.ApplicationType.ExperimentRun.equals(protocol.getApplicationType()))
        {
            if (FlowProtocol.isDefaultProtocol(protocol))
                return Priority.HIGH;
        }
        return null;
    }

    @Override
    public String getProtocolPattern()
    {
        return "%:" + FlowProtocol.getProtocolLSIDPrefix() + ".%:" + FlowProtocol.DEFAULT_PROTOCOL_NAME;
    }

    @NotNull
    @Override
    public AssayTableMetadata getTableMetadata(@NotNull ExpProtocol protocol)
    {
        return new FlowAssayTableMetadata(this, protocol, false);
    }

    @Override
    public ActionURL getImportURL(Container container, ExpProtocol protocol)
    {
        return new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container);
    }

    @Override
    public AssayProviderSchema createProviderSchema(User user, Container container, Container targetStudy)
    {
        return new FlowProviderSchema(user, container, this, targetStudy);
    }

    @Override
    public AssayProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return new FlowProtocolSchema(user, container, this, protocol, targetStudy);
    }

    @Override
    public List<ParticipantVisitResolverType> getParticipantVisitResolverTypes()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Pair<Domain, Map<DomainProperty, Object>>> getDomains(ExpProtocol protocol)
    {
        return Collections.emptyList();
    }

    @Override
    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<ExpProtocol, List<Pair<Domain, Map<DomainProperty, Object>>>> getAssayTemplate(User user, Container targetContainer, ExpProtocol toCopy)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFileLinkPropertyAllowed(ExpProtocol protocol, Domain domain)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMandatoryDomainProperty(Domain domain, String propertyName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean allowDefaultValues(Domain domain)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DefaultValueType[] getDefaultValueOptions(Domain domain)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DefaultValueType getDefaultValueDefault(Domain domain)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCustomView(IAssayDomainType domainType, boolean details)
    {
        return false;
    }

    @Override
    public ModelAndView createBeginView(ViewContext context, ExpProtocol protocol)
    {
        return null;
    }

    @Override
    public ModelAndView createBatchesView(ViewContext context, ExpProtocol protocol)
    {
        Portal.WebPart wp = new Portal.WebPart();
        wp.setIndex(1);
        wp.setRowId(-1);
        AnalysesWebPart view = new AnalysesWebPart(context, wp);
        view.setFrame(WebPartView.FrameType.NONE);
        view.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);
        return view;
    }

    @Override
    public ModelAndView createBatchDetailsView(ViewContext context, ExpProtocol protocol, ExpExperiment batch)
    {
        return null;
    }

    @Override
    public ModelAndView createRunsView(ViewContext context, ExpProtocol protocol)
    {
        RunsForm form = new RunsForm();
        form.setViewContext(context);
        return new FlowQueryView(form);
    }

    @Override
    public ModelAndView createRunDetailsView(ViewContext context, ExpProtocol protocol, ExpRun run)
    {
        return null;
    }

    @Override
    public ModelAndView createResultsView(ViewContext context, ExpProtocol protocol, BindException errors)
    {
        return null;
    }

    @Override
    public ModelAndView createResultDetailsView(ViewContext context, ExpProtocol protocol, ExpData data, Object dataRowId)
    {
        return null;
    }

    @Override
    public void deleteProtocol(ExpProtocol protocol, User user) throws ExperimentException
    {
        // Do nothing. Flow protocol can't be deleted.
    }

    @Override
    public Class<? extends Controller> getDesignerAction()
    {
        return null;
    }

    @Override
    public Class<? extends Controller> getDataImportAction()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PipelineProvider getPipelineProvider()
    {
        return PipelineService.get().getPipelineProvider(FlowPipelineProvider.NAME);
    }

    @Override
    public List<NavTree> getHeaderLinks(ViewContext viewContext, ExpProtocol protocol, ContainerFilter containerFilter)
    {
        return super.getHeaderLinks(viewContext, protocol, containerFilter);
    }

    @Override
    public void setValidationAndAnalysisScripts(ExpProtocol protocol, @NotNull List<File> scripts) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<File> getValidationAndAnalysisScripts(ExpProtocol protocol, Scope scope)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSaveScriptFiles(ExpProtocol protocol, boolean save) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSaveScriptFiles(ExpProtocol protocol)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataExchangeHandler createDataExchangeHandler()
    {
        throw new UnsupportedOperationException();
    }

    public Container getAssociatedStudyContainer(ExpProtocol protocol, Object dataId)
    {
        ExpData data = getDataForDataRow(dataId, protocol);
        if (data == null)
            return null;
        ExpRun run = data.getRun();
        if (run == null)
            return null;

        Map<String, Object> properties = OntologyManager.getProperties(run.getContainer(), run.getLSID());
        String targetStudyId = (String) properties.get(FlowProperty.TargetStudy.getPropertyDescriptor().getPropertyURI());

        if (targetStudyId != null)
            return ContainerManager.getForId(targetStudyId);
        return null;
    }
}
