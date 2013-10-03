<%
/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
%>
<input type="hidden" name="column" value="peptide" />
<table>
    <tr>
        <td colspan="2">Please select the columns to include in the comparison:</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="peptideCount" value="1" checked disabled>Count</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="maxPeptideProphet" value="1" checked>Maximum Peptide Prophet Probability</td>
        <td><input type="checkbox" name="avgPeptideProphet" value="1" checked>Average Peptide Prophet Probability</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="minPeptideProphetErrorRate" value="1">Minimum Peptide Prophet Error Rate</td>
        <td><input type="checkbox" name="avgPeptideProphetErrorRate" value="1">Average Peptide Prophet Error Rate</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="sumLightArea-Peptide" value="1">Total light area (quantitation)</td>
        <td><input type="checkbox" name="sumHeavyArea-Peptide" value="1">Total heavy area (quantitation)</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="avgDecimalRatio-Peptide" value="1">Average decimal ratio (quantitation)</td>
        <td><input type="checkbox" name="maxDecimalRatio-Peptide" value="1">Maximum decimal ratio (quantitation)</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="minDecimalRatio-Peptide" value="1">Minimum decimal ratio (quantitation)</td>
    </tr>
</table>
