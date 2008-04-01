package org.labkey.ms1.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * User: jeckels
 * Date: March 24, 2007
 */
public interface CompareServiceAsync
{
    void getFeaturesByPeptideComparison(String originalURL, AsyncCallback async);
}