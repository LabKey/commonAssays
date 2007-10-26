package org.labkey.ms1;

import org.labkey.api.ms1.MS1Service;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;

/**
 * Implementation of MS1Service.Service
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 26, 2007
 * Time: 10:41:11 AM
 */
public class MS1ServiceImpl implements MS1Service.Service
{

    public TableInfo createFeaturesTableInfo(User user, Container container)
    {
        return createFeaturesTableInfo(user, container, true);
    }

    public TableInfo createFeaturesTableInfo(User user, Container container, boolean includePepFk)
    {
        return new MS1Schema(user, container).getFeaturesTableInfo(includePepFk);
    }
}
