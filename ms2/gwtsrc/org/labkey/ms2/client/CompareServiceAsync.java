package org.labkey.ms2.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public interface CompareServiceAsync
{

    void getProteinProphetComparison(String originalURL, AsyncCallback async);

    void getPeptideComparison(String originalURL, AsyncCallback async);

    void getProteinProphetCrosstabComparison(String originalURL, AsyncCallback async);
}
