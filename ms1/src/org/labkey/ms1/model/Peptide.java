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

/**
 * Simple bean representing a row in ms2.PeptideData
 * User: Dave
 * Date: Oct 26, 2007
 * Time: 1:45:37 PM
 */
public class Peptide
{
    private long _rowId;
    private int _fraction;
    private int _scan;
    private short _charge;
    private double _score1;
    private double _score2;
    private double _score3;
    private Double _score4;
    private Double _score5;
    private double _ionPercent;
    private double _mass;
    private double _deltaMass;
    private double _peptideProphet;
    private String _peptide;
    private char _prevAA;
    private String _trimmedPeptide;
    private char _nextAA;
    private short _proteinHits;
    private int _sequencePosition;
    private String _protein;
    private Integer _seqId;
    private Double _retentionTime;
    private Double _peptideProphetErrorRate;
    private int _run = -1; //run column from fractions table if used in the select

    public long getRowId()
    {
        return _rowId;
    }

    public void setRowId(long rowId)
    {
        _rowId = rowId;
    }

    public int getFraction()
    {
        return _fraction;
    }

    public void setFraction(int fraction)
    {
        _fraction = fraction;
    }

    public int getScan()
    {
        return _scan;
    }

    public void setScan(int scan)
    {
        _scan = scan;
    }

    public short getCharge()
    {
        return _charge;
    }

    public void setCharge(short charge)
    {
        _charge = charge;
    }

    public double getScore1()
    {
        return _score1;
    }

    public void setScore1(double score1)
    {
        _score1 = score1;
    }

    public double getScore2()
    {
        return _score2;
    }

    public void setScore2(double score2)
    {
        _score2 = score2;
    }

    public double getScore3()
    {
        return _score3;
    }

    public void setScore3(double score3)
    {
        _score3 = score3;
    }

    public Double getScore4()
    {
        return _score4;
    }

    public void setScore4(Double score4)
    {
        _score4 = score4;
    }

    public Double getScore5()
    {
        return _score5;
    }

    public void setScore5(Double score5)
    {
        _score5 = score5;
    }

    public double getIonPercent()
    {
        return _ionPercent;
    }

    public void setIonPercent(double ionPercent)
    {
        _ionPercent = ionPercent;
    }

    public double getMass()
    {
        return _mass;
    }

    public void setMass(double mass)
    {
        _mass = mass;
    }

    public double getDeltaMass()
    {
        return _deltaMass;
    }

    public void setDeltaMass(double deltaMass)
    {
        _deltaMass = deltaMass;
    }

    public double getPeptideProphet()
    {
        return _peptideProphet;
    }

    public void setPeptideProphet(double peptideProphet)
    {
        _peptideProphet = peptideProphet;
    }

    public String getPeptide()
    {
        return _peptide;
    }

    public void setPeptide(String peptide)
    {
        _peptide = peptide;
    }

    public char getPrevAA()
    {
        return _prevAA;
    }

    public void setPrevAA(char prevAA)
    {
        _prevAA = prevAA;
    }

    public String getTrimmedPeptide()
    {
        return _trimmedPeptide;
    }

    public void setTrimmedPeptide(String trimmedPeptide)
    {
        _trimmedPeptide = trimmedPeptide;
    }

    public char getNextAA()
    {
        return _nextAA;
    }

    public void setNextAA(char nextAA)
    {
        _nextAA = nextAA;
    }

    public short getProteinHits()
    {
        return _proteinHits;
    }

    public void setProteinHits(short proteinHits)
    {
        _proteinHits = proteinHits;
    }

    public int getSequencePosition()
    {
        return _sequencePosition;
    }

    public void setSequencePosition(int sequencePosition)
    {
        _sequencePosition = sequencePosition;
    }

    public String getProtein()
    {
        return _protein;
    }

    public void setProtein(String protein)
    {
        _protein = protein;
    }

    public Integer getSeqId()
    {
        return _seqId;
    }

    public void setSeqId(Integer seqId)
    {
        _seqId = seqId;
    }

    public Double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(Double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getPeptideProphetErrorRate()
    {
        return _peptideProphetErrorRate;
    }

    public void setPeptideProphetErrorRate(Double peptideProphetErrorRate)
    {
        _peptideProphetErrorRate = peptideProphetErrorRate;
    }

    public int getRun()
    {
        return _run;
    }

    public void setRun(int run)
    {
        _run = run;
    }
}
