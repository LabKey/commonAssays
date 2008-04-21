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
 * Created by IntelliJ IDEA.
 * User: Bill
 * Date: Jan 29, 2008
 * Time: 4:04:45 PM
 * To change this template use File | Settings | File Templates.
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
            getSequenceDbs(results.getDefaultSequenceDb(), dirSequenceRoot, searchEngine);
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
                results.appendError("Error: Problem loading protocol: provider equals null\n");
            }
        }
        if(protocolName == null || protocolName.length() == 0)
        {
            protocolName = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
        if(protocolName == null || protocolName.length() == 0) protocolName = "new";
        }
        if(protocolName.equals("new"))
        {
            results.setSelectedProtocol("");
            getMzXml("",path, searchEngine);
            return results;
        }
        URI uriRoot = null;
        try
        {
            uriRoot = new URI(dirRoot);   
        }
        catch(URISyntaxException e)
        {
            results.setSelectedProtocol("Loading Error");
            _log.error("Problem loading protocols", e);
            results.appendError("Error: Problem loading protocol\n" + e.getMessage());
        }
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
            results.appendError("Error: Problem loading protocol\n" + e.getMessage());
        }
        results.setSelectedProtocol(protocol.getName());
        File dbFile = null;
        try
        {
            results.setDefaultSequenceDb(protocol.getDbNames()[0]);
            if(!provider.dbExists(dirSequenceRoot,protocol.getDbNames()[0]))
                results.appendError("Error: The database " + dbFile.getPath() + " cannot be found.");
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            _log.error("Problem loading protocol: no database in protocol");
            results.appendError("Error: Problem loading protocol: No database in protocol");
        }

        PipelineService.get().rememberLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                    getUser(),"",results.getDefaultSequenceDb());
        results.setProtocolDescription(protocol.getDescription());
        results.setProtocolXml(protocol.getXml());
        getMzXml(results.getSelectedProtocol(),path, searchEngine);
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
        URI dirRootURI;
        try
        {
            dirRootURI = new URI(dirRoot);
        }
        catch(URISyntaxException e)
        {
            protocolList.add("Loading Error.");
            results.appendError("Error: Problem loading protocols\n" + e.getMessage());
            results.setProtocols(protocolList);
            return;
        }
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
        if(!provider.supportsDirectories()) return;
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }

        List<String> sequenceDbPaths;
        sequenceDbPaths = PipelineService.get().getLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                getContainer(),getUser());
        if(sequenceDbPaths == null || sequenceDbPaths.size() == 0 || refresh)
        {
            URI dirSequenceRootURI;
            try
            {
                dirSequenceRootURI = new URI(dirSequenceRoot);
                sequenceDbPaths =  provider.getSequenceDbPaths(dirSequenceRootURI);
                if(sequenceDbPaths == null) throw new IOException("Fasta directory not found.");
                if(provider.remembersDirectories())
                {
                    PipelineService.get().rememberLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                            getContainer(),getUser(), sequenceDbPaths);
                }
            }
            catch(URISyntaxException e)
            {
                results.appendError("Error: There was a problem retrieving the database list from the server:\n"
                        + e.getMessage());
                return;
            }
            catch(IOException e)
            {
                 results.appendError("Error: There was a problem retrieving the database list from the server:\n"
                        + e.getMessage());
            }
        }
        results.setSequenceDbPaths(sequenceDbPaths);
    }

    public GWTSearchServiceResult getSequenceDbs(String defaultDb, String dirSequenceRoot, String searchEngine)
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
                    defaultDb = relativePath;
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
        return getSequenceDbs(relativePath,defaultDb, dirSequenceRoot, searchEngine);
    }

    public GWTSearchServiceResult refreshSequenceDbPaths(String dirSequenceRoot)
    {
        return getSequenceDbs("","", dirSequenceRoot, "X! Tandem", true);
    }

    private GWTSearchServiceResult getSequenceDbs(String relativePath, String defaultDb, String dirSequenceRoot,
                                                  String searchEngine)
    {
        return getSequenceDbs(relativePath, defaultDb, dirSequenceRoot, searchEngine, false);
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
                if(defaultDb.equals("/") && savedDb.indexOf("/") == -1)
                {
                    defaultDb = savedDb;
                }
                else if(savedDb.indexOf("/") != -1)
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
                defaultDbPathURI = new URI(relativePath);
            }
            else
            {
                defaultDbPathURI = new URI(defaultDbPath);
            }
            sequenceDbs =  provider.getSequenceDbDirList(defaultDbPathURI);
            if(sequenceDbs == null) throw new IOException("Fasta files not found.");
            results.setDefaultSequenceDb(defaultDb);
        }
        catch(URISyntaxException e)
        {
            results.appendError("Error: There was a problem retrieving the database list from the server:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs);
            return results;
        }
        catch(IOException e)
        {
            results.appendError("Error: There was a problem retrieving the database list from the server:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs);
            return results;
        }

        if(sequenceDbs == null || sequenceDbs.size() == 0  )
        {
            sequenceDbs = new ArrayList();
            sequenceDbs.add("None found.");
            results.setSequenceDbs(sequenceDbs);
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
        results.setSequenceDbs(returnList);
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
                results.appendError("Error: Can't find root directory.");
            uriRoot = pr.getUri();
        }
        catch(SQLException e)
        {
            results.appendError("Error: " + e.getMessage());
        }
        URI uriData = URIUtil.resolve(uriRoot, path);
        if (uriData == null)
            results.appendError("Error: Can't find the root directory");

        File dirData = new File(uriData);
        File dirAnalysis = provider.getProtocolFactory().getAnalysisDir(dirData,protocolName);;
        if(dirData == null)
        {
            _log.error("Problem loading protocol: no analysis directory in protocol");
            results.appendError("Error: Problem loading protocol: no analysis directory in protocol\n");
        }
        Map<File,FileStatus> mzXmlFileStatus = null;

        try
        {
            mzXmlFileStatus =
                MS2PipelineManager.getAnalysisFileStatus(dirData,dirAnalysis,getContainer());
        }
        catch(IOException e)
        {
            results.appendError("Error: " + e.getMessage());
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