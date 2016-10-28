/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.mascot;

import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.settings.AbstractWriteableSettingsGroup;

/**
 * Created by: jeckels
 * Date: 2/7/16
 */
public class MascotConfig extends AbstractWriteableSettingsGroup
{
    protected static final String MASCOT_SERVER_PROP = "MascotServer";
    protected static final String MASCOT_USERACCOUNT_PROP = "MascotUserAccount";
    protected static final String MASCOT_USERPASSWORD_PROP = "MascotUserPassword";
    protected static final String MASCOT_HTTPPROXY_PROP = "MascotHTTPProxy";

    protected static final String GROUP_NAME = "MascotConfig";

    private final Container _container;

    public MascotConfig(Container container)
    {
        _container = container;
    }

    @Override
    protected String getType()
    {
        return "Mascot configuration";
    }

    @Override
    protected String getGroupName()
    {
        return GROUP_NAME;
    }

    public boolean hasMascotServer()
    {
        return !"".equals(getMascotServer());
    }

    public String getMascotServer()
    {
        return lookupStringValue(_container, MASCOT_SERVER_PROP, "");
    }

    public String getMascotUserAccount()
    {
        return lookupStringValue(_container, MASCOT_USERACCOUNT_PROP, "");
    }

    public String getMascotUserPassword()
    {
        return lookupStringValue(_container, MASCOT_USERPASSWORD_PROP, "");
    }

    public String getMascotHTTPProxy()
    {
        return lookupStringValue(_container, MASCOT_HTTPPROXY_PROP, "");
    }


    public void setMascotServer(String mascotServer)
    {
        storeStringValue(MASCOT_SERVER_PROP, mascotServer);
    }

    public void setMascotUserAccount(String mascotUserAccount)
    {
        storeStringValue(MASCOT_USERACCOUNT_PROP, mascotUserAccount);
    }

    public void setMascotUserPassword(String mascotUserPassword)
    {
        storeStringValue(MASCOT_USERPASSWORD_PROP, mascotUserPassword);
    }

    public void setMascotHTTPProxy(String mascotHTTPProxy)
    {
        storeStringValue(MASCOT_HTTPPROXY_PROP, mascotHTTPProxy);
    }

    public void save()
    {
        super.save();
    }

    public Container getContainer()
    {
        return _container;
    }

    @Override
    protected boolean isPasswordProperty(String propName)
    {
        return super.isPasswordProperty(propName) || MASCOT_USERPASSWORD_PROP.equals(propName);
    }

    public static void reset(Container container)
    {
        PropertyManager.PropertyMap props = PropertyManager.getProperties(SITE_CONFIG_USER, container, GROUP_NAME);
        props.delete();
    }

    public static MascotConfig getWriteableMascotConfig(Container container)
    {
        MascotConfig result = new MascotConfig(container);
        result.makeWriteable(container);
        return result;
    }

    public static MascotConfig findMascotConfig(Container container)
    {
        while (!container.isRoot())
        {
            PropertyManager.PropertyMap props = PropertyManager.getProperties(SITE_CONFIG_USER, container, GROUP_NAME);
            if (!props.isEmpty())
            {
                break;
            }
            container = container.getParent();
        }
        return new MascotConfig(container);
    }
}
