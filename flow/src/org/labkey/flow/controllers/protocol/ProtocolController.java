/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
import org.labkey.api.action.SpringActionController;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.ICSMetadata;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolController extends SpringFlowController<ProtocolController.Action>
{
    public enum Action
    {
        begin,
        showProtocol,
        joinSampleSet,
        updateSamples,
        editFCSAnalysisName,
        editFCSAnalysisFilter,
        editICSMetadata,
    }

    static SpringActionController.DefaultActionResolver _actionResolver = new SpringActionController.DefaultActionResolver(ProtocolController.class);

    public ProtocolController() throws Exception
    {
        super();
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

    @RequiresPermission(ACL.PERM_NONE)
    public class BeginAction extends SimpleViewAction<ProtocolForm>
    {
        public ModelAndView getView(ProtocolForm form, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(ProtocolController.Action.showProtocol));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
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
            return appendFlowNavTrail(root, protocol, "Protocol", Action.showProtocol);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
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
            return getProtocol().urlFor(Action.updateSamples);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, getProtocol(), "Join Samples", Action.joinSampleSet);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
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
            return appendFlowNavTrail(root, protocol, "Update Samples", Action.updateSamples);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
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
            return appendFlowNavTrail(root, getProtocol(), "Edit FCS Analysis Name", Action.editFCSAnalysisName);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
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
            return appendFlowNavTrail(root, getProtocol(), "Edit FCS Analysis Filter", Action.editFCSAnalysisFilter);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class EditICSMetadataAction extends ProtocolViewAction<EditICSMetadataForm>
    {
        public void validateCommand(EditICSMetadataForm target, Errors errors)
        {
        }

        public ModelAndView getView(EditICSMetadataForm form, boolean reshow, BindException errors) throws Exception
        {
            String metadata = getProtocol().getICSMetadataString();
            if (metadata == null)
                metadata = "";
            form.setMetadata(metadata);
            return FormPage.getView(ProtocolController.class, form, "editICSMetadata.jsp");
        }

        public boolean handlePost(EditICSMetadataForm form, BindException errors) throws Exception
        {
            ICSMetadata metadata = ICSMetadata.fromString(form.getMetadata());
            getProtocol().setICSMetadata(getUser(), form.getMetadata());
            return true;
        }

        public ActionURL getSuccessURL(EditICSMetadataForm form)
        {
            return getProtocol().urlShow();
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, getProtocol(), "Edit ICS Metadata", Action.editICSMetadata);
        }
    }
}
