package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializableException;

/**
 * User: billnelson@uky.edu
 * Date: Jan 29, 2008
 */
public interface SearchServiceAsync
{
    void getSearchServiceResult(String searchEngine,String dirSequenceRoot, String dirRoot, String path,
                                AsyncCallback async);
    void getSequenceDbs(String defaultDb, String dirSequenceRoot, String searchEngine, AsyncCallback async);

    void refreshSequenceDbPaths(String dirSequenceRoot, AsyncCallback async);

    void getProtocol(String searchEngine, String protocolName, String dirRoot,String dirSequenceRoot,
                     String path, AsyncCallback async);
}
