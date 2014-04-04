/*
 * Copyright (c) 2006-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.security.User;
import org.labkey.ms2.reader.ProteinGroup;

import java.util.List;

/**
 * User: jeckels
 * Date: Dec 1, 2006
 */
public class ProteinGroupWithQuantitation extends ProteinGroup
{
    private IcatProteinQuantitation _quantitation;

    private List<Protein> _proteins;
    private List<MS2Peptide> _peptides;

    public Float getHeavy2LightRatioMean()
    {
        return _quantitation == null ? null : _quantitation.getHeavy2lightRatioMean();
    }

    public void setHeavy2LightRatioMean(Float heavy2LightRatioMean)
    {
        if (heavy2LightRatioMean != null)
        {
            ensureQuantitation().setHeavy2lightRatioMean(heavy2LightRatioMean.floatValue());
        }
    }

    public Float getHeavy2LightRatioStandardDev()
    {
        return _quantitation == null ? null : _quantitation.getHeavy2lightRatioStandardDev();
    }

    public void setHeavy2LightRatioStandardDev(Float heavy2LightRatioStandardDev)
    {
        if (heavy2LightRatioStandardDev != null)
        {
            ensureQuantitation().setHeavy2lightRatioStandardDev(heavy2LightRatioStandardDev.floatValue());
        }
    }

    public Float getRatioMean()
    {
        return _quantitation == null ? null : _quantitation.getRatioMean();
    }

    public void setRatioMean(Float ratioMean)
    {
        if (ratioMean != null)
        {
            ensureQuantitation().setRatioMean(ratioMean.floatValue());
        }
    }

    public Integer getRatioNumberPeptides()
    {
        return _quantitation == null ? null : _quantitation.getRatioNumberPeptides();
    }

    public void setRatioNumberPeptides(Integer ratioNumberPeptides)
    {
        if (ratioNumberPeptides != null)
        {
            ensureQuantitation().setRatioNumberPeptides(ratioNumberPeptides.intValue());
        }
    }

    private IcatProteinQuantitation ensureQuantitation()
    {
        if (_quantitation == null)
        {
            _quantitation = new IcatProteinQuantitation();
            _quantitation.setProteinGroupId(getRowId());
        }
        return _quantitation;
    }

    public Float getRatioStandardDev()
    {
        return _quantitation == null ? null : _quantitation.getRatioStandardDev();
    }

    public void setRatioStandardDev(Float ratioStandardDev)
    {
        if (ratioStandardDev != null)
        {
            ensureQuantitation().setRatioStandardDev(ratioStandardDev.floatValue());
        }
    }

    public List<Protein> lookupProteins()
    {
        if (_proteins == null)
        {
            _proteins = MS2Manager.getProteinsForGroup(getProteinProphetFileId(), getGroupNumber(), getIndistinguishableCollectionId());
        }
        return _proteins;
    }

    public List<MS2Peptide> lookupPeptides()
    {
        if (_peptides == null)
        {
            SQLFragment sql = new SQLFragment("SELECT * FROM ");
            sql.append(MS2Manager.getTableInfoPeptidesData());
            sql.append(" WHERE RowId IN (SELECT PeptideId FROM ");
            sql.append(MS2Manager.getTableInfoPeptideMemberships());
            sql.append(" WHERE ProteinGroupId = ?)");
            sql.add(getRowId());
            _peptides = new SqlSelector(MS2Manager.getSchema(), sql).getArrayList(MS2Peptide.class);
        }

        return _peptides;
    }

    private static final int MAX_LOG = 3;

    /**
     * This code is translated from the TPP C++ source at revision 5114 in:
     * https://sashimi.svn.sourceforge.net/svnroot/sashimi/trunk/trans_proteomic_pipeline/src/Quantitation/Q3/Q3ProteinRatioParser/Q3GroupPeptideParser.cxx
     */
    public void recalcQuantitation(User user)
    {
        int ratioNum = 0;
        double h2LRatioSquareSum = 0;
        double h2LRatioLogSum = 0;
        double ratioSquareSum = 0;
        double ratioLogSum = 0;

        for (MS2Peptide ms2Peptide : lookupPeptides())
        {
            PeptideQuantitation quant = ms2Peptide.getQuantitation();
            if (quant != null && quant.includeInProteinCalc())
            {
                double ratio;
                if (quant.getHeavyArea() == 0.0) // inf ratio
                {
                    ratio = Math.pow(10.0, MAX_LOG);
                }
                else
                {
                    ratio = quant.getLightArea() / quant.getHeavyArea();

                    if (ratio > Math.pow(10.0, MAX_LOG))
                    {
                        ratio = Math.pow(10.0, MAX_LOG);
                    }
                    else if (ratio < Math.pow(10.0, -1 * MAX_LOG))
                    {
                        ratio = Math.pow(10.0, -1 * MAX_LOG);
                    }
                }
                double h2LRatio;
                if (ratio <= Math.pow(10.0, -1 * MAX_LOG))
                {
                    h2LRatio = Math.pow(10.0, MAX_LOG);
                }
                else if (ratio >= Math.pow(10.0, MAX_LOG))
                {
                    h2LRatio = Math.pow(10.0, -1 * MAX_LOG);
                }
                else
                {
                    h2LRatio = 1 / ratio;
                }

                h2LRatioSquareSum += Math.log10(h2LRatio) * Math.log10(h2LRatio);
                h2LRatioLogSum += Math.log10(h2LRatio);

                ratioSquareSum += Math.log10(ratio) * Math.log10(ratio);
                ratioLogSum += Math.log10(ratio);
                ratioNum++;
            }
        }

        if (ratioNum == 0)
        {
            // No peptides available for quantitation
            if (_quantitation != null)
            {
                Table.delete(MS2Manager.getTableInfoProteinQuantitation(), getRowId());
                _quantitation = null;
            }
        }
        else
        {
            boolean insert = _quantitation == null;
            // Come up with the aggregates
            double logmean = ratioLogSum / ratioNum;
            double h2LogMean = h2LRatioLogSum / ratioNum;

            if (logmean >= MAX_LOG) // infinite ratio
            {
                setRatioMean(999f);
            }
            else if (logmean <= -1 * MAX_LOG) // zero ratio
            {
                setRatioMean(0f);
            }
            else
            {
                setRatioMean((float)Math.pow(10, logmean));
            }

            if (h2LogMean >= MAX_LOG) // infinite ratio
            {
                setHeavy2LightRatioMean(999f);
            }
            else if (h2LogMean <= -1 * MAX_LOG) // zero ratio
            {
                setHeavy2LightRatioMean(0.0f);
            }
            else
            {
                setHeavy2LightRatioMean((float)Math.pow(10, h2LogMean));
            }

            if (ratioNum == 1)
            {
                setRatioStandardDev(0f);
                setHeavy2LightRatioStandardDev(0f);
            }
            else
            {
                double variance = (ratioSquareSum / ratioNum) - (logmean * logmean);
                double h2l_variance = (h2LRatioSquareSum / ratioNum) - (h2LogMean * h2LogMean);
                if (variance < 0.0)
                    variance = 0.0;
                if (h2l_variance < 0.0)
                    h2l_variance = 0.0;

                setRatioStandardDev((float)Math.sqrt(variance) * getRatioMean().floatValue()); //sqrt((ratio_square_sum_ / ratio_num_) - (logmean * logmean));
                setHeavy2LightRatioStandardDev((float)Math.sqrt(h2l_variance) * getHeavy2LightRatioMean().floatValue());
            }
            setRatioNumberPeptides(ratioNum);
            if (insert)
            {
                Table.insert(user, MS2Manager.getTableInfoProteinQuantitation(), _quantitation);
            }
            else
            {
                Table.update(user, MS2Manager.getTableInfoProteinQuantitation(), _quantitation, getRowId());
            }
        }
    }
}
