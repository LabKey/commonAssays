package org.labkey.flow.webparts;

import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ViewURLHelper;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;

public class Overview
{
    static protected class Step
    {
        protected enum Status
        {
            undone,
            optional,
            disabled,
            completed,
        }

        String _title;
        Status _status;
        String _expanatoryHTML;
        String _statusHTML;
        List<Action> _actions;
        public Step(String title, Status status)
        {
            _title = title;
            _status = status;
            _actions = new ArrayList();
        }

        public void setExplanatoryHTML(String html)
        {
            _expanatoryHTML = html;
        }

        public void setStatusHTML(String html)
        {
            _statusHTML = html;
        }

        public void addAction(Action action)
        {
            if (action == null)
                return;
            _actions.add(action);
        }

        public String toString()
        {
            StringBuilder ret = new StringBuilder("<li");
            if (_status == Status.disabled)
            {
                ret.append(" class=\"step-disabled\"");
            }
            ret.append(">");
            ret.append("<b>");
            ret.append(h(_title));
            ret.append("</b>");
            if (_status == Status.optional)
            {
                ret.append(" (optional)");
            }
            if (_status == Status.completed)
            {
                //ret.append(" (completed)");
            }
            if (_status == Status.undone)
            {
                ret.append(" (required)");
            }
            if (!StringUtils.isEmpty(_expanatoryHTML))
            {
                ret.append("<br><i>");
                ret.append(_expanatoryHTML);
                ret.append("</i>");
            }
            if (!(StringUtils.isEmpty(_statusHTML)))
            {
                ret.append("<br>");
                ret.append(_statusHTML);
            }
            for (Action action : _actions)
            {
                ret.append("<br>\n");
                ret.append(action);
            }
            ret.append("</li>");
            return ret.toString();
        }
    }

    static protected class Action
    {
        String _label;
        ViewURLHelper _url;
        String _descriptionHTML;
        public Action(String label, ViewURLHelper url)
        {
            _label = label;
            _url = url;
        }

        public void setDescriptionHTML(String html)
        {
            _descriptionHTML = html;
        }
        public String toString()
        {
            StringBuilder ret = new StringBuilder();
            if (_descriptionHTML != null)
            {
                ret.append(_descriptionHTML);
                ret.append("<br>");
            }
            if (_url != null)
            {
                ret.append("[<a href=\"" + h(_url) + "\">" + h(_label) + "</a>]");
            }
            else
            {
                ret.append(_label);
            }
            return ret.toString();
        }
    }

    protected String formatAction(String label, ViewURLHelper action)
    {
        return formatAction(label, action, null);
    }

    protected String formatAction(String label, ViewURLHelper action, String description)
    {
        StringBuilder ret = new StringBuilder();
        if (description != null)
        {
            ret.append(description);
            ret.append("<br>\n");
        }
        ret.append("[<a href=\"" + h(action) + "\">" + h(label) + "</a>");
        return ret.toString();
    }

    static protected String h(Object text)
    {
        return PageFlowUtil.filter(text);
    }

    protected String formatOverview(String title, String description, List<Step> steps, List<Action> miscActions)
    {
        StringBuilder ret = new StringBuilder();
        ret.append("<style>.step-disabled, .step-disabled a:link, .step-disabled a:visited { color: silver; }</style>");
        ret.append("<b>");
        ret.append(h(title));
        ret.append("</b><br>\n");
        if (description != null)
        {
            ret.append(description);
            ret.append("<br>");
        }
        if (steps.size() != 0)
        {
            ret.append("<ol>");
            for (Step step : steps)
            {
                ret.append(step);
            }
            ret.append("</ol>");
        }
        for (Action miscAction : miscActions)
        {
            ret.append(miscAction);
            ret.append("<br>");
        }
        return ret.toString();
    }
}
