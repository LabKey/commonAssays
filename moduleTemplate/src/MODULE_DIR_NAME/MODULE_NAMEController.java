package @@MODULE_LOWERCASE_NAME@@;

import cpas.@@MODULE_LOWERCASE_NAME@@.@@MODULE_NAME@@Manager;
import cpas.@@MODULE_LOWERCASE_NAME@@.@@MODULE_NAME@@Schema;
import org.apache.beehive.netui.pageflow.FormData;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.fhcrc.cpas.data.*;
import org.fhcrc.cpas.security.ACL;
import org.fhcrc.cpas.view.*;
import org.fhcrc.cpas.util.PageFlowUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class @@MODULE_NAME@@Controller extends ViewController
{
    static Logger _log = Logger.getLogger(@@MODULE_NAME@@Controller.class);

    /**
     * This method represents the point of entry into the pageflow
     */
    @Jpf.Action
    protected Forward begin() throws Exception
    {
        JspView v = new JspView("/cpas/@@MODULE_LOWERCASE_NAME@@/view/hello.jsp");
        return renderInTemplate(v);
    }

    private Forward renderInTemplate(HttpView view) throws Exception
    {
        HttpView template = new HomeTemplate(getViewContext(), getContainer(), view);
        includeView(template);
        return null;
    }
}