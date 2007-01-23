/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.mousemodel.mouse;

import org.labkey.mousemodel.MouseModelController;
import org.labkey.mousemodel.MouseModelController.MouseModelTemplateView;
import org.labkey.mousemodel.NotesView;
import org.labkey.mousemodel.sample.SampleController;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.labkey.api.attachments.Attachment;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.DownloadUrlHelper;
import org.labkey.api.attachments.AttachmentForm;
import org.labkey.api.data.*;
import org.labkey.api.exp.MaterialSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.sample.*;
import org.labkey.api.security.ACL;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.common.util.Pair;
import org.labkey.api.view.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.*;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class MouseController extends ViewController
{
    private static String _bdiSampleSource = null;
    private static Logger _log = Logger.getLogger(MouseController.class);

    private DataRegion getGridRegion(ViewForm form) throws ServletException
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(MouseSchema.getMouseView().getColumns("EntityId,MouseNo,CageName,toeNo,Sex"));
        rgn.addColumn(new ControlColumn(MouseSchema.getMouseView()));
        rgn.addColumns(MouseSchema.getMouseView().getColumns("BirthDate,StartDate,DeathDate,Weeks"));
        rgn.addColumns(getCustomColumns(MouseModelController.getModel(form)));
        rgn.getDisplayColumn(0).setVisible(false);
        ViewURLHelper urlhelp = cloneViewURLHelper();
        urlhelp.setAction("details");
        rgn.getDisplayColumn(1).setURL(urlhelp.getLocalURIString() + "&entityId=${entityId}");
        rgn.setShowRecordSelectors(true);
        ButtonBar bb = new ButtonBar();
        bb.add(new ActionButton("showBulkUpdate.view", "Update Selected"));
        rgn.setButtonBar(bb, DataRegion.MODE_GRID);
        return rgn;
    }


    private DataRegion getDefaultRegion(ViewForm form) throws ServletException
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(MouseSchema.getMouse().getColumns("EntityId,MouseNo,modelId,Sex,control,BirthDate,StartDate,DeathDate,MouseComments"));
        rgn.addColumns(getCustomColumns(MouseModelController.getModel(form)));
        rgn.getDisplayColumn(0).setVisible(false);
        return rgn;
    }

    private DataRegion getDetailsRegion(MouseForm form) throws ServletException
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(MouseSchema.getMouseView().getColumns("MouseNo,modelId,Sex,BirthDate,StartDate,DeathDate,control,MouseComments"));
        DisplayColumn dc = new DataColumn(MouseSchema.getMouseView().getColumn("CageName"))
        {
            @Override
            public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
            {
                super.renderDetailsCellContents(ctx, out);
                out.write("&nbsp;&nbsp;[<a href='begin.view?modelId=" + ctx.getRow().get("modelId") + "&MouseView.CageName~eq=" + PageFlowUtil.filter(ctx.getRow().get("CageName")) + "'>View Mice From Cage</a>]");
            }
        };
        rgn.addColumn(dc);
        rgn.addColumns(getCustomColumns(MouseModelController.getModel(form)));
        ButtonBar bb = new ButtonBar();
        bb.add(ActionButton.BUTTON_SHOW_GRID);
        bb.add(ActionButton.BUTTON_SHOW_UPDATE);

        Mouse mouse = (Mouse) form.getBean();
        ViewURLHelper urlhelp = cloneViewURLHelper();
        if (!mouse.getNecropsyComplete())
        {
            ActionButton ab = new ActionButton("necropsy", "Necropsy");
            urlhelp.setPageFlow("MouseModel-Necropsy").setAction("begin.view");
            urlhelp.replaceParameter("mouseEntityId", form.getEntityId());
            ab.setActionType(ActionButton.Action.LINK);
            ab.setURL(urlhelp.getLocalURIString());
            bb.add(ab);
        }
        rgn.setButtonBar(bb, DataRegion.MODE_DETAILS);

        return rgn;
    }


    @Jpf.Action
    /**
     * This method represents the point of entry into the pageflow
     */
    protected Forward begin(MouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        DataRegion rgn = getGridRegion(form);
        GridView gridView = new GridView(rgn);
        gridView.setContainer(form.getContainer());
        gridView.setFilter(new SimpleFilter("modelId", new Integer(MouseModelController.getModelId(form))));
        gridView.setTitle("Mice");
        Sort baseSort = new Sort("+BirthDate,+CageName,+toeNo");
        gridView.setSort(baseSort);

        _renderInTemplate(gridView, form);

        return null;
    }


    @Jpf.Action
    protected Forward delete(MouseForm form) throws Exception
    {
        //redundant, but defensive
        requiresPermission(ACL.PERM_DELETE);

        form.doDelete();
        return form.getForward("begin", (Pair[]) null, true);
    }


    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showUpdate.do", name = "validate"))
    protected Forward update(MouseForm form) throws Exception
    {
        form.doUpdate();
        return form.getPkForward("details");
    }


    @Jpf.Action
    protected Forward showUpdate(MouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        UpdateView updateView = new UpdateView(getDefaultRegion(form), form);
        updateView.setTitle("Update Mouse");
        _renderInTemplate(updateView, form);

        return null;
    }


    @Jpf.Action
    protected Forward showBulkUpdate(MouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        String[] selectedRows = form.getSelectedRows();
        if (null == selectedRows || selectedRows.length == 0)
            return form.getForward("begin.view", new Pair("modelId", new Integer(MouseModelController.getModelId(form))), true);

        DataRegion dr = new DataRegion();
        dr.addColumns(MouseSchema.getMouseView().getColumns("control,BirthDate,StartDate,DeathDate,MouseComments"));
        dr.addColumns(getCustomColumns(MouseModelController.getModel(form)));

        ButtonBar bb = new ButtonBar();
        bb.add(new ActionButton("bulkUpdate.post", "Update"));
        dr.setButtonBar(bb, DataRegion.MODE_INSERT);
        VelocityDataView view = new VelocityDataView(dr, form, "/MouseModel/Mouse/bulkUpdate.vm");
        view.setMode(DataRegion.MODE_INSERT);
        view.addObject("selectedRows", selectedRows);
        view.addObject("modelId", new Integer(MouseModelController.getModelId(form)));
        view.setTitle("Update Selected Mice");
        _renderInTemplate(view, form);

        return null;
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showBulkUpdate.do", name = "validate"))
    protected Forward bulkUpdate(BulkMouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        String[] selectedRows = form.getSelectedRows();
        if (null == selectedRows || selectedRows.length == 0)
            return form.getForward("begin.view", new Pair("modelId", new Integer(MouseModelController.getModelId(form))), true);

        Map fields = form.getTypedValues();
        Map changedFields = new HashMap();
        Collection keys = fields.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();)
        {
            String key = (String) iterator.next();
            changedFields.put(key, fields.get(key));
        }

        try
        {
            MouseSchema.getSchema().getScope().beginTransaction();
            for (int i = 0; i < selectedRows.length; i++)
                Table.update(form.getUser(), MouseSchema.getMouse(), changedFields, selectedRows[i], null);
            MouseSchema.getSchema().getScope().commitTransaction();
        }
        catch (SQLException x)
        {
            MouseSchema.getSchema().getScope().rollbackTransaction();
            throw x;
        }
        return form.getForward("begin.view", new Pair(DataRegion.LAST_FILTER_PARAM, "1"), true);
    }


    @Jpf.Action
    protected Forward details(MouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        Container c = form.getContainer();
        Mouse mouse = MouseModelManager.getMouse((Mouse) form.getBean());
        form.setBean(mouse);
        //form.refreshFromDb(false);
        DetailsView detailsView = new DetailsView(getDetailsRegion(form), form);
        detailsView.setTitle("Mouse Details");

        NotesView notesView = new NotesView(c, form.getEntityId());
        GridView sampleView = new GridView(getSamplesGridRegion(mouse));
        SimpleFilter filter = new SimpleFilter("organismId", form.getEntityId());
        sampleView.setFilter(filter);
        sampleView.setTitle("Samples");

        MouseModel model = MouseModelManager.getModel(mouse.getModelId());
        HttpView slidesView = new SampleController.SlidesView(model, mouse, getViewContext());

        JspView photoView = new JspView("/org/labkey/mousemodel/mouse/showPhoto.jsp");
        photoView.setTitle("Necropsy Photos");
        Attachment[] attachments = AttachmentService.get().getAttachments(mouse);
//        photoView.addObject("parent", form.get("entityId"));
        photoView.addObject("attachments", attachments);
        DownloadUrlHelper deleteUrl = new DownloadUrlHelper(getRequest(), "MouseModel-Mouse", getContainer().getPath(), mouse.getEntityId(), null);
        deleteUrl.setAction("showConfirmDelete.view");
        photoView.addObject("deleteUrl", deleteUrl);
        photoView.addObject("canDelete", getViewContext().hasPermission(ACL.PERM_UPDATE));

        VBox box = new VBox(detailsView, sampleView, photoView, slidesView, notesView);

        _renderInTemplate(box, form);

        return null;
    }


    private Mouse getMouse(AttachmentForm form, int perm) throws ServletException
    {
        requiresPermission(perm);

        Mouse mouse = MouseModelManager.getMouse(form.getEntityId());

        if (null == mouse || !mouse.getContainer().equals(getContainer().getId()))
            HttpView.throwNotFound("Unable to find mouse");

        return mouse;
    }


    @Jpf.Action
    protected Forward download(AttachmentForm form) throws IOException, ServletException
    {
        Mouse mouse = getMouse(form, ACL.PERM_READ);

        AttachmentService.get().download(getResponse(), mouse, form);

        return null;
    }


    @Jpf.Action
    protected Forward showConfirmDelete(AttachmentForm form) throws Exception
    {
        Mouse mouse = getMouse(form, ACL.PERM_UPDATE);  // TODO: Shouldn't this be DELETE?  But it's UPDATE above...

        return includeView(AttachmentService.get().getConfirmDeleteView(mouse, form));
    }


    @Jpf.Action
    protected Forward deleteAttachment(AttachmentForm form) throws Exception
    {
        Mouse mouse = getMouse(form, ACL.PERM_DELETE);  // TODO: DELETE or UPDATE?

        return includeView(AttachmentService.get().delete(mouse, form));
    }


    private transient Map lastSampleInserted = null;

    private DataRegion getSamplesGridRegion(Mouse mouse)
    {
        DataRegion rgn = new DataRegion();
        rgn.setColumns(getSampleTable().getColumns("OrganismId,LSID,SampleId,SampleTypeId,Description"));
        rgn.addColumn(new TrueFalseColumn(getSampleTable().getColumn("FrozenUsed"), "Used", "Unused"));
        rgn.getDisplayColumn(0).setVisible(false);
        rgn.getDisplayColumn(1).setVisible(false);
        ViewURLHelper urlhelp = cloneViewURLHelper();

        urlhelp.deleteParameters();
        urlhelp.setPageFlow("MouseModel-Sample");
        urlhelp.setAction("details");
        rgn.getDisplayColumn(2).setURL(urlhelp.getLocalURIString() + "&LSID=${LSID}&modelId=" + mouse.getModelId());

        /*
        urlhelp.setPageFlow("MouseModel-Necropsy");
        urlhelp.setAction("locateSample");
        urlhelp.replaceParameter("modelId", String.valueOf(mouse.getModelId()));
        rgn.addColumn(new SimpleDisplayColumn("<a href='"
                    + urlhelp.getLocalURIString()
                    + "&sampleId=${sampleId}&sourceId=${sourceId}'>Locate</a>")
                    {
                    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                        {
                        Boolean frozenUsed = (Boolean) ctx.get("FrozenUsed");
                        if (frozenUsed != null && frozenUsed.booleanValue())
                            out.write("(Used)");
                        else
                            super.renderGridCellContents(ctx, out);
                        }
                    });
        */

        rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
        return rgn;
    }

    private DataRegion getSamplesDefaultRegion(String organismId)
    {
        /*
        if (null == _samplesDefaultRegion)
            {
        */
        DataRegion rgn = new DataRegion();
        rgn.setColumns(getSampleTable().getUserEditableColumns());

        ButtonBar bb = new ButtonBar();
        ActionButton buttonSubmitInsert = new ActionButton("insertSample.post", "Insert Sample");
        bb.add(buttonSubmitInsert);
        rgn.setButtonBar(bb, DataRegion.MODE_INSERT);

        ButtonBar bbDetails = new ButtonBar();
        ActionButton buttonEdit = new ActionButton("showUpdateSample.view", "Edit");
        buttonEdit.setActionType(ActionButton.Action.GET);
        bbDetails.add(buttonEdit);
        ViewURLHelper urlhelp = cloneViewURLHelper();
        if (null != organismId)
        {
            urlhelp.replaceParameter("entityId", organismId);
            urlhelp.setAction("showSamples");

            ActionButton buttonShowGrid = new ActionButton("Show Samples");
            buttonShowGrid.setActionType(ActionButton.Action.LINK);
            buttonShowGrid.setURL(urlhelp.getLocalURIString());
            bb.add(buttonShowGrid);
        }

        rgn.setButtonBar(bbDetails, DataRegion.MODE_DETAILS);

        ButtonBar bbUpdate = new ButtonBar();
        ActionButton buttonSubmit = new ActionButton("updateSample.post", "Submit");
        buttonSubmit.setActionType(ActionButton.Action.POST);
        bbUpdate.add(buttonSubmit);
        rgn.setButtonBar(bbUpdate, DataRegion.MODE_UPDATE);

        return rgn;
    }


    @Jpf.Action
    protected Forward sampleDetails(MouseSampleForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        form.refreshFromDb(false);
        MouseForm mouseForm = new MouseForm();
        mouseForm.reset(null, getRequest());
        mouseForm.setEntityId((String) form.get("organismId"));
        DetailsView detailsView = new DetailsView(getSamplesDefaultRegion(mouseForm.getEntityId()), form);

        VelocityDataView mouseView = new VelocityDataView(getDefaultRegion(mouseForm), mouseForm, "mousedetails.vm");
        mouseView.setMode(DataRegion.MODE_DETAILS);
        mouseView.addObject("caption", "Sample Details");
        mouseView.setView("child", detailsView);

        JspView slidesView = new JspView("/org/labkey/mousemodel/sample/slides.jsp");
        slidesView.setTitle("Slides");
        SimpleFilter filter = new SimpleFilter("sampleId", form.getTypedValue("sampleId"));
        Map[] slides = Table.select(MouseSchema.getMouseSlide(), Table.ALL_COLUMNS, filter, null, Map.class);
        slidesView.addObject("slides", slides);

        NotesView notesView = new NotesView(form.getContainer(), form.getBean().getEntityId());
        VBox box = new VBox(mouseView, slidesView, notesView);

        //InsertView insertView = new InsertView(getDefaultRegion());
        _renderInTemplate(box, form);
        return null;
    }


    @Jpf.Action
    protected Forward showSamples(MouseForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);

        //This may be entered with only a mouseId; if so, select the mouse
        Container c = form.getContainer();
        Mouse mouse = form.getMouse();
        if (null == mouse.getEntityId())
        {
            if (mouse.getMouseNo() != null)
            {
                SimpleFilter filter = new SimpleFilter("mouseNo", mouse.getMouseNo());
                Mouse[] mice = (Mouse[]) Table.select(MouseSchema.getMouse(), Table.ALL_COLUMNS, filter, null, Mouse.class);
                if (null != mice && mice.length == 1)
                {
                    mouse = mice[0];
                    form.setBean(mouse);
                }
            }
        }
        //UNDONE: If couldn't find the mouse assume we are inserting a new one.
        if (null == mouse.getEntityId())
            HttpView.throwNotFound("Couldn't find mouse: " + mouse.getMouseNo());

        GridView gridView = new GridView(getSamplesGridRegion(mouse));
        gridView.setContainer(c);
        gridView.setFilter(new SimpleFilter("organismId", mouse.getEntityId()));

        VelocityDataView mouseView = new VelocityDataView(getDefaultRegion(form), form, "mousedetails.vm");
        mouseView.addObject("caption", "Mouse Samples");
        mouseView.setMode(DataRegion.MODE_DETAILS);
        mouseView.setView("child", gridView);
        _renderInTemplate(mouseView, form);

        return null;
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showInsertSample.do", name = "validate"))
    protected Forward insertSample(MouseSampleForm form) throws Exception
    {
        requiresPermission(ACL.PERM_INSERT);

        Sample sample = form.getBean();

        MouseModel model = MouseModelController.getModel(form);
        MaterialSource source = ExperimentService.get().getMaterialSource(model.getMaterialSourceLSID());
        sample.setLSID(source.getMaterialLSIDPrefix() + sample.getSampleId());
        SampleManager.insert(getUser(), sample);

        lastSampleInserted = form.getTypedValues();
        lastSampleInserted.remove("sampleId");
        lastSampleInserted.remove("sampleTypeId");
        Object organismId = form.getTypedValue("organismId");
        return form.getForward("showSamples", new Pair("entityId", organismId), true);
    }


    @Jpf.Action
    protected Forward showInsertSample(MouseForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_READ);
        InsertView insertView = new InsertView(getSamplesDefaultRegion(form.getEntityId()));
        if (null == lastSampleInserted)
        {
            Map initialValues = new HashMap();
            initialValues.put("organismId", form.getMouse().getEntityId());
            insertView.setInitialValues(initialValues);
        }
        else
            insertView.setInitialValues(lastSampleInserted);

        insertView.setContainer(c);
        VelocityDataView mouseView = new VelocityDataView(getDefaultRegion(form), form, "mousedetails.vm");
        mouseView.addObject("caption", "Insert Sample");
        mouseView.setMode(DataRegion.MODE_DETAILS);
        mouseView.setView("child", insertView);
        //InsertView insertView = new InsertView(getDefaultRegion());
        _renderInTemplate(mouseView, form);

        return null;
    }

    @Jpf.Action
    protected Forward showUpdateSample(MouseSampleForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);

        form.refreshFromDb(false);
        UpdateView updateView = new UpdateView(getSamplesDefaultRegion((String) form.get("organismId")), form);

        MouseForm mouseForm = new MouseForm();
        mouseForm.reset(null, getRequest());
        mouseForm.setEntityId((String) form.get("organismId"));
        VelocityDataView mouseView = new VelocityDataView(getDefaultRegion(mouseForm), mouseForm, "mousedetails.vm");
        mouseView.setMode(DataRegion.MODE_DETAILS);
        mouseView.addObject("caption", "Update Samples");
        mouseView.setView("child", updateView);
        //InsertView insertView = new InsertView(getDefaultRegion());
        _renderInTemplate(mouseView, form);

        return null;
    }

    @Jpf.Action(validationErrorForward = @Jpf.Forward(path = "showUpdateSample.do", name = "validate"))
    protected Forward updateSample(MouseSampleForm form) throws Exception
    {
        //redundant, but defensive
        requiresPermission(ACL.PERM_UPDATE);

        form.doUpdate();
        return form.getPkForward("sampleDetails");
    }


    private static DisplayColumn[] getCustomColumns(MouseModel model)
    {
        Map customCols = model.getExtraColumnCaptions();
        if (null == customCols)
            return new DisplayColumn[0];

        String [] colNames = (String[]) customCols.keySet().toArray(new String[customCols.size()]);

        DataColumn[] cols = MouseSchema.getMouseView().getDisplayColumns(colNames);
        for (int i = 0; i < cols.length; i++)
            cols[i].setCaption((String) customCols.get(colNames[i]));

        return cols;
    }


    private static TableInfo getSampleTable()
    {
        return MouseSchema.getSample();
    }


    private void _renderInTemplate(HttpView view, TableViewForm form) throws Exception
    {
        MouseModelTemplateView tv = new MouseModelTemplateView(MouseModelController.getModelId(form), 2, view);
        HomeTemplate template = new HomeTemplate(getViewContext(), getContainer(), tv);
        includeView(template);
    }


    public static class MouseSampleForm extends BeanViewForm
    {
        public MouseSampleForm()
        {
            super(Sample.class, MouseSchema.getSample(), new String[]{"modelId", "materialSourceLSID"});
        }

        public Sample getBean()
        {
            return (Sample) super.getBean();
        }
    }

    /**
     * FormData get and set methods may be overwritten by the Form Bean editor.
     */
    public static class MouseForm extends BeanViewForm
    {
        public MouseForm()
        {
            super(Mouse.class, MouseSchema.getMouse());
        }

        public Mouse getMouse()
        {
            return (Mouse) getBean();
        }

        public String getEntityId()
        {
            return (String) get("entityId");
        }

        public void setEntityId(String entityId)
        {
            set("entityId", entityId);
        }


    }

    public static class BulkMouseForm extends MouseForm
    {
        //Validation needs to be done differently since we ignore nulls
        protected void populateValues(ActionErrors errors)
        {
            Map values = new HashMap();
            Set keys = _stringValues.keySet();
            Iterator iter = keys.iterator();
            while (iter.hasNext())
            {
                String propName = (String) iter.next();
                String str = (String) _stringValues.get(propName);
                if (null != str && "".equals(str.trim()))
                    str = null;

                try
                {
                    if (null != str)
                    {
                        Object val = ConvertUtils.convert(str, _dynaClass.getTruePropType(propName));
                        values.put(propName, val);
                    }
                }
                catch (ConversionException e)
                {
                    errors.add(propName, new ActionError("ConversionError", _dynaClass.getPropertyCaption(propName)));
                }
            }
            _values = values;
        }

    }

    public static class MouseSampleView extends WebPartView
    {
        static DataRegion _region;
        private Container _c;

        public DataRegion getDataRegion()
        {
            if (null == _region)
            {
                DataRegion dr = new DataRegion();
                dr.setColumns(getSampleTable().getColumns("SampleId,SampleTypeId,TissComment,OrganismId"));
                dr.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
                _region = dr;
            }
            return _region;
        }

        public MouseSampleView()
        {
            setTitle("Samples");
        }

        @Override
        public void prepareWebPart(Object model)
        {
            if (null == _c)
            {
                String path = (String) getViewContext().get("path");
                if (null == path)
                    path = getViewContext().getViewURLHelper().getExtraPath();
                _c = ContainerManager.getForPath(path);
            }

            if (null == _c || !_c.hasPermission(getViewContext().getUser(), ACL.PERM_READ))
            {
                setVisible(false);
                return;
            }

            if (_c.hasPermission(getViewContext().getUser(), ACL.PERM_UPDATE))
                setTitleHref(ViewURLHelper.toPathString("Mouse", "showSamples", _c.getPath()));
        }


        @Override
        public void renderView(Object model, PrintWriter out) throws Exception
        {
            try
            {
                getViewContext().getContainer(ACL.PERM_READ);
                getDataRegion().renderTable(getViewContext(), out, null, null);
            }
            catch (SQLException x)
            {
                out.print(x);
            }
        }
    }


    public static class MouseView extends WebPartView
    {
        static DataRegion _region;
        private Container _c;

        public DataRegion getDataRegion()
        {
            if (null == _region)
            {
                DataRegion dr = new DataRegion();
                dr.setColumns(MouseSchema.getMouse().getColumns("MouseNo,modelId,Sex,MouseComments"));
                dr.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
                _region = dr;
            }
            return _region;
        }

        public MouseView()
        {
            setTitle("Mice");
        }

        @Override
        public void prepareWebPart(Object model)
                throws ServletException
        {
            if (null == _c)
            {
                String path = (String) getViewContext().get("path");
                if (null == path)
                    path = getViewContext().getViewURLHelper().getExtraPath();
                _c = ContainerManager.getForPath(path);
            }

            if (null == _c || !_c.hasPermission(getViewContext().getUser(), ACL.PERM_READ))
            {
                setVisible(false);
                return;
            }
            ViewURLHelper urlhelp = getViewContext().cloneViewURLHelper();
            urlhelp.setAction(null);
            urlhelp.setPageFlow("Mouse");
            urlhelp.deleteParameters();
            getDataRegion().setPageFlowUrl(urlhelp.getURIString());
            if (_c.hasPermission(getViewContext().getUser(), ACL.PERM_UPDATE))
                setTitleHref(ViewURLHelper.toPathString("Mouse", "begin", _c.getPath()));
        }


        @Override
        public void renderView(Object model, PrintWriter out) throws Exception
        {
            try
            {
                getViewContext().getContainer(ACL.PERM_READ);
                getDataRegion().renderTable(getViewContext(), out, null, null);
            }
            catch (SQLException x)
            {
                out.print(x);
            }
        }
    }

    
    public static class TrueFalseColumn extends DataColumn
    {
        private final String strTrue;
        private final String strFalse;
        public TrueFalseColumn(ColumnInfo col, String strTrue, String strFalse)
        {
            super(col);
            this.strTrue = strTrue;
            this.strFalse = strFalse;
        }


        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            out.write(getValue(ctx));
        }

        public String getValue(RenderContext ctx)
        {
            Boolean val = (Boolean) super.getValue(ctx);
            return val ? strTrue : strFalse;
        }
    }

    public static class ControlColumn extends TrueFalseColumn
    {
        public ControlColumn(TableInfo tableInfo)
        {
            super(tableInfo.getColumn("Control"), "Control", "Tumor Bearing");
        }
    }

}

