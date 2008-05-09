package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializableException;
import org.labkey.ms2.pipeline.client.GWTSearchServiceResult;


/**
 * User: billnelson@uky.edu
 * Date: Jan 29, 2008
 */
public interface SearchService extends RemoteService
{
    public GWTSearchServiceResult getSearchServiceResult(String searchEngine,String dirSequenceRoot, String dirRoot,
                                                  String path) throws SerializableException;

    public GWTSearchServiceResult getSequenceDbs(String defaultDb, String dirSequenceRoot,String searchEngine)
            throws SerializableException;

    public GWTSearchServiceResult refreshSequenceDbPaths(String dirSequenceRoot) throws SerializableException;

    public GWTSearchServiceResult getProtocol(String searchEngine, String protocolName, String dirRoot,
                                              String dirSequenceRoot,String path)
            throws SerializableException;

    public GWTSearchServiceResult getMascotTaxonomy(String searchEngine);

    public GWTSearchServiceResult getEnzymes(String searchEngine);
}
