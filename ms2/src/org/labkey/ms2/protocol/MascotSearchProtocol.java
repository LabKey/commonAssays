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

import org.apache.log4j.Logger;
import org.labkey.ms2.pipeline.BioMLInputParser;
import org.labkey.ms2.pipeline.MascotInputParser;
import org.labkey.api.pipeline.PipelineProtocolFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MascotSearchProtocol class
 * <p/>
 * Created: Jun 6, 2006
 *
 * @author bmaclean
 */
public class MascotSearchProtocol extends MS2SearchPipelineProtocol
{
    private static Logger _log = Logger.getLogger(MascotSearchProtocol.class);

    private String mascotServer;
    private String mascotHTTPProxy;

    public MascotSearchProtocol(String name, String description, String[] dbNames, String xml)
    {
        super(name, description, dbNames, xml);
    }

    protected BioMLInputParser createInputParser()
    {
        return new MascotInputParser();
    }

    public String getMascotServer ()
    {
        return mascotServer;
    }

    public void setMascotServer (String mascotServer)
    {
        this.mascotServer = mascotServer;
    }

    public String getMascotHTTPProxy ()
    {
        return mascotHTTPProxy;
    }

    public void setMascotHTTPProxy (String mascotHTTPProxy)
    {
        this.mascotHTTPProxy = mascotHTTPProxy;
    }

    public PipelineProtocolFactory getFactory()
    {
        return MascotSearchProtocolFactory.get();
    }

    public void saveInstance(File file) throws IOException
    {
        Map<String, String> addParams = new HashMap<String, String>();
        addParams.put("pipeline, email address", email);
        addParams.put("pipeline, mascot server", mascotServer);
        addParams.put("pipeline, mascot http proxy", mascotHTTPProxy);
        save(file, addParams);
    }
}
