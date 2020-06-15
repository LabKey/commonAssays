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

package org.labkey.flow.controllers.protocol;

import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolController extends BaseFlowController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ProtocolController.class);

    public ProtocolController()
    {
        setActionResolver(_actionResolver);
    }

    public abstract class ProtocolViewAction<FORM extends ProtocolForm> extends FormViewAction<FORM>
    {
        private FlowProtocol protocol;

        @Override
        public ModelAndView handleRequest(FORM form, BindException errors) throws Exception
        {
            try
            {
                protocol = form.getProtocol();
            }
            catch (UnauthorizedException e)
            {
                errors.reject(ERROR_MSG, "You don't have permission to view the protocol.");
            }
            return super.handleRequest(form, errors);
        }

        protected FlowProtocol getProtocol()
        {
            return protocol;
        }
    }

    @RequiresNoPermission
    public class BeginAction extends SimpleViewAction<ProtocolForm>
    {
        @Override
        public ModelAndView getView(ProtocolForm form, BindException errors)
        {
            return HttpView.redirect(urlFor(ProtocolController.ShowProtocolAction.class));
        }

        @Override
        public void addNavTrail(NavTree root)
        {
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowProtocolAction extends SimpleViewAction<ProtocolForm>
    {
        FlowProtocol protocol = null;

        @Override
        public ModelAndView getView(ProtocolForm form, BindException errors)
        {
            protocol = form.getProtocol();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/showProtocol.jsp", form, errors);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, protocol, "Protocol");
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowSamplesAction extends SimpleViewAction<ProtocolForm>
    {
        FlowProtocol protocol;

        @Override
        public ModelAndView getView(ProtocolForm form, BindException errors)
        {
            protocol = form.getProtocol();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/showSamples2.jsp", form, errors);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, protocol, "Show Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class JoinSampleSetAction extends ProtocolViewAction<JoinSampleTypeForm>
    {
        private int _fileCount;

        @Override
        public void validateCommand(JoinSampleTypeForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(JoinSampleTypeForm form, boolean reshow, BindException errors)
        {
            form.init();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/joinSampleType.jsp", form);
        }

        @Override
        public boolean handlePost(JoinSampleTypeForm form, BindException errors) throws Exception
        {
            Map<String, FieldKey> fields = new LinkedHashMap();
            for (int i = 0; i < form.ff_samplePropertyURI.length; i ++)
            {
                String samplePropertyURI = form.ff_samplePropertyURI[i];
                FieldKey fcsKey = form.ff_dataField[i];
                if (samplePropertyURI == null || fcsKey == null)
                    continue;
                fields.put(samplePropertyURI, fcsKey);
            }
            getProtocol().setSampleTypeJoinFields(getUser(), fields);
            _fileCount = getProtocol().updateSampleIds(getUser());

            return true;
        }

        @Override
        public ActionURL getSuccessURL(JoinSampleTypeForm form)
        {
            return getProtocol().urlFor(UpdateSamplesAction.class).addParameter("fileCount", _fileCount);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, getProtocol(), "Join Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class UpdateSamplesAction extends SimpleViewAction<UpdateSamplesForm>
    {
        FlowProtocol protocol;

        @Override
        public ModelAndView getView(UpdateSamplesForm form, BindException errors) throws Exception
        {
            protocol = form.getProtocol();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/updateSamples.jsp", form);
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, protocol, "Update Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditFCSAnalysisNameAction extends ProtocolViewAction<EditFCSAnalysisNameForm>
    {
        @Override
        public void validateCommand(EditFCSAnalysisNameForm form, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(EditFCSAnalysisNameForm form, boolean reshow, BindException errors)
        {
            form.init();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/editFCSAnalysisName.jsp", form);
        }

        @Override
        public boolean handlePost(EditFCSAnalysisNameForm form, BindException errors) throws Exception
        {
            getProtocol().setFCSAnalysisNameExpr(getUser(), form.getFieldSubstitution());
            getProtocol().updateFCSAnalysisName(getUser());
            return true;
        }

        @Override
        public ActionURL getSuccessURL(EditFCSAnalysisNameForm form)
        {
            return getProtocol().urlShow();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit FCS Analysis Name");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditFCSAnalysisFilterAction extends ProtocolViewAction<EditFCSAnalysisFilterForm>
    {
        @Override
        public void validateCommand(EditFCSAnalysisFilterForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(EditFCSAnalysisFilterForm form, boolean reshow, BindException errors)
        {
            form.init();
            return FormPage.getView("/org/labkey/flow/controllers/protocol/editFCSAnalysisFilter.jsp", form);
        }

        @Override
        public boolean handlePost(EditFCSAnalysisFilterForm form, BindException errors) throws Exception
        {
            getProtocol().setFCSAnalysisFilter(getUser(), form.getFilterValue());
            return true;
        }

        @Override
        public ActionURL getSuccessURL(EditFCSAnalysisFilterForm form)
        {
            return getProtocol().urlShow();
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit FCS Analysis Filter");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditICSMetadataAction extends ProtocolViewAction<EditICSMetadataForm>
    {
        ICSMetadata metadata;

        @Override
        public void validateCommand(EditICSMetadataForm target, Errors errors)
        {
        }

        @Override
        public ModelAndView getView(EditICSMetadataForm form, boolean reshow, BindException errors)
        {
            if (metadata == null)
                metadata = getProtocol().getICSMetadata();

            form.init(metadata);
            return FormPage.getView("/org/labkey/flow/controllers/protocol/editICSMetadata.jsp", form, errors);
        }

        @Override
        public boolean handlePost(EditICSMetadataForm form, BindException errors) throws Exception
        {
            // Populate a new ICSMetadata from the form posted values.
            metadata = new ICSMetadata();
            metadata.setSpecimenIdColumn(form.getSpecimenIdColumn());
            metadata.setParticipantColumn(form.getParticipantColumn());
            metadata.setVisitColumn(form.getVisitColumn());
            metadata.setDateColumn(form.getDateColumn());
            metadata.setMatchColumns(form.getMatchColumns());
            metadata.setBackgroundFilter(form.getBackgroundFilters());

            if (metadata.isEmpty())
            {
                getProtocol().setICSMetadata(getUser(), null);
                return true;
            }
            else
            {
                for (String error : metadata.getErrors())
                    errors.reject(ERROR_MSG, error);

                if (errors.hasErrors())
                    return false;

                String value = metadata.toXmlString();
                getProtocol().setICSMetadata(getUser(), value);
                return true;
            }
        }

        @Override
        public ActionURL getSuccessURL(EditICSMetadataForm form)
        {
            ActionURL url = form.getReturnActionURL();
            if (url == null)
                url = getProtocol().urlShow();
            return url;
        }

        @Override
        public void addNavTrail(NavTree root)
        {
            addFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit Metadata");
        }
    }
}
