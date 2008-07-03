/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.ms2.pipeline;

import org.labkey.ms2.pipeline.client.SearchService;
import org.labkey.ms2.pipeline.client.GWTSearchServiceResult;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.util.URIUtil;
import org.apache.log4j.Logger;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;


/**
 * User: Bill
 * Date: Jan 29, 2008
 * Time: 4:04:45 PM
 */
public class SearchServiceImpl extends BaseRemoteService implements SearchService
{

    private static Logger _log = Logger.getLogger(SearchServiceImpl.class);
    private GWTSearchServiceResult results= new GWTSearchServiceResult();
    private AbstractMS2SearchPipelineProvider provider;
    private AbstractMS2SearchProtocol protocol;


    public SearchServiceImpl(ViewContext context)
    {
        super(context);
    }

    public GWTSearchServiceResult getSearchServiceResult(String searchEngine, String dirSequenceRoot,
                                                         String dirRoot,String path)
    {
        provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        getProtocols(dirRoot, "",dirSequenceRoot, searchEngine, path);
        if(results.getSelectedProtocol() == null || results.getSelectedProtocol().equals("") )
            getSequenceDbs(results.getDefaultSequenceDb(), dirSequenceRoot, searchEngine, false);
        getMascotTaxonomy(searchEngine);
        getEnzymes(searchEngine);
        getResidueMods(searchEngine);
        return results;
    }

    public GWTSearchServiceResult getProtocol(String searchEngine, String protocolName, String dirRoot,
                                              String dirSequenceRoot, String path)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.setSelectedProtocol("Loading Error");
                _log.error("Problem loading protocols: provider equals null");
                results.appendError("Problem loading protocol: provider equals null\n");
            }
        }
        if(protocolName == null || protocolName.length() == 0)
        {
            protocolName = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(protocolName == null || protocolName.length() == 0)
                protocolName = "new";
        }
        if(protocolName.equals("new"))
        {
            results.setSelectedProtocol("");
            getMzXml("",path, searchEngine);
            return results;
        }
        URI  uriRoot = new File(dirRoot).toURI();

        AbstractMS2SearchProtocolFactory protocolFactory = provider.getProtocolFactory();
        try
        {
            if(protocol == null)
                protocol = protocolFactory.load(uriRoot, protocolName);
        }
        catch(IOException e)
        {
            results.setSelectedProtocol("Loading Error");
            _log.error("Problem loading protocols: provider equals null");
            results.appendError("Problem loading protocol\n" + e.getMessage());
        }
        results.setSelectedProtocol(protocol.getName());
        try
        {
            results.setDefaultSequenceDb(protocol.getDbNames()[0]);
            if(!provider.dbExists(dirSequenceRoot,protocol.getDbNames()[0]))
                results.appendError("The database " + protocol.getDbNames()[0] + " cannot be found.");
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            _log.error("Problem loading protocol: no database in protocol");
            results.appendError("Problem loading protocol: No database in protocol");
        }

        PipelineService.get().rememberLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                    getUser(),"",results.getDefaultSequenceDb());
        results.setProtocolDescription(protocol.getDescription());
        results.setProtocolXml(protocol.getXml());
        getMzXml(results.getSelectedProtocol(),path, searchEngine);
        return results;
    }

    public GWTSearchServiceResult getMascotTaxonomy(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading taxonomy: provider equals null\n");
            }
        }
        try
        {
            results.setMascotTaxonomyList(provider.getTaxonomyList());
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving taxonomy list: " + e.getMessage());
        }
        return results;
    }

    public GWTSearchServiceResult getEnzymes(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading enzymes: provider equals null\n");
            }
        }
        try
        {
            results.setEnzymeMap(provider.getEnzymes());
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving enzyme list: " + e.getMessage());
        }
        return results;
    }

    public GWTSearchServiceResult getResidueMods(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading residu modifications: provider equals null\n");
            }
        }
        try
        {
            results.setMod0Map(provider.getResidue0Mods());
            results.setMod1Map(provider.getResidue1Mods());
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving residue mods list: " + e.getMessage());
        }
        return results;

    }

    private void getProtocols(String dirRoot, String defaultProtocol, String dirSequenceRoot, String searchEngine,
                              String path)
    {
        ArrayList protocolList = new ArrayList();
        if(defaultProtocol == null || defaultProtocol.length() == 0 )
        {
            if(provider == null)
            {
                provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            }
            defaultProtocol = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(defaultProtocol == null) defaultProtocol = "";
        }
        getProtocol(searchEngine,defaultProtocol,dirRoot, dirSequenceRoot, path);

        String[] protocols;
        URI  dirRootURI = new File(dirRoot).toURI();

        protocols = provider.getProtocolFactory().getProtocolNames(dirRootURI);
        for(String protName:protocols)
        {
            if(!protName.equals("default"))
                protocolList.add(protName);
        }
        results.setProtocols(protocolList);
    }

    public void getSequenceDbPaths(String dirSequenceRoot, String searchEngine)
    {
        getSequenceDbPaths(dirSequenceRoot, searchEngine, false);
    }

    private void getSequenceDbPaths(String dirSequenceRoot, String searchEngine, boolean refresh)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }
        if(!provider.supportsDirectories()) return;

        List<String> sequenceDbPaths;
        sequenceDbPaths = PipelineService.get().getLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                getContainer(),getUser());
        if(sequenceDbPaths == null || sequenceDbPaths.size() == 0 || refresh)
        {
            URI dirSequenceRootURI;
            try
            {
                dirSequenceRootURI = new File(dirSequenceRoot).toURI();
                sequenceDbPaths =  provider.getSequenceDbPaths(dirSequenceRootURI);
                if(sequenceDbPaths == null) throw new IOException("Fasta directory not found.");
                if(provider.remembersDirectories())
                {
                    PipelineService.get().rememberLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                            getContainer(),getUser(), sequenceDbPaths);
                }
            }
            catch(IOException e)
            {
                 results.appendError("There was a problem retrieving the database list from the server:\n"
                        + e.getMessage());
            }
        }
        results.setSequenceDbPaths(sequenceDbPaths);
    }

    public GWTSearchServiceResult getSequenceDbs(String defaultDb, String dirSequenceRoot, String searchEngine, boolean refresh)
    {
        if(defaultDb == null) defaultDb = "";
        String relativePath;
        String savedRelativePath;
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }
        if((defaultDb.length() == 0)||(defaultDb.endsWith("/")))
        {
            String savedDefaultDb = PipelineService.get().getLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(savedDefaultDb == null ||savedDefaultDb.length() == 0)
            {
                savedRelativePath = defaultDb;
                defaultDb = "";
            }
            else
            {
                savedRelativePath = savedDefaultDb.substring(0, savedDefaultDb.lastIndexOf('/') + 1);
            }
            if(defaultDb.equals(""))
            {
                relativePath = savedRelativePath;
            }
            else
            {
                relativePath = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
            }
            if(relativePath.equals(savedRelativePath) && (savedDefaultDb != null && savedDefaultDb.length() != 0))
            {
                defaultDb = savedDefaultDb;
            }
            else
            {
                defaultDb = relativePath;
            }
        }
        else
        {
            relativePath = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
        }
        return getSequenceDbs(relativePath,defaultDb, dirSequenceRoot, searchEngine, refresh);
    }

    private GWTSearchServiceResult getSequenceDbs(String relativePath, String defaultDb, String dirSequenceRoot,
                                                  String searchEngine, boolean refresh)
    {

        List<String> sequenceDbs = null;
        String defaultDbPath;
        ArrayList returnList = new ArrayList();
        if(defaultDb != null && defaultDb.endsWith("/"))
        {
            String savedDb =
                    PipelineService.get().getLastSequenceDbSetting(provider.getProtocolFactory(),getContainer(),getUser());
            if(savedDb != null && savedDb.length() > 0)
            {
                if(defaultDb.equals("/") && (savedDb.indexOf("/") == -1 || savedDb.indexOf("/") == 0 ) )
                {
                    defaultDb = savedDb;
                }
                else if(savedDb.indexOf("/") != -1 && defaultDb.indexOf("/") != 0)
                {
                    String test = savedDb.replaceFirst(defaultDb, "");
                    if(test.indexOf("/") == -1) defaultDb = savedDb;
                }
            }
        }
        getSequenceDbPaths(dirSequenceRoot, searchEngine,refresh);

        if(relativePath.equals("/"))
        {
            defaultDbPath = dirSequenceRoot;
        }
        else
        {
            defaultDbPath = dirSequenceRoot + relativePath;
        }
        URI defaultDbPathURI;
        try
        {
            if(provider.hasRemoteDirectories())
            {
                relativePath = relativePath.replaceAll(" ","%20");
                URI uriPath = new URI(relativePath);
                sequenceDbs =  provider.getSequenceDbDirList(uriPath);
            }
            else
            {
                defaultDbPathURI = new File(defaultDbPath).toURI();
                sequenceDbs =  provider.getSequenceDbDirList(defaultDbPathURI);
            }          
            if(sequenceDbs == null)
            {
                results.appendError("Could not find the default sequence database path : " + defaultDbPath);
                defaultDbPathURI = new File(dirSequenceRoot).toURI();
                sequenceDbs = provider.getSequenceDbDirList(defaultDbPathURI);
            }
            else
            {
                results.setDefaultSequenceDb(defaultDb);
            }
        }
        catch(URISyntaxException e)
        {
            results.appendError("There was a problem parsing the database database path:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }
        catch(IOException e)
        {
            results.appendError("There was a problem retrieving the database list from the server:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }

        if(sequenceDbs == null || sequenceDbs.size() == 0  )
        {
            sequenceDbs = new ArrayList();
            sequenceDbs.add("None found.");
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }

        for(String db:sequenceDbs)
        {
            if(!db.endsWith("/"))
            {
                returnList.add(db);
            }
        }

        if(returnList.size() == 0  )
        {
            returnList = new ArrayList();
            returnList.add("None found.");
        }
        results.setSequenceDbs(returnList, relativePath);
        return results;
    }

    private void getMzXml(String protocolName, String path, String searchEngine)
    {
        if(protocolName == null) protocolName = "";
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }
        PipeRoot pr;
        URI uriRoot = null;
        try
        {
            pr = PipelineService.get().findPipelineRoot(getContainer());
            if (pr == null || !URIUtil.exists(pr.getUri()))
                results.appendError("Can't find root directory.");
            uriRoot = pr.getUri();
        }
        catch(SQLException e)
        {
            results.appendError(e.getMessage());
        }
        URI uriData = URIUtil.resolve(uriRoot, path);
        if (uriData == null)
            results.appendError("Can't find the root directory");

        File dirData = new File(uriData);
        File dirAnalysis = provider.getProtocolFactory().getAnalysisDir(dirData,protocolName);
        if(dirData == null)
        {
            _log.error("Problem loading protocol: no analysis directory in protocol");
            results.appendError("Problem loading protocol: no analysis directory in protocol\n");
        }
        Map<File,FileStatus> mzXmlFileStatus = null;

        try
        {
            mzXmlFileStatus =
                MS2PipelineManager.getAnalysisFileStatus(dirData,dirAnalysis,getContainer());
        }
        catch(IOException e)
        {
            results.appendError(e.getMessage());
        }
        catch(NullPointerException e)
        {
            //Error should have already been reported.
        }
        Map<String,String> returnMap = new HashMap<String,String>();

        if(mzXmlFileStatus.size() > 0)
        {
            for(Map.Entry<File, FileStatus> entry:mzXmlFileStatus.entrySet())
            {
                FileStatus status = entry.getValue();
                String name = entry.getKey().getName();
                returnMap.put(name, status.toString());
            }

        }
        results.setMzXmlMap(returnMap);
    }
}