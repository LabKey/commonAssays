/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ObjectProperty;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.api.*;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.Position;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.util.PageFlowUtil;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.*;

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

    @RequiresPermissionClass(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getViewContext().getContainer()));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class RunDetailsAction extends SimpleViewAction<DetailsForm>
    {
        private ExpProtocol _protocol;
        private ExpRun _run;

        public ModelAndView getView(DetailsForm form, BindException errors) throws Exception
        {
            _run = ExperimentService.get().getExpRun(form.getRowId());
            if (_run == null || !_run.getContainer().equals(getContainer()))
                HttpView.throwNotFound("Run " + form.getRowId() + " does not exist.");

            _protocol = _run.getProtocol();
            ElispotAssayProvider provider = (ElispotAssayProvider) AssayService.get().getProvider(_protocol);
            PlateTemplate template = provider.getPlateTemplate(getContainer(), _protocol);

            Map<Position, WellInfo> wellInfoMap = createWellInfoMap(_run, _protocol, provider, template);

            PlateSummaryBean bean = new PlateSummaryBean();
            bean.setTemplate(template);
            bean.setWellInfoMap(wellInfoMap);

            return new JspView<PlateSummaryBean>("/org/labkey/elispot/view/plateSummary.jsp", bean);
        }

        private Map<Position, WellInfo> createWellInfoMap(ExpRun run, ExpProtocol protocol, AbstractPlateBasedAssayProvider provider,
                                                          PlateTemplate template) throws SQLException
        {
            Map<Position, WellInfo> map = new HashMap<Position, WellInfo>();

            ExpData[] data = run.getOutputDatas(ElispotDataHandler.ELISPOT_DATA_TYPE);
            assert(data.length == 1);

            Domain sampleDomain = provider.getSampleWellGroupDomain(protocol);
            DomainProperty[] sampleProperties = sampleDomain.getProperties();

            Map<String, ExpMaterial> inputs = new HashMap<String, ExpMaterial>();
            for (ExpMaterial material : run.getMaterialInputs().keySet())
                inputs.put(material.getName(), material);

            for (int row=0; row < template.getRows(); row++)
            {
                for (int col=0; col < template.getColumns(); col++)
                {
                    Position position = template.getPosition(row, col);
                    WellInfo wellInfo = new WellInfo();

                    Lsid dataRowLsid = ElispotDataHandler.getDataRowLsid(data[0].getLSID(), position);
                    String specimenGroup = "";

                    for (ObjectProperty prop : OntologyManager.getPropertyObjects(getContainer(), dataRowLsid.toString()).values())
                    {
                        wellInfo.addWellProperty(prop);
                        if (ElispotDataHandler.WELLGROUP_PROPERTY_NAME.equals(prop.getName()))
                        {
                            specimenGroup = String.valueOf(prop.value());
                        }
                        else if (ElispotDataHandler.SFU_PROPERTY_NAME.equals(prop.getName()))
                        {
                            wellInfo.setTitle(String.valueOf(prop.value()));
                        }
                    }

                    // get the specimen wellgroup info
                    if (!StringUtils.isEmpty(specimenGroup))
                    {
                        ExpMaterial material = inputs.get(specimenGroup);
                        if (material != null)
                        {
                            for (DomainProperty dp : sampleProperties)
                            {
                                Object value = material.getProperty(dp);
                                wellInfo.addSpecimenProperty(dp, String.valueOf(value));
                            }
                        }
                    }
                    map.put(position, wellInfo);
                }
            }
            return map;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            ActionURL assayListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(_run.getContainer());
            ActionURL runListURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(_run.getContainer(), _protocol);
            ActionURL runDataURL = PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(_run.getContainer(), _protocol, _run.getRowId());
            return root.addChild("Assay List", assayListURL).addChild(_protocol.getName() +
                    " Runs", runListURL).addChild(_protocol.getName() + " Data", runDataURL).addChild("Run " + _run.getRowId() + " Details");
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
        private String _title = "";
        private Map<String, ObjectProperty> _wellProperties = new LinkedHashMap<String, ObjectProperty>();
        private Map<DomainProperty, String> _specimenProperties = new LinkedHashMap<DomainProperty, String>();

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

        public void addSpecimenProperty(DomainProperty pd, String value)
        {
            _specimenProperties.put(pd, value);
        }

        public Map<String, ObjectProperty> getWellProperties()
        {
            return _wellProperties;
        }

        public Map<DomainProperty, String> getSpecimenProperties()
        {
            return _specimenProperties;
        }

        public String getTitle()
        {
            return _title;
        }

        public void setTitle(String title)
        {
            _title = title;
        }

        public String getHtml()
        {
            StringBuffer sb = new StringBuffer();

            for (ObjectProperty prop : _wellProperties.values())
            {
                sb.append(prop.getName());
                sb.append(':');
                sb.append(String.valueOf(prop.value()));
                sb.append("<br/>");
            }
            return sb.toString();
        }
    }

    public static class PlateSummaryBean
    {
        private PlateTemplate _template;
        private String _dataLsid;
        private Map<String, PropertyDescriptor> _samplePropertyMap;
        private Map<String, ExpMaterial> _inputMaterialMap;
        private Map<Position, WellInfo> _wellInfoMap;

        public PlateTemplate getTemplate()
        {
            return _template;
        }

        public void setTemplate(PlateTemplate template)
        {
            _template = template;
        }

        public String getDataLsid()
        {
            return _dataLsid;
        }

        public void setDataLsid(String dataLsid)
        {
            _dataLsid = dataLsid;
        }

        public Map<String, PropertyDescriptor> getSamplePropertyMap()
        {
            return _samplePropertyMap;
        }

        public void setSamplePropertyMap(Map<String, PropertyDescriptor> samplePropertyMap)
        {
            _samplePropertyMap = samplePropertyMap;
        }

        public Map<String, ExpMaterial> getInputMaterialMap()
        {
            return _inputMaterialMap;
        }

        public void setInputMaterialMap(Map<String, ExpMaterial> inputMaterialMap)
        {
            _inputMaterialMap = inputMaterialMap;
        }

        public Map<Position, WellInfo> getWellInfoMap()
        {
            return _wellInfoMap;
        }

        public void setWellInfoMap(Map<Position, WellInfo> wellInfoMap)
        {
            _wellInfoMap = wellInfoMap;
        }
    }
}