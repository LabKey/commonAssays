package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpObject;
import org.labkey.api.exp.property.SystemProperty;
import org.labkey.api.security.User;
import org.labkey.api.util.AppProps;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.flow.controllers.FlowParam;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

abstract public class FlowObject<T extends ExpObject> implements Comparable<Object>, Serializable
{
    static public final String PROP_CATEGORY = "flow";
    protected T _expObject;
    public FlowObject(T expObject)
    {
        _expObject = expObject;
    }

    public T getExpObject()
    {
        return _expObject;
    }
    abstract public FlowObject getParent();
    abstract public void addParams(Map<FlowParam, Object> map);
    abstract public ActionURL urlShow();
    public String getLSID()
    {
        return _expObject.getLSID();
    }

    public String getContainerPath()
    {
        return _expObject.getContainer().getPath();
    }

    public Container getContainer()
    {
        return _expObject.getContainer();
    }

    public String getContainerId()
    {
        return _expObject.getContainer().getId();
    }

    protected ActionURL pfURL(Enum action)
    {
        return PageFlowUtil.urlFor(action, getContainerPath());
    }

    public ActionURL urlFor(Enum action)
    {
        return addParams(pfURL(action));
    }

    final public ActionURL addParams(ActionURL url)
    {
        EnumMap<FlowParam, Object> map = new EnumMap(FlowParam.class);
        addParams(map);
        for (Map.Entry<FlowParam,Object> param : map.entrySet())
        {
            url.replaceParameter(param.getKey().toString(), param.getValue().toString());
        }
        return url;
    }

    final public Map<FlowParam,Object> getParams()
    {
        Map<FlowParam,Object> ret = new EnumMap(FlowParam.class);
        addParams(ret);
        return ret;
    }

    public String getLabel()
    {
        return getName();
    }
    public Object getId()
    {
        Map<FlowParam, Object> params = getParams();
        if (params.size() != 1)
            throw new UnsupportedOperationException();
        return params.values().iterator().next();
    }

    public void checkContainer(ActionURL url) throws ServletException
    {
        if (!getContainerPath().equals(url.getExtraPath()))
            HttpView.throwNotFound("Wrong container");
    }

    static public String getParam(ActionURL url, HttpServletRequest request, FlowParam param)
    {
        String ret = url.getParameter(param.toString());
        if (ret != null)
        {
            return ret;
        }
        if (request != null)
        {
            return request.getParameter(param.toString());
        }
        return null;
    }

    static public int getIntParam(ActionURL url, HttpServletRequest request, FlowParam param)
    {
        String str = getParam(url, request, param);
        if (str == null || str.length() == 0)
            return 0;
        return Integer.valueOf(str);
    }

    public void addHiddenFields(DataRegion region)
    {
        for (Map.Entry<FlowParam, Object> param : getParams().entrySet())
        {
            region.addHiddenFormField(param.getKey().toString(), param.getValue().toString());
        }
    }

    static public <T extends FlowObject> String strSelect(String name, T current, Collection<T> objects)
    {
        return PageFlowUtil.strSelect(name, idLabelsFor(objects, ""), current == null ? null : current.getId());
    }

    static public Map<Object,String> idLabelsFor(Collection<? extends FlowObject> list, String nullLabel)
    {
        Map<Object,String> ret = new LinkedHashMap();
        for (FlowObject obj : list)
        {
            Object id = obj == null ? null : obj.getId();
            String label = obj == null ? nullLabel : obj.getLabel();
            ret.put(id, label);
        }
        return ret;
    }
    static public Map<Object,String> idLabelsFor(Collection<? extends FlowObject> list)
    {
        return idLabelsFor(list, "");
    }

    public String getOwnerObjectLSID()
    {
        return getLSID();
    }

    public Object getProperty(SystemProperty property)
    {
        return getExpObject().getProperty(property.getPropertyDescriptor());
    }

    public Object getProperty(PropertyDescriptor pd) throws SQLException
    {
        if (pd == null)
            return null;
        Map<String, Object> props = OntologyManager.getProperties(getContainerId(), getLSID());
        return props.get(pd.getPropertyURI());
    }

    public FlowLog getLog(LogType type) throws SQLException
    {
        return null;
    }

    public void logPropertyChange(User user, String propertyURI, Object oldValue, Object newValue) throws SQLException
    {
        FlowLog log = getLog(LogType.changes);
        if (log == null)
        {
            return;
        }
        EnumMap values = new EnumMap(LogField.class);
        values.put(LogField.date, new Date());
        values.put(LogField.user, user);
        values.put(LogField.objectURI, getLSID());
        values.put(LogField.propertyURI, propertyURI);
        values.put(LogField.oldValue, oldValue);
        values.put(LogField.newValue, newValue);
        log.append(values);
    }

    public void setProperty(User user, PropertyDescriptor pd, Object value) throws SQLException
    {
        try
        {
            getExpObject().setProperty(user, pd, value);
        }
        catch (SQLException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public String getName()
    {
        return _expObject.getName();
    }

    static public String generateLSID(Container container, String type, String name)
    {
        String str = "urn:lsid:" + AppProps.getInstance().getDefaultLsidAuthority() + ":" + type + ".Folder-" + container.getRowId() + ":" + name;
        return new Lsid(str).toString();
    }

    static public String generateLSID(Container container, DataType type, String name)
    {
        return generateLSID(container, type.getNamespacePrefix(), name);
    }

    static public String generateUniqueLSID(String type)
    {
        String str = "urn:lsid:" + AppProps.getInstance().getDefaultLsidAuthority() + ":" + type + ":" + GUID.makeGUID();
        return new Lsid(str).toString();
    }

    public Container getContainerObject()
    {
        return ContainerManager.getForId(getContainerId());
    }

    public int compareTo(Object o)
    {
        if (!(o instanceof FlowObject))
            return 0;
        return getName().compareTo(((FlowObject) o).getName());
    }
}
