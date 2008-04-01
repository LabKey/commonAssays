package org.labkey.ms1.client;

import org.labkey.api.gwt.client.ui.VennDiagramView;
import org.labkey.api.gwt.client.util.ServiceUtil;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;

/**
 * User: jeckels
 * Date: March 24, 2007
 */
public class MS1VennDiagramView extends VennDiagramView implements EntryPoint
{
    public static final String FEATURES_BY_PEPTIDE = "FeaturesByPeptide";

    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.ms1.MS1VennDiagramView-Root");
        initialize(panel);
        requestComparison();
    }

    private CompareServiceAsync _service;
    private CompareServiceAsync getService()
    {
        if (_service == null)
        {
            _service = (CompareServiceAsync) GWT.create(CompareService.class);
            ServiceUtil.configureEndpoint(_service, "compareService");
        }
        return _service;
    }

    protected void requestComparison(String originalURL, String comparisonGroup, AsyncCallback callbackHandler)
    {
        if (FEATURES_BY_PEPTIDE.equalsIgnoreCase(comparisonGroup))
        {
            getService().getFeaturesByPeptideComparison(originalURL, callbackHandler);
        }
        else
        {
            throw new IllegalArgumentException("Unknown comparison group: " + comparisonGroup);
        }
    }

}