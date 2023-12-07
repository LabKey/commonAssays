/*
 * Copyright (c) 2009-2018 LabKey Corporation
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

package org.labkey.viability;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssayUrls;
import org.labkey.api.assay.actions.ProtocolIdForm;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

public class ViabilityController extends SpringActionController
{
    public static final String NAME = "viability";

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(ViabilityController.class, ViabilityAssayUploadWizardAction.class);

    public ViabilityController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleRedirectAction
    {
        @Override
        public ActionURL getRedirectURL(Object o)
        {
            return urlProvider(AssayUrls.class).getAssayListURL(getContainer());
        }
    }

    @RequiresSiteAdmin
    public class RecalculateSpecimenAggregatesAction extends ConfirmAction<ProtocolIdForm>
    {
        @Override
        public ModelAndView getConfirmView(ProtocolIdForm protocolIdForm, BindException errors)
        {
            return HtmlView.of("Recalculate all specimen aggregates for this assay?");
        }

        @Override
        public boolean handlePost(ProtocolIdForm protocolIdForm, BindException errors)
        {
            ExpProtocol protocol = protocolIdForm.getProtocol(false);
            ViabilityManager.updateSpecimenAggregates(getUser(), protocol.getContainer(), protocolIdForm.getProvider(), protocol, null);
            return true;
        }

        @Override
        public void validateCommand(ProtocolIdForm protocolIdForm, Errors errors)
        {
            AssayProvider provider = protocolIdForm.getProvider();
            if (!(provider instanceof ViabilityAssayProvider))
                throw new NotFoundException("Assay " + provider.getName() + " is not a viabitiliy assay.");
        }

        @Override
        public @NotNull URLHelper getSuccessURL(ProtocolIdForm protocolIdForm)
        {
            return protocolIdForm.getReturnActionURL();
        }
    }
}
