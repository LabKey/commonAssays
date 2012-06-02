package org.labkey.ms2.protein;

import org.labkey.api.ProteinService;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.view.JspView;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.AnnotationView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.protein.organism.GuessOrgByParsing;
import org.labkey.ms2.protein.organism.GuessOrgBySharedHash;
import org.labkey.ms2.protein.organism.GuessOrgBySharedIdents;
import org.labkey.ms2.protein.organism.OrganismGuessStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: jeckels
 * Date: May 3, 2012
 */
public class ProteinServiceImpl implements ProteinService
{
    private List<OrganismGuessStrategy> _strategies;
    private List<QueryViewProvider<ProteinSearchForm>> _proteinSearchViewProviders = new CopyOnWriteArrayList<QueryViewProvider<ProteinSearchForm>>();
    private List<QueryViewProvider<PeptideSearchForm>> _peptideSearchViewProviders = new CopyOnWriteArrayList<QueryViewProvider<PeptideSearchForm>>();

    public ProteinServiceImpl()
    {
    }

    private synchronized List<OrganismGuessStrategy> getStrategies()
    {
        // Populate lazily since some implementations need access to DbSchemas, etc
        if (_strategies == null)
        {
            _strategies = new ArrayList<OrganismGuessStrategy>();
            _strategies.add(new GuessOrgByParsing());
            _strategies.add(new GuessOrgBySharedHash());
            _strategies.add(new GuessOrgBySharedIdents());
        }
        return _strategies;
    }

    public int ensureProtein(String sequence, String organism, String name, String description)
    {
        ProteinPlus pp = new ProteinPlus(new Protein(name, sequence.getBytes()));
        if (organism == null)
        {
            for (OrganismGuessStrategy strategy : getStrategies())
            {
                organism = strategy.guess(pp);
                if (organism != null)
                {
                    break;
                }
            }
            if (organism == null)
            {
                organism = FastaDbLoader.UNKNOWN_ORGANISM;
            }
        }

        return ProteinManager.ensureProtein(sequence, organism, name, description);
    }

    public void registerProteinSearchView(QueryViewProvider<ProteinSearchForm> provider)
    {
        _proteinSearchViewProviders.add(provider);
    }

    public void registerPeptideSearchView(QueryViewProvider<PeptideSearchForm> provider)
    {
        _peptideSearchViewProviders.add(provider);
    }

    public List<QueryViewProvider<PeptideSearchForm>> getPeptideSearchViews()
    {
        return Collections.unmodifiableList(_peptideSearchViewProviders);
    }

    public WebPartView getProteinCoverageView(int seqId, String[] peptides, int aaRowWidth)
    {
        MS2Controller.ProteinViewBean bean = new MS2Controller.ProteinViewBean();
        bean.protein = ProteinManager.getProtein(seqId);
        bean.protein.setPeptides(peptides);
        bean.aaRowWidth = aaRowWidth;
        return new JspView<MS2Controller.ProteinViewBean>("/org/labkey/ms2/proteinCoverageMap.jsp", bean);
    }

    public WebPartView getAnnotationsView(int seqId)
    {
        org.labkey.ms2.Protein protein = ProteinManager.getProtein(seqId);
        return new AnnotationView(protein);
    }

    public List<QueryViewProvider<ProteinSearchForm>> getProteinSearchViewProviders()
    {
        return Collections.unmodifiableList(_proteinSearchViewProviders);
    }

    public static ProteinServiceImpl getInstance()
    {
        return (ProteinServiceImpl) ServiceRegistry.get().getService(ProteinService.class);
    }
}
