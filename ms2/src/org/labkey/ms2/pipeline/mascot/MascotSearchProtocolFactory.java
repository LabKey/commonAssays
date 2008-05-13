/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.pipeline.ParamParser;
import org.labkey.api.util.AppProps;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocol;

import java.io.File;

/**
 * MascotSearchProtocolFactory class
 * <p/>
 * Created: Jun 6, 2006
 *
 * @author bmaclean
 */
public class MascotSearchProtocolFactory extends AbstractMS2SearchProtocolFactory
{
    public static MascotSearchProtocolFactory instance = new MascotSearchProtocolFactory();

    public static MascotSearchProtocolFactory get()
    {
        return instance;
    }

    private MascotSearchProtocolFactory()
    {
        // Use the get() function.
    }

    public String getName()
    {
        return "mascot";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/mascot/MascotDefaults.xml";
    }

    public MascotSearchProtocol createProtocolInstance(String name, String description, String xml)
    {
        AppProps appProps = AppProps.getInstance();
        String mascotServer = appProps.getMascotServer();
        String mascotHTTPProxy = appProps.getMascotHTTPProxy();
        if (!appProps.hasMascotServer() || 0 == mascotServer.length())
            throw new IllegalArgumentException("Mascot server has not been specified in site customization.");

        MascotSearchProtocol protocol = new MascotSearchProtocol(name, description, xml);

        protocol.setMascotServer(mascotServer);
        protocol.setMascotHTTPProxy(mascotHTTPProxy);
        return protocol;
    }

    protected AbstractMS2SearchProtocol createProtocolInstance(ParamParser parser)
    {
        String mascotServer = parser.removeInputParameter("pipeline, mascot server");
        String mascotHTTPProxy = parser.removeInputParameter("pipeline, mascot http proxy");

        MascotSearchProtocol instance = (MascotSearchProtocol) super.createProtocolInstance(parser);

        instance.setMascotServer(mascotServer);
        instance.setMascotHTTPProxy(mascotHTTPProxy);
        return instance;
    }
}
