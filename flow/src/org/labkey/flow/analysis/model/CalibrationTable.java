package org.labkey.flow.analysis.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

public interface CalibrationTable extends Serializable
{
    double indexOf(double value);
    double fromIndex(double index);
    double getRange();
    boolean isLinear();
}
