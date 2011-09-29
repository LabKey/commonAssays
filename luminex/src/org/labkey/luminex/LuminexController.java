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

import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.study.actions.BaseAssayAction;
import org.labkey.api.study.actions.ProtocolIdForm;
import org.labkey.api.study.assay.AbstractAssayView;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.HttpView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.WebPartView;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;

/**
 * User: jeckels
 * Date: Jul 31, 2007
 */
public class LuminexController extends SpringActionController
{
    private static final DefaultActionResolver _resolver = new DefaultActionResolver(LuminexController.class,
            LuminexUploadWizardAction.class
        );

    public LuminexController()
    {
        super();
        setActionResolver(_resolver);
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
            throw new UnsupportedOperationException();
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class ExcludedDataAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;


        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AbstractAssayView result = new AbstractAssayView();
            AssaySchema schema = AssayService.get().createSchema(getUser(), getContainer());
            QuerySettings runsSetting = new QuerySettings(getViewContext(), LuminexSchema.getRunExclusionTableName(form.getProtocol()), LuminexSchema.getRunExclusionTableName(form.getProtocol()));
            QueryView runsView = createQueryView(runsSetting, schema, errors);
            runsView.setTitle("Excluded Analytes");
            result.setupViews(runsView, false, form.getProvider(), form.getProtocol());

            QuerySettings wellsSetting = new QuerySettings(getViewContext(), LuminexSchema.getWellExclusionTableName(form.getProtocol()), LuminexSchema.getWellExclusionTableName(form.getProtocol()));
            QueryView wellsView = createQueryView(wellsSetting, schema, errors);
            wellsView.setTitle("Excluded Wells");
            result.addView(wellsView);

            return result;
        }

        private QueryView createQueryView(QuerySettings settings, UserSchema schema, BindException errors)
        {
            settings.setAllowChooseQuery(false);
            QueryView result = new QueryView(schema, settings, errors);
            result.setShadeAlternatingRows(true);
            result.setShowBorders(true);
            result.setShowInsertNewButton(false);
            result.setShowImportDataButton(false);
            result.setShowDeleteButton(false);
            result.setShowUpdateColumn(false);
            result.setFrame(WebPartView.FrameType.PORTAL);
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = super.appendNavTrail(root);
            return result.addChild(_protocol.getName() + " Excluded Data");
        }
    }

    @RequiresPermissionClass(UpdatePermission.class)
    public class LeveyJenningsReport extends SimpleViewAction<TitrationForm>
    {
        @Override
        public ModelAndView getView(TitrationForm form, BindException errors) throws Exception
        {
            return new JspView<TitrationForm>("/org/labkey/luminex/leveyJenningsReport.jsp", form);
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Levey-Jennings Report");
        }
    }    
}
