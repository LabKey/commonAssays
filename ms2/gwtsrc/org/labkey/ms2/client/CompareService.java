package org.labkey.ms2.client;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.core.client.GWT;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public interface CompareService extends RemoteService
{
    /**
     * Utility/Convinience class.
     * Use CompareService.App.getInstance() to access static instance of CompareServiceAsync
     */
    public static class App
    {
        private static CompareServiceAsync ourInstance = null;

        public static synchronized CompareServiceAsync getInstance()
        {
            if (ourInstance == null)
            {
                ourInstance = (CompareServiceAsync) GWT.create(CompareService.class);
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "org.labkey.ms2.RunComparator/CompareService");
            }
            return ourInstance;
        }

    }

    public CompareResult getProteinProphetComparison(String originalURL) throws Exception;

    public CompareResult getPeptideComparison(String originalURL) throws Exception;


}
