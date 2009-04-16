package org.labkey.ms2.xarassay;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.view.JspView;

import java.io.IOException;
import java.io.Writer;

/**
 * User: jeckels
 * Date: Apr 14, 2009
 */
public class FractionsDisplayColumn extends SimpleDisplayColumn
{
    private XarAssayForm _form;
    private ColumnInfo _col;
    public static final String FRACTIONS_FIELD_NAME = "__fractions";

    public FractionsDisplayColumn(XarAssayForm form)
    {
        _form = form;
        setCaption("Fractions");

        _col = new ColumnInfo("Fractions");
        _col.setInputType("file");
        setWidth("100%");
    }

    public boolean isEditable()
    {
        return true;
    }

    public ColumnInfo getColumnInfo()
    {
        return _col;
    }

    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        JspView<XarAssayForm> view = new JspView<XarAssayForm>("/org/labkey/ms2/xarassay/fractionsInput.jsp", _form);
        try
        {
            view.render(ctx.getRequest(), ctx.getViewContext().getResponse());
        }
        catch (Exception e)
        {
            throw (IOException)new IOException().initCause(e);
        }
    }
}
