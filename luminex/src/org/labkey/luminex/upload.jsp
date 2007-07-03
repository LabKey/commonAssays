<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib"%>

<form action="processUpload.view" method="post" enctype="multipart/form-data">
<table>
    <tr>
        <td class="ms-searchform">Network:</td>
        <td>
            <select name="Network">
                <option value="CHAVI" selected="true">CHAVI</option>
            </select>
        </td>
    </tr>
    <tr>
        <td class="ms-searchform">Study:</td>
        <td>
            <select name="Study">
                <option value="CHAVI 001" selected="true">CHAVI 001</option>
                <option value="CHAVI 002">CHAVI 002</option>
                <option value="CHAVI 003">CHAVI 003</option>
            </select>
        </td>
    </tr>
    <tr>
        <td class="ms-searchform">Species:</td>
        <td>
            <select name="Species">
                <option value="Human" selected="true">Human</option>
            </select>
        </td>
    </tr>
    <tr>
        <td class="ms-searchform">Lab:</td>
        <td>
            <select name="labId">
                <option value="Haynes">Haynes</option>
                <option value="Montefiori">Montefiori</option>
                <option value="Tomaras" selected="true">Tomaras</option>
            </select>
        </td>
    </tr>
<!--    <tr>
        <td class="ms-searchform">Weighted:</td>
        <td>
            <input type="radio" name="weighted" value="yes"> Yes<br/>
            <input type="radio" name="weighted" value="no" checked="true"> No<br/>
        </td>
    </tr>-->
    <tr>
        <td class="ms-searchform">Data file:</td>
        <td>
            <input type="file" name="file1" size="40" />
    </tr>

    <tr>
        <td class="ms-searchform">Thaw list:</td>
        <td>
            <textarea rows="8" cols="50" name="thawList"></textarea>
        </td>
    </tr>

    <tr>
        <td></td>
        <td></td>
    </tr>
    <tr>
        <td></td>
        <td><labkey:button text="Upload" /></td>
    </tr>
</table>
</form>