package org.labkey.ms2.protein.organism;

import org.labkey.ms2.protein.ProteinPlus;

import java.sql.SQLException;

/**
 * User: brittp
 * Date: Jan 2, 2006
 * Time: 3:41:13 PM
 */
public interface OrganismGuessStrategy
{
    public String guess(ProteinPlus p) throws SQLException;
}