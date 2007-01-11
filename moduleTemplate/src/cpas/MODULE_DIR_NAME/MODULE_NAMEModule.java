package cpas.@@MODULE_LOWERCASE_NAME@@;

import org.fhcrc.cpas.module.DefaultModule;
import org.fhcrc.cpas.module.ModuleContext;
import org.fhcrc.cpas.data.ContainerManager;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.DbSchema;
import org.fhcrc.cpas.view.WebPartFactory;
import org.fhcrc.cpas.view.WebPartView;
import org.fhcrc.cpas.view.Portal;
import org.fhcrc.cpas.view.ViewContext;
import org.fhcrc.cpas.util.PageFlowUtil;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Set;
import java.lang.reflect.InvocationTargetException;

import @@MODULE_LOWERCASE_NAME@@.@@MODULE_NAME@@Controller;

public class @@MODULE_NAME@@Module extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(@@MODULE_NAME@@Module.class);
    public static final String NAME = "@@MODULE_NAME@@";

    private static final String[] RECOGNIZED_PAGEFLOWS = new String[]{"@@MODULE_LOWERCASE_NAME@@"};

    public @@MODULE_NAME@@Module()
    {
        super(NAME, 0.01, "/@@MODULE_LOWERCASE_NAME@@", new WebPartFactory[0]);
        addController("@@MODULE_LOWERCASE_NAME@@", @@MODULE_NAME@@Controller.class);
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c)
    {
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(@@MODULE_NAME@@Schema.getInstance().getSchema());
    }

    public String[] getPageFlows()
    {
        return RECOGNIZED_PAGEFLOWS;
    }
}