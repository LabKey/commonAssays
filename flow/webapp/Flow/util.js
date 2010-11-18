/*
 * Copyright (c) 2006-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function flowImgError(img)
{
    img.onerror = null;
    img.src = LABKEY.ActionURL.getContextPath() + '/_images/exclaim.gif';
    img.style.width = "12px";
    img.style.height = "12px";

    var msg = 'Error generating graph.<br>See the server log for more information.';
    var node = document.createElement("span");
    node.className = 'labkey-error';
    node.innerHTML = msg;
    img.parentNode.appendChild(node);
}
