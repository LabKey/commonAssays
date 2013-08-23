package org.labkey.luminex;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.view.JspView;

import java.util.List;

/**
 * User: jeckels
 * Date: 8/22/13
 */
public class LeveyJenningsMenuView extends JspView<LeveyJenningsMenuView.Bean>
{
    public static class Bean
    {

        private final ExpProtocol _protocol;
        private final List<String> _titrations;
        private final List<String> _singlePointControls;

        public Bean(ExpProtocol protocol, List<String> titrations, List<String> singlePointControls)
        {
            _protocol = protocol;
            _titrations = titrations;
            _singlePointControls = singlePointControls;
        }

        public List<String> getTitrations()
        {
            return _titrations;
        }

        public List<String> getSinglePointControls()
        {
            return _singlePointControls;
        }

        public ExpProtocol getProtocol()
        {
            return _protocol;
        }
    }

    public LeveyJenningsMenuView(ExpProtocol protocol)
    {
        super("/org/labkey/luminex/leveyJenningsMenu.jsp");
        ContainerFilter.AllFolders containerFilter = new ContainerFilter.AllFolders(getViewContext().getUser());
        SQLFragment containerSQL = containerFilter.getSQLFragment(LuminexProtocolSchema.getSchema(), new SQLFragment("r.Container"), getViewContext().getContainer());

        SQLFragment titrationSQL = new SQLFragment("SELECT DISTINCT t.Name FROM ");
        titrationSQL.append(LuminexProtocolSchema.getTableInfoTitration(), "t");
        titrationSQL.append(", ");
        titrationSQL.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        titrationSQL.append(" WHERE r.RowId = t.RunId AND r.ProtocolLSID = ? AND (t.Standard = ? OR t.QCControl = ?) AND ");
        titrationSQL.add(protocol.getLSID());
        titrationSQL.add(true);
        titrationSQL.add(true);
        titrationSQL.append(containerSQL);
        titrationSQL.append(" ORDER BY t.Name");
        List<String> titrations = new SqlSelector(LuminexProtocolSchema.getTableInfoTitration().getSchema(), titrationSQL).getArrayList(String.class);

        SQLFragment singlePointControlSQL = new SQLFragment("SELECT DISTINCT s.Name FROM ");
        singlePointControlSQL.append(LuminexProtocolSchema.getTableInfoSinglePointControl(), "s");
        singlePointControlSQL.append(", ");
        singlePointControlSQL.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        singlePointControlSQL.append(" WHERE s.RunId = r.RowId AND r.ProtocolLSID = ? AND ");
        singlePointControlSQL.add(protocol.getLSID());
        singlePointControlSQL.append(containerSQL);
        singlePointControlSQL.append(" ORDER BY s.Name");
        List<String> singlePointControls = new SqlSelector(LuminexProtocolSchema.getTableInfoTitration().getSchema(), singlePointControlSQL).getArrayList(String.class);

        setModelBean(new Bean(protocol, titrations, singlePointControls));
    }
}
