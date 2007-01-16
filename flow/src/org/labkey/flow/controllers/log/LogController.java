package org.labkey.flow.controllers.log;

import org.labkey.flow.controllers.BaseFlowController;
import org.labkey.flow.controllers.FlowController;
import org.labkey.flow.controllers.FlowParam;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.view.ViewForward;
import org.labkey.api.view.HomeTemplate;
import org.labkey.api.view.JspView;
import org.labkey.flow.data.*;

import java.sql.SQLException;

@Jpf.Controller
public class LogController extends BaseFlowController<LogController.Action>
{
    public enum Action
    {
        begin,
        showLog,
    }

    @Jpf.Action
    protected Forward begin() throws Exception
    {
        return new ViewForward(urlFor(FlowController.Action.begin));
    }

    @Jpf.Action
    protected Forward showLog() throws Exception
    {
        Page page = getPage("showLog.jsp");
        return includeView(new HomeTemplate(getViewContext(), new JspView(page), getNavTrailConfig(page.log, "Log " + page.log.getName(), Action.showLog)));
    }

    protected Page getPage(String name) throws Exception
    {
        Page ret = (Page) getFlowPage(name);
        ret.log = getLog();
        return ret;
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
                e = e;
            }
            return h(value);
        }
    }
}
