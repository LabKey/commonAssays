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
package org.labkey.ms2.protocol;

import org.labkey.api.util.AppProps;
import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.MascotInputParser;

import java.io.File;

/**
 * MascotSearchProtocolFactory class
 * <p/>
 * Created: Jun 6, 2006
 *
 * @author bmaclean
 */
public class MascotSearchProtocolFactory extends AbstractMS2SearchProtocolFactory<MascotSearchProtocol>
{
    public static MascotSearchProtocolFactory instance = new MascotSearchProtocolFactory();

    public static MascotSearchProtocolFactory get()
    {
        return instance;
    }

    public String getName()
    {
        return "mascot";
    }

    public String getDefaultParametersResource()
    {
        return "org/labkey/ms2/pipeline/MascotDefaults.xml";
    }

    public BioMLInputParser createInputParser()
    {
        return new MascotInputParser();
    }

    public MascotSearchProtocol createProtocolInstance(String name, String description, String[] dbNames, String xml)
    {
        return new MascotSearchProtocol(name, description, dbNames, xml);
    }

    public MascotSearchProtocol createProtocolInstance(String name, String description, File dirSeqRoot,
                                                       String dbPath, String[] dbNames, String xml)
    {
        AppProps appProps = AppProps.getInstance();
        String mascotServer = appProps.getMascotServer();
        String mascotHTTPProxy = appProps.getMascotHTTPProxy();
        if (!appProps.hasMascotServer() || 0 == mascotServer.length())
            throw new IllegalArgumentException("Mascot server has not been specified in site customization.");

        MascotSearchProtocol protocol = createProtocolInstance(name,
                description, dbNames, xml);

        protocol.setMascotServer(mascotServer);
        protocol.setMascotHTTPProxy(mascotHTTPProxy);
        return protocol;
    }
}
