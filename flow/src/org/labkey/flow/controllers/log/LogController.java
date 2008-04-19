package org.labkey.flow.controllers.log;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowLog;
import org.labkey.flow.data.FlowObject;
import org.labkey.flow.data.LogField;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

public class LogController extends SpringFlowController<LogController.Action>
{
    public enum Action
    {
        begin,
        showLog,
    }


    static DefaultActionResolver _actionResolver = new DefaultActionResolver(LogController.class);

    public LogController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    @RequiresPermission(ACL.PERM_NONE)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(FlowController.Action.begin));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }


    @RequiresPermission(ACL.PERM_READ)
    public class ShowLogAction extends SimpleViewAction
    {
        Page _page;

        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            _page = (Page)getFlowPage("showLog.jsp");
            _page.log = getLog();
            if (_page.log == null)
                HttpView.throwNotFound();
            return new JspView(_page);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return appendFlowNavTrail(root, _page.log, "Log " + _page.log.getName(), Action.showLog);
        }
    }


    public FlowLog getLog() throws SQLException
    {
        return FlowLog.fromLogId(getIntParam(FlowParam.logId));
    }


    static public abstract class Page extends FlowPage<LogController>
    {
        public FlowLog log;
        private String lsidLast;
        private FlowObject dataObjectLast;

        public String formatValue(int index, String[] headers, String[] values)
        {
            if (index > values.length)
                return "";
            String value = values[index];
            if (index > headers.length)
                return h(value);
            String header = headers[index];
            try
            {
                LogField field = LogField.valueOf(header);
                switch (field)
                {
                    case objectURI:
                        FlowObject dataObj;
                        if (StringUtils.equals(value, lsidLast))
                        {
                            dataObj = dataObjectLast;
                        }
                        else
                        {
                            dataObjectLast = dataObj = FlowDataObject.fromLSID(value);
                            lsidLast = value;
                        }

                        if (dataObj != null)
                        {
                            return textLink(dataObj.getName(), dataObj.urlShow().toString());
                        }
                        break;
                }
            }
            catch (Exception e)
            {
                Logger.getLogger(LogController.class).error("unexpected exception", e);
            }
            return h(value);
        }
    }
}
