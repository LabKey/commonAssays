/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms1.model;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms1.MS1Manager;

/**
 * Represents an MS1 Feature.
 *
 * User: Dave
 * Date: Oct 12, 2007
 * Time: 10:17:09 AM
 */
public class Feature
{
    public int getFeatureId()
    {
        return _featureId;
    }

    public void setFeatureId(int featureId)
    {
        _featureId = featureId;
    }

    public int getFileId()
    {
        return _fileId;
    }

    public void setFileId(int fileId)
    {
        _fileId = fileId;
    }

    public Integer getScan()
    {
        return _scan;
    }

    public void setScan(Integer scan)
    {
        _scan = scan;
    }

    public Double getTime()
    {
        return _time;
    }

    public void setTime(Double time)
    {
        _time = time;
    }

    public Double getMz()
    {
        return _mz;
    }

    public void setMz(Double mz)
    {
        _mz = mz;
    }

    public Boolean getAccurateMz()
    {
        return _accurateMz;
    }

    public void setAccurateMz(Boolean accurateMz)
    {
        _accurateMz = accurateMz;
    }

    public Double getMass()
    {
        return _mass;
    }

    public void setMass(Double mass)
    {
        _mass = mass;
    }

    public Double getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Double intensity)
    {
        _intensity = intensity;
    }

    public Short getCharge()
    {
        return _charge;
    }

    public void setCharge(Short charge)
    {
        _charge = charge;
    }

    public Short getChargeStates()
    {
        return _chargeStates;
    }

    public void setChargeStates(Short chargeStates)
    {
        _chargeStates = chargeStates;
    }

    public Double getKl()
    {
        return _kl;
    }

    public void setKl(Double kl)
    {
        _kl = kl;
    }

    public Double getBackground()
    {
        return _background;
    }

    public void setBackground(Double background)
    {
        _background = background;
    }

    public Double getMedian()
    {
        return _median;
    }

    public void setMedian(Double median)
    {
        _median = median;
    }

    public Integer getPeaks()
    {
        return _peaks;
    }

    public void setPeaks(Integer peaks)
    {
        _peaks = peaks;
    }

    public Integer getScanFirst()
    {
        return _scanFirst;
    }

    public void setScanFirst(Integer scanFirst)
    {
        _scanFirst = scanFirst;
    }

    public Integer getScanLast()
    {
        return _scanLast;
    }

    public void setScanLast(Integer scanLast)
    {
        _scanLast = scanLast;
    }

    public Integer getScanCount()
    {
        return _scanCount;
    }

    public void setScanCount(Integer scanCount)
    {
        _scanCount = scanCount;
    }

    public Double getTotalIntensity()
    {
        return _totalIntensity;
    }

    public void setTotalIntensity(Double totalIntensity)
    {
        _totalIntensity = totalIntensity;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public Integer getMs2Scan()
    {
        return _ms2Scan;
    }

    public void setMs2Scan(Integer ms2Scan)
    {
        _ms2Scan = ms2Scan;
    }

    public Integer getMs2Charge()
    {
        return _ms2Charge;
    }

    public void setMs2Charge(Integer ms2Charge)
    {
        _ms2Charge = ms2Charge;
    }

    public Double getMs2ConnectivityProbability()
    {
        return _ms2ConnectivityProbability;
    }

    public void setMs2ConnectivityProbability(Double ms2ConnectivityProbability)
    {
        _ms2ConnectivityProbability = ms2ConnectivityProbability;
    }

    public Integer getRunId()
    {
        if(null == _runId)
            _runId = MS1Manager.get().getRunIdFromFeature(_featureId);
        return _runId;
    }

    public ExpRun getExpRun()
    {
        if(null == _run)
        {
            Integer runId = getRunId();
            _run = null == runId ? null : ExperimentService.get().getExpRun(getRunId());
        }

        return _run;
    }

    public Peptide[] getMatchingPeptides()
    {
        String sql = "SELECT fr.run, pd.* FROM ms2.PeptidesData AS pd\n" +
                "INNER JOIN ms2.Fractions AS fr ON (fr.fraction=pd.fraction)\n" +
                "INNER JOIN ms2.Runs AS r ON (fr.Run=r.Run)\n" +
                "INNER JOIN exp.Data AS d ON (r.Container=d.Container)\n" +
                "INNER JOIN ms1.Files AS fi ON (fi.MzXmlUrl=fr.MzXmlUrl AND fi.ExpDataFileId=d.RowId)\n" +
                "INNER JOIN ms1.Features AS fe ON (fe.FileId=fi.FileId AND pd.scan=fe.MS2Scan AND pd.charge=fe.MS2Charge)\n" +
                "WHERE fe.FeatureId=? AND r.Deleted=? ORDER BY pd.RowId";

        return new SqlSelector(DbSchema.get("ms2"), sql, _featureId, false).getArray(Peptide.class);
    }

    private int _featureId;
    private int _fileId;
    private Integer _scan;
    private Double _time;
    private Double _mz;
    private Boolean _accurateMz;
    private Double _mass;
    private Double _intensity;
    private Short _charge;
    private Short _chargeStates;
    private Double _kl;
    private Double _background;
    private Double _median;
    private Integer _peaks;
    private Integer _scanFirst;
    private Integer _scanLast;
    private Integer _scanCount;
    private Double _totalIntensity;
    private String _description;
    private Integer _ms2Scan;
    private Integer _ms2Charge;
    private Double _ms2ConnectivityProbability;
    private Integer _runId = null;
    private ExpRun _run = null;
}
