/*
 * Copyright (c) 2006-2016 LabKey Corporation
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
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.GUID;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.flow.controllers.FlowParam;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

abstract public class FlowObject<T extends ExpObject> implements Comparable<Object>, Serializable
{
    protected T _expObject;
    protected String _entityId;

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
    abstract public ActionURL urlDownload();

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

    public ActionURL urlFor(Class<? extends Controller> actionClass)
    {
        return addParams(new ActionURL(actionClass, getContainer()));
    }

    final public ActionURL addParams(ActionURL url)
    {
        EnumMap<FlowParam, Object> map = new EnumMap<>(FlowParam.class);
        addParams(map);

        for (Map.Entry<FlowParam,Object> param : map.entrySet())
        {
            url.replaceParameter(param.getKey().toString(), param.getValue().toString());
        }
        return url;
    }


    final public Map<FlowParam, Object> getParams()
    {
        Map<FlowParam, Object> ret = new EnumMap<>(FlowParam.class);
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


    public void checkContainer(Container actionContainer, User user, ActionURL actionURL) throws NotFoundException
    {
        if (!getContainer().equals(actionContainer))
        {
            if (getContainer().hasPermission(user, ReadPermission.class))
                throw new RedirectException(actionURL.clone().setContainer(getContainer()));
            else
                throw new NotFoundException("Flow object does not exist in this folder");
        }
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
        try
        {
            return Integer.valueOf(str);
        }
        catch (NumberFormatException nfe)
        {
            return 0;
        }
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

    public Object getProperty(PropertyDescriptor pd)
    {
        if (pd == null)
            return null;
        Map<String, Object> props = OntologyManager.getProperties(getContainer(), getLSID());
        return props.get(pd.getPropertyURI());
    }

    public void setProperty(User user, PropertyDescriptor pd, Object value) throws SQLException
    {
        try
        {
            getExpObject().setProperty(user, pd, value);
        }
        catch (ValidationException e)
        {
            throw (SQLException)new SQLException().initCause(e);
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
        return new Lsid(type, "Folder-" + container.getRowId(), name).toString();
    }

    static public String generateLSID(Container container, DataType type, String name)
    {
        return generateLSID(container, type.getNamespacePrefix(), name);
    }

    static public String generateUniqueLSID(String type)
    {
        return new Lsid(type, GUID.makeGUID()).toString();
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

    public void setEntityId(String entityId)
    {
        _entityId = entityId;    
    }

    public String getEntityId()
    {
        return _entityId;
    }
}
