/*
 * Copyright (c) 2008-2015 LabKey Corporation
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

package org.labkey.elispot;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.query.CrosstabView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.plate.PlateReader;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartView;
import org.labkey.elispot.pipeline.BackgroundSubtractionJob;
import org.labkey.elispot.pipeline.ElispotPipelineProvider;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ElispotController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ElispotController.class,
            ElispotUploadWizardAction.class
        );

    public ElispotController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class RunDetailsAction extends SimpleViewAction<DetailsForm>
    {
        private ExpProtocol _protocol;
        private ExpRun _run;
        private boolean _hasRunFilter;

        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            _run = ExperimentService.get().getExpRun(form.getRowId());
            if (_run == null || !_run.getContainer().equals(getContainer()))
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }

            _protocol = _run.getProtocol();
            _hasRunFilter = hasRunFilter(_protocol, getViewContext().getActionURL());

            ElispotAssayProvider provider = (ElispotAssayProvider) AssayService.get().getProvider(_protocol);
            if (null == provider)
                throw new IllegalStateException("ElispotAssayProvider no found.");

            ElispotAssayProvider.DetectionMethodType method = provider.getDetectionMethod(getContainer(), _protocol);

            PlateSummaryBean bean = new PlateSummaryBean();
            bean.setRun(form.getRowId());
            bean.setFluorospot(method == ElispotAssayProvider.DetectionMethodType.FLUORESCENT);

            VBox view = new VBox();

            JspView plateView = new JspView<>("/org/labkey/elispot/view/plateSummary.jsp", bean);

            plateView.setTitle("Plate Summary Information Run: " + form.getRowId());
            plateView.setFrame(WebPartView.FrameType.PORTAL);

            String tableName = ElispotProtocolSchema.ANTIGEN_STATS_TABLE_NAME;

            // create the query view for antigen information
            QuerySettings settings = new QuerySettings(getViewContext(), tableName, tableName);
            settings.setAllowChooseView(true);

            CrosstabView queryView = new ElispotProtocolSchema(getUser(), getContainer(), provider, _protocol, null)
                    .createAntigenStatsQueryView(settings, errors, _hasRunFilter ? form.getRowId() : null);

            ElispotDetailsHeaderView header = new ElispotDetailsHeaderView(_protocol, provider, null);
            ActionURL url = new ActionURL(RunDetailsAction.class, getContainer()).addParameter("rowId", form.getRowId());

            if (_hasRunFilter)
            {
                header.getLinks().add(new NavTree("details for all runs", url));
            }
            else
            {
                addRunFilter(_protocol, url, form.getRowId());
                header.getLinks().add(new NavTree("details for run " + form.getRowId(), url));
            }
            view.addView(header);
            view.addView(queryView);
            view.addView(plateView);
            return view;
        }

        private boolean hasRunFilter(ExpProtocol protocol, ActionURL url)
        {
            String tableName = ElispotProtocolSchema.ANTIGEN_STATS_TABLE_NAME;
            SimpleFilter urlFilter = new SimpleFilter(getViewContext().getActionURL(), tableName);
            List<SimpleFilter.FilterClause> clauses = urlFilter.getClauses();

            if (!clauses.isEmpty())
            {
                AssayProvider provider = AssayService.get().getProvider(protocol);
                FieldKey column = provider.getTableMetadata(protocol).getRunRowIdFieldKeyFromResults();

                for (SimpleFilter.FilterClause clause : clauses)
                {
                    if (clause.getFieldKeys().contains(column))
                        return true;
                }
            }
            return false;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String title;

            if (_hasRunFilter)
                title = "Run " + _run.getRowId() + " Details";
            else
                title = "Details for All Runs";

            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(_run.getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(_run.getContainer(), _protocol);
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(_run.getContainer(), _protocol, _run.getRowId());
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild(_protocol.getName() + " Data", runDataURL).addChild(title);
        }
    }

    private List<WellInfo> createWellInfoList(ExpRun run, ExpProtocol protocol, AbstractPlateBasedAssayProvider provider,
                                              PlateTemplate template, PlateReader reader) throws SQLException, ExperimentException
    {
        List<WellInfo> wellInfos = new ArrayList<>();

        List<? extends ExpData> data = run.getOutputDatas(ExperimentService.get().getDataType(ElispotDataHandler.NAMESPACE));
        assert(data.size() == 1);

        Map<String, ExpMaterial> inputs = new HashMap<>();
        for (Map.Entry<ExpMaterial,String> e : run.getMaterialInputs().entrySet())
            inputs.put(e.getValue(), e.getKey());

        for (int row=0; row < template.getRows(); row++)
        {
            for (int col=0; col < template.getColumns(); col++)
            {
                Position position = template.getPosition(row, col);

                Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(data.get(0).getLSID(), position);
                for (RunDataRow dataRow : ElispotManager.get().getRunDataRows(dataRowLsid.toString(), run.getContainer()))
                {
                    WellInfo wellInfo = new WellInfo(dataRow, position);
                    wellInfo.setSpotCount(reader.getWellDisplayValue(dataRow.getSpotCount()));
                    if (dataRow.getSpotSize() != null)
                        wellInfo.setSpotSize(reader.getWellDisplayValue(dataRow.getSpotSize()));
                    if (dataRow.getActivity() != null)
                        wellInfo.setActivity(String.valueOf(dataRow.getActivity()));
                    if (dataRow.getIntensity() != null)
                        wellInfo.setIntensity(String.valueOf(dataRow.getIntensity()));

                    for (ObjectProperty prop : OntologyManager.getPropertyObjects(getContainer(), dataRowLsid.toString()).values())
                    {
                        wellInfo.addWellProperty(prop);
                    }
                    wellInfos.add(wellInfo);
                }
            }
        }
        return wellInfos;
    }

    private void addRunFilter(ExpProtocol protocol, ActionURL url, int rowId)
    {
        AssayProvider provider = AssayService.get().getProvider(protocol);

        url.addFilter(ElispotProtocolSchema.ANTIGEN_STATS_TABLE_NAME, provider.getTableMetadata(protocol).getRunRowIdFieldKeyFromResults(), CompareType.EQUAL, rowId);
    }

    private class ElispotDetailsHeaderView extends AssayHeaderView
    {
        List<NavTree> _links = new ArrayList<>();

        public ElispotDetailsHeaderView(ExpProtocol protocol, AssayProvider provider, ContainerFilter containerFilter)
        {
            super(protocol, provider, true, true, containerFilter);

            _links.add(new NavTree("view runs", PageFlowUtil.addLastFilterParameter(PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getViewContext().getContainer(), _protocol, _containerFilter), AssayProtocolSchema.getLastFilterScope(_protocol))));
        }


        @Override
        public List<NavTree> getLinks()
        {
            return _links;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class RunDetailRedirectAction extends SimpleRedirectAction<DetailsForm>
    {
        public ActionURL getRedirectURL(DetailsForm form) throws Exception
        {
            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null)
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }

            ActionURL url = new ActionURL(RunDetailsAction.class, getContainer());
            url.addParameter("rowId", form.getRowId());
            addRunFilter(run.getProtocol(), url, form.getRowId());

            return url;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class GetPlateSummary extends ApiAction<DetailsForm>
    {
        @Override
        public ApiResponse execute(DetailsForm form, BindException errors) throws Exception
        {
            ApiSimpleResponse response = new ApiSimpleResponse();

            ExpRun run = ExperimentService.get().getExpRun(form.getRowId());
            if (run == null || !run.getContainer().equals(getContainer()))
            {
                throw new NotFoundException("Run " + form.getRowId() + " does not exist.");
            }

            ExpProtocol protocol = run.getProtocol();

            ElispotAssayProvider provider = (ElispotAssayProvider) AssayService.get().getProvider(protocol);
            PlateTemplate template = provider.getPlateTemplate(getContainer(), protocol);

            String plateReaderName = null;
            for (ObjectProperty prop : run.getObjectProperties().values())
            {
                if (ElispotAssayProvider.READER_PROPERTY_NAME.equals(prop.getName()))
                {
                    plateReaderName = prop.getStringValue();
                    break;
                }
            }

            if (plateReaderName != null)
            {
                PlateReader reader = provider.getPlateReader(plateReaderName);
                JSONArray rows = new JSONArray();
                Map<String, String> analyteMap = new TreeMap<>();

                for (WellInfo wellInfo : createWellInfoList(run, protocol, provider, template, reader))
                {
                    rows.put(wellInfo.toJSON());
                    if (wellInfo.getAnalyte() != null)
                    {
                        if (wellInfo.getCytokine() != null)
                            analyteMap.put(wellInfo.getAnalyte(), wellInfo.getCytokine());
                        else
                            analyteMap.put(wellInfo.getAnalyte(), "");
                    }
                }
                response.put("summary", rows);
                if (analyteMap.size() > 0)
                {
                    response.put("analytes", analyteMap.keySet());
                    response.put("analyteMap", analyteMap);
                }
                response.put("success", true);
            }
            return response;
        }
    }

    public static class DetailsForm
    {
        private int _rowId;

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }
    }

    public static class WellInfo
    {
        private String _dataRowLsid;
        private String _spotCount = "";
        private String _spotSize = "";
        private String _activity = "";
        private String _intensity = "";
        private Map<String, ObjectProperty> _wellProperties = new LinkedHashMap<>();
        private final RunDataRow _runDataRow;
        private Position _position;

        public WellInfo(@Nullable RunDataRow runDataRow, Position position)
        {
            _runDataRow = runDataRow;
            _position = position;
        }

        public String getDataRowLsid()
        {
            return _dataRowLsid;
        }

        public void setDataRowLsid(String dataRowLsid)
        {
            _dataRowLsid = dataRowLsid;
        }

        public void addWellProperty(ObjectProperty prop)
        {
            _wellProperties.put(prop.getName(), prop);
        }

        public Map<String, ObjectProperty> getWellProperties()
        {
            return _wellProperties;
        }

        public String getSpotCount()
        {
            return _spotCount;
        }

        public void setSpotCount(String spotCount)
        {
            _spotCount = spotCount;
        }

        public String getSpotSize()
        {
            return _spotSize;
        }

        public void setSpotSize(String spotSize)
        {
            _spotSize = spotSize;
        }

        public String getActivity()
        {
            return _activity;
        }

        public void setActivity(String activity)
        {
            _activity = activity;
        }

        public String getIntensity()
        {
            return _intensity;
        }

        public void setIntensity(String intensity)
        {
            _intensity = intensity;
        }

        public Position getPosition()
        {
            return _position;
        }

        public String getAnalyte()
        {
            return _runDataRow.getAnalyte();
        }

        public String getCytokine()
        {
            return _runDataRow.getCytokine();
        }

        public JSONObject toJSON()
        {
            JSONObject well = new JSONObject();

            well.put("spotCount", getSpotCount());
            well.put("activity", getActivity());
            well.put("intensity", getIntensity());
            well.put("dataRowLsid", getDataRowLsid());
            well.put("position", _position.toString());
            well.put("analyte", getAnalyte());
            well.put("cytokine", getCytokine());
            well.put("spotSize", getSpotSize());

            JSONObject wellProps = new JSONObject();
            for (ObjectProperty prop : _wellProperties.values())
            {
                // don't need the specimen lsid
                if (!ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY.equals(prop.getName()))
                    wellProps.put(prop.getName(), String.valueOf(prop.value()));
            }

            if (null != _runDataRow)
            {
                wellProps.put("SpotCount", _runDataRow.getSpotCount());
                wellProps.put("AntigenWellgroupName", _runDataRow.getAntigenWellgroupName());
                wellProps.put("WellgroupName", _runDataRow.getWellgroupName());
                wellProps.put("WellgroupLocation", _runDataRow.getWellgroupLocation());
                if (null != _runDataRow.getNormalizedSpotCount())
                    wellProps.put("NormalizedSpotCount", _runDataRow.getNormalizedSpotCount());
                if (null != _runDataRow.getActivity())
                    wellProps.put("Activity", _runDataRow.getActivity());
                if (null != _runDataRow.getAnalyte())
                    wellProps.put("Analyte", _runDataRow.getAnalyte());
                if (null != _runDataRow.getCytokine())
                    wellProps.put("Cytokine", _runDataRow.getCytokine());
                if (null != _runDataRow.getIntensity())
                    wellProps.put("Intensity", _runDataRow.getIntensity());
            }
            well.put("wellProperties", wellProps);

            return well;
        }
    }

    public static class PlateSummaryBean
    {
        private int _run;
        private boolean _fluorospot;

        public int getRun()
        {
            return _run;
        }

        public void setRun(int run)
        {
            _run = run;
        }

        public boolean isFluorospot()
        {
            return _fluorospot;
        }

        public void setFluorospot(boolean fluorospot)
        {
            _fluorospot = fluorospot;
        }
    }

    @RequiresPermission(InsertPermission.class)
    public class BackgroundSubtractionAction extends FormHandlerAction<Object>
    {
        @Override
        public void validateCommand(Object target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object o, BindException errors) throws Exception
        {
            Set<String> selections = DataRegionSelection.getSelected(getViewContext(), true);
            if (!selections.isEmpty())
            {
                ViewBackgroundInfo info = new ViewBackgroundInfo(getContainer(), getUser(), getViewContext().getActionURL());
                BackgroundSubtractionJob job = new BackgroundSubtractionJob(ElispotPipelineProvider.NAME, info,
                        PipelineService.get().findPipelineRoot(getContainer()), selections);

                PipelineService.get().queueJob(job);

                return true;
            }
            return false; 
        }

        @Override
        public URLHelper getSuccessURL(Object o)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }
    }
}
