package org.labkey.announcements.model;

import org.labkey.api.data.Container;

import java.sql.SQLException;

/**
 * User: adam
 * Date: Mar 4, 2007
 * Time: 9:53:16 PM
 */
public class DailyDigestEmailPrefsSelector extends EmailPrefsSelector
{
    protected DailyDigestEmailPrefsSelector(Container c) throws SQLException
    {
        super(c);
    }


    @Override
    protected boolean includeEmailPref(AnnouncementManager.EmailPref ep)
    {
        return super.includeEmailPref(ep) && ((ep.getEmailOptionId() & AnnouncementManager.EMAIL_NOTIFICATION_TYPE_DIGEST) != 0);
    }
}
