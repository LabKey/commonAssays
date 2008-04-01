package org.labkey.ms1.client;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.core.client.GWT;
import org.labkey.api.gwt.client.model.GWTComparisonResult;

/**
 * User: jeckels
 * Date: March 24, 2007
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
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "org.labkey.ms1.MS1VennDiagramView/CompareService");
            }
            return ourInstance;
        }

    }

    public GWTComparisonResult getFeaturesByPeptideComparison(String originalURL) throws Exception;

}