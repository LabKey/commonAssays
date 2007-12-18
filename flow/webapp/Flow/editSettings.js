
function addNewCriteria()
{
    var criteriaTable = document.getElementById("criteria.table");
    var no_criteria = document.getElementById("no_criteria");

    if (no_criteria && no_criteria.parentNode != null)
    {
        no_criteria.parentNode.removeChild(no_criteria);
    }

    var doc = criteriaTable.ownerDocument;

    var tr = doc.createElement('tr');
    var td = doc.createElement('td');
    var img = doc.createElement('img');
    img.src = contextPath + '/_images/partdelete.gif';
    img.title = "Delete Criteria";
    img.alt = "delete";
    img.onclick = function (event) { deleteCriteria(img); }
    td.appendChild(img);
    tr.appendChild(td);

    td = doc.createElement('td');
    var keyword_input = doc.createElement('input');
    keyword_input.type = 'text';
    keyword_input.name = 'ff_criteria_keyword';
    keyword_input.value = '';
    td.appendChild(keyword_input);
    tr.appendChild(td);

    td = doc.createElement('td');
    var input = doc.createElement('input');
    input.type = 'text';
    input.name = 'ff_criteria_pattern';
    input.value = '';
    td.appendChild(input);
    tr.appendChild(td);

    criteriaTable.appendChild(tr);
    keyword_input.focus();
}

function deleteCriteria(el)
{
    var criteriaTable = document.getElementById("criteria.table");
    var tr = el;
    do {
        tr = tr.parentNode;
    } while (tr && tr.tagName.toUpperCase() != "TR" && tr != el.ownerDocument);

    if (tr && tr.tagName.toUpperCase() == "TR")
    {
        tr.parentNode.removeChild(tr);
    }

    if (criteriaTable.getElementsByTagName("TR").length == 1)
    {
        var doc = criteriaTable.ownerDocument;
        var tr = doc.createElement('tr');
        tr.id = 'no_criteria';
        var td = doc.createElement('td');
        td.colspan = "2";
        td.innerHTML = "<i>No criteria defined.</i>";
        tr.appendChild(td);
        
        criteriaTable.appendChild(tr);
    }
}