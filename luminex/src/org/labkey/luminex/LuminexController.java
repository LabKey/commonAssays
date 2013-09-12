/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.UrlColumn;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.actions.AssayHeaderView;
import org.labkey.api.study.actions.BaseAssayAction;
import org.labkey.api.study.actions.ProtocolIdForm;
import org.labkey.api.study.assay.AbstractAssayView;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.HttpView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
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
            LuminexProtocolSchema schema = new LuminexProtocolSchema(getUser(), getContainer(), _protocol, null);
            QuerySettings runsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.RUN_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.RUN_EXCLUSION_TABLE_NAME);
            QueryView runsView = createQueryView(runsSetting, schema, errors);
            runsView.setTitle("Excluded Analytes");
            runsView.setTitlePopupHelp("Excluded Analytes", "Shows all of the analytes that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.setupViews(runsView, false, form.getProvider(), form.getProtocol());

            QuerySettings titrationsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.TITRATION_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.TITRATION_EXCLUSION_TABLE_NAME);
            QueryView titrationsView = createQueryView(titrationsSetting, schema, errors);
            titrationsView.setTitle("Excluded Titrations");
            titrationsView.setTitlePopupHelp("Excluded Titrations", "Shows all of the titrations that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.addView(titrationsView);

            QuerySettings wellsSetting = new QuerySettings(getViewContext(), LuminexProtocolSchema.WELL_EXCLUSION_TABLE_NAME, LuminexProtocolSchema.WELL_EXCLUSION_TABLE_NAME);
            QueryView wellsView = createQueryView(wellsSetting, schema, errors);
            wellsView.setTitle("Excluded Wells");
            wellsView.setTitlePopupHelp("Excluded Wells", "Shows all of the wells that have been marked as excluded in individual runs in this folder. Data may be marked as excluded from the results views.");
            result.addView(wellsView);

            setHelpTopic(new HelpTopic("excludeAnalytes"));

            return result;
        }

        private QueryView createQueryView(QuerySettings settings, UserSchema schema, BindException errors)
        {
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

    @RequiresPermissionClass(ReadPermission.class)
    public class TitrationQcReportAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AbstractAssayView result = new AbstractAssayView();
            AssaySchema schema = form.getProvider().createProtocolSchema(getUser(), getContainer(), form.getProtocol(), null);
            QuerySettings settings = new QuerySettings(getViewContext(), LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME, LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME);
            setHelpTopic(new HelpTopic("applyGuideSets"));
            QueryView view = new QueryView(schema, settings, errors)
            {
                @Override
                protected void setupDataView(DataView ret)
                {
                    super.setupDataView(ret);

                    ActionURL graph = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getViewContext().getContainer(), _protocol, LuminexController.LeveyJenningsReportAction.class);
                    graph.addParameter("titration", "${Titration/Name}");
                    graph.addParameter("analyte", "${Analyte/Name}");
                    graph.addParameter("isotype", "${Titration/Run/Isotype}");
                    graph.addParameter("conjugate", "${Titration/Run/Conjugate}");
                    SimpleDisplayColumn graphDetails = new UrlColumn(StringExpressionFactory.createURL(graph), "graph");
                    ret.getDataRegion().addDisplayColumn(0, graphDetails);
                }
            };
            view.setShadeAlternatingRows(true);
            view.setShowBorders(true);
            view.setShowUpdateColumn(false);
            view.setFrame(WebPartView.FrameType.NONE);
            result.setupViews(view, false, form.getProvider(), form.getProtocol());

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = super.appendNavTrail(root);
            return result.addChild(_protocol.getName() + " QC Report");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class SinglePointControlQcReportAction extends BaseAssayAction<ProtocolIdForm>
    {
        private ExpProtocol _protocol;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _protocol = form.getProtocol();

            AbstractAssayView result = new AbstractAssayView();
            AssaySchema schema = form.getProvider().createProtocolSchema(getUser(), getContainer(), form.getProtocol(), null);
            QuerySettings settings = new QuerySettings(getViewContext(), LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME, LuminexProtocolSchema.ANALYTE_SINGLE_POINT_CONTROL_TABLE_NAME);
            setHelpTopic(new HelpTopic("applyGuideSets"));
            QueryView view = new QueryView(schema, settings, errors)
            {
                @Override
                protected void setupDataView(DataView ret)
                {
                    super.setupDataView(ret);

                    ActionURL graph = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(getViewContext().getContainer(), _protocol, LuminexController.LeveyJenningsReportAction.class);
                    graph.addParameter("controlName", "${SinglePointControl/Name}");
                    graph.addParameter("controlType", "SinglePoint");
                    graph.addParameter("analyte", "${Analyte/Name}");
                    graph.addParameter("isotype", "${SinglePointControl/Run/Isotype}");
                    graph.addParameter("conjugate", "${SinglePointControl/Run/Conjugate}");
                    SimpleDisplayColumn graphDetails = new UrlColumn(StringExpressionFactory.createURL(graph), "graph");
                    ret.getDataRegion().addDisplayColumn(0, graphDetails);
                }
            };
            view.setShadeAlternatingRows(true);
            view.setShowBorders(true);
            view.setShowUpdateColumn(false);
            view.setFrame(WebPartView.FrameType.NONE);
            result.setupViews(view, false, form.getProvider(), form.getProtocol());

            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = super.appendNavTrail(root);
            return result.addChild(_protocol.getName() + " QC Report");
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class LeveyJenningsReportAction extends SimpleViewAction<LeveyJenningsForm>
    {
        private LeveyJenningsForm _form;

        @Override
        public ModelAndView getView(LeveyJenningsForm form, BindException errors) throws Exception
        {
            _form = form;

            if (form.getControlName() == null)
            {
                throw new NotFoundException("No control name specified");
            }
            VBox result = new VBox();
            AssayHeaderView header = new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null);
            result.addView(header);
            JspView report = new JspView<>("/org/labkey/luminex/leveyJenningsReport.jsp", form);
            result.addView(report);
            setHelpTopic(new HelpTopic("trackLuminexAnalytes"));
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_form.getProtocol().getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _form.getProtocol()));
            result.addChild("Levey-Jennings Reports", new ActionURL(LeveyJenningsMenuAction.class, getContainer()).addParameter("rowId", _form.getProtocol().getRowId()));
            return result.addChild(_form.getControlName());
        }
    }

    @RequiresPermissionClass(ReadPermission.class)
    public class LeveyJenningsMenuAction extends SimpleViewAction<ProtocolIdForm>
    {
        private ProtocolIdForm _form;

        @Override
        public ModelAndView getView(ProtocolIdForm form, BindException errors) throws Exception
        {
            _form = form;
            VBox result = new VBox();
            AssayHeaderView header = new AssayHeaderView(form.getProtocol(), form.getProvider(), false, true, null);
            result.addView(header);
            result.addView(new LeveyJenningsMenuView(form.getProtocol()));
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            NavTree result = root.addChild("Assay List", PageFlowUtil.urlProvider(AssayUrls.class).getAssayListURL(getContainer()));
            result.addChild(_form.getProtocol().getName(), PageFlowUtil.urlProvider(AssayUrls.class).getAssayRunsURL(getContainer(), _form.getProtocol()));
            return result.addChild("Levey-Jennings Reports");
        }
    }
}
