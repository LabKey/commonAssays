/*
 * Copyright (c) 2006-2015 LabKey Corporation
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
        public ModelAndView getView(ProtocolForm form, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(ProtocolController.ShowProtocolAction.class));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowProtocolAction extends SimpleViewAction<ProtocolForm>
    {
        FlowProtocol protocol = null;

        public ModelAndView getView(ProtocolForm form, BindException errors) throws Exception
        {
            protocol = form.getProtocol();
            return FormPage.getView(ProtocolController.class, form, errors, "showProtocol.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, protocol, "Protocol");
        }
    }

    public static class ShowSamplesForm extends ProtocolForm
    {
        private boolean unlinkedOnly = false;

        public boolean isUnlinkedOnly() { return unlinkedOnly; }
        public void setUnlinkedOnly(boolean b) { unlinkedOnly = b; }
    }

    @RequiresPermission(ReadPermission.class)
    public class ShowSamplesAction extends SimpleViewAction<ShowSamplesForm>
    {
        FlowProtocol protocol;

        public ModelAndView getView(ShowSamplesForm form, BindException errors) throws Exception
        {
            protocol = form.getProtocol();
            return FormPage.getView(ProtocolController.class, form, errors, "showSamples2.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, protocol, "Show Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class JoinSampleSetAction extends ProtocolViewAction<JoinSampleSetForm>
    {
        public void validateCommand(JoinSampleSetForm form, Errors errors)
        {
        }

        public ModelAndView getView(JoinSampleSetForm form, boolean reshow, BindException errors) throws Exception
        {
            form.init();
            return FormPage.getView(ProtocolController.class, form, "joinSampleSet.jsp");
        }

        public boolean handlePost(JoinSampleSetForm form, BindException errors) throws Exception
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
            getProtocol().setSampleSetJoinFields(getUser(), fields);
            return true;
        }

        public ActionURL getSuccessURL(JoinSampleSetForm form)
        {
            return getProtocol().urlFor(UpdateSamplesAction.class);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, getProtocol(), "Join Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class UpdateSamplesAction extends SimpleViewAction<UpdateSamplesForm>
    {
        FlowProtocol protocol;

        public ModelAndView getView(UpdateSamplesForm form, BindException errors) throws Exception
        {
            protocol = form.getProtocol();
            form.fileCount = protocol.updateSampleIds(getUser());
            return FormPage.getView(ProtocolController.class, form, "updateSamples.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, protocol, "Update Samples");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditFCSAnalysisNameAction extends ProtocolViewAction<EditFCSAnalysisNameForm>
    {
        public void validateCommand(EditFCSAnalysisNameForm form, Errors errors)
        {
        }

        public ModelAndView getView(EditFCSAnalysisNameForm form, boolean reshow, BindException errors) throws Exception
        {
            form.init();
            return FormPage.getView(ProtocolController.class, form, "editFCSAnalysisName.jsp");
        }

        public boolean handlePost(EditFCSAnalysisNameForm form, BindException errors) throws Exception
        {
            getProtocol().setFCSAnalysisNameExpr(getUser(), form.getFieldSubstitution());
            getProtocol().updateFCSAnalysisName(getUser());
            return true;
        }

        public ActionURL getSuccessURL(EditFCSAnalysisNameForm form)
        {
            return getProtocol().urlShow();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit FCS Analysis Name");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditFCSAnalysisFilterAction extends ProtocolViewAction<EditFCSAnalysisFilterForm>
    {
        public void validateCommand(EditFCSAnalysisFilterForm target, Errors errors)
        {
        }

        public ModelAndView getView(EditFCSAnalysisFilterForm form, boolean reshow, BindException errors) throws Exception
        {
            form.init();
            return FormPage.getView(ProtocolController.class, form, "editFCSAnalysisFilter.jsp");
        }

        public boolean handlePost(EditFCSAnalysisFilterForm form, BindException errors) throws Exception
        {
            getProtocol().setFCSAnalysisFilter(getUser(), form.getFilterValue());
            return true;
        }

        public ActionURL getSuccessURL(EditFCSAnalysisFilterForm form)
        {
            return getProtocol().urlShow();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit FCS Analysis Filter");
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public class EditICSMetadataAction extends ProtocolViewAction<EditICSMetadataForm>
    {
        ICSMetadata metadata;

        public void validateCommand(EditICSMetadataForm target, Errors errors)
        {
        }

        public ModelAndView getView(EditICSMetadataForm form, boolean reshow, BindException errors) throws Exception
        {
            if (metadata == null)
                metadata = getProtocol().getICSMetadata();

            form.init(metadata);
            return FormPage.getView(ProtocolController.class, form, errors, "editICSMetadata.jsp");
        }

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

        public ActionURL getSuccessURL(EditICSMetadataForm form)
        {
            ActionURL url = form.getReturnActionURL();
            if (url == null)
                url = getProtocol().urlShow();
            return url;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(getPageConfig(), root, getProtocol(), "Edit Metadata");
        }
    }
}
