var g_oCapture;
var g_ptCapture;
var g_polygon;
var g_fAddingPoints;

function captureMouse(o, event)
{
    g_oCapture = o;
    g_ptCapture = { x : event.clientX, y : event.clientY };
    if (document.body.setCapture)
    {
        document.body.setCapture();
    }
    if (event.preventDefault)
    {
        // need to prevent default on NS so we don't go into D&D mode.
        event.preventDefault();
    }
}
function releaseCapture(o)
{
    g_oCapture = null;
    if (document.body.releaseCapture)
    {
        document.body.releaseCapture();
    }
    setCursor("")
}
function hasCapture(o)
{
    return o == g_oCapture;
}

function constrainOffset(ptIn, rcBounds)
{
    var pt = { x : ptIn.x, y : ptIn.y };
    pt.x = Math.max(pt.x, rcData.x -rcBounds.x - 1);
    pt.x = Math.min(pt.x, rcData.x + rcData.width - rcBounds.x - rcBounds.width);
    pt.y = Math.max(pt.y, rcData.y -rcBounds.y);
    pt.y = Math.min(pt.y, rcData.y + rcData.height - rcBounds.y - rcBounds.height + 1);
    return pt;
}

function polyBounds(points)
{
    var rc = rcPoint(points[0]);
    for (var i = 0; i < points.length; i ++)
    {
        rc = unionRc(rc, rcPoint(points[i]));
    }
    return rc;
}
function unionRc(rc1, rc2)
{
    var left = Math.min(rc1.x, rc2.x);
    var top = Math.min(rc1.y, rc2.y);
    var right = Math.max(rc1.x + rc1.width, rc2.x + rc2.width);
    var bottom = Math.max(rc1.y + rc1.height, rc2.y + rc2.height);
    return { x : left, y : top, width: right - left, height : bottom - top };
}
function rcPoint(pt)
{
    return { x : pt.x, y : pt.y, height: 0, width: 0};
}

function polygon()
{
    if (g_polygon)
    {
        return g_polygon;
    }
    g_polygon = {
            points: []
    };
    function getPoly()
    {
        var ret = [];
        var pts = getPoints();
        for (var i = 0; i < pts.length; i ++)
        {
            ret.push(toScreenCoordinates(pts[i].x, pts[i].y));
        }
        return ret;
    }
    function getOffset(event)
    {
        var pt = { x : event.clientX - g_ptCapture.x, y : event.clientY - g_ptCapture.y };
        pt = constrainOffset(pt, polyBounds(getPoly()));
        return pt;
    }
    g_polygon.hitTest = function(event)
    {
        for (var i = 0; i < this.points.length; i ++)
        {
            var o = this.points[i].hitTest(event);
            if (o)
                return o;
        }
        if (ptInPoly(event.clientX, event.clientY, getPoly()))
        {
            return this;
        }
    }
    g_polygon.mouseMove = function(event)
    {
        setCursor("move");
        if (hasCapture(this))
        {
            var ptOffset = getOffset(event);
            for (var i = 0; i < this.points.length; i ++)
            {
                this.points[i].offsetPosition(ptOffset.x, ptOffset.y);
            }
        }
    },
    g_polygon.mouseDown = function(event)
    {
        captureMouse(this, event);
    },
    g_polygon.mouseUp = function(event)
    {
        if (!hasCapture(this))
            return;
        var ptOffset = getOffset(event);
        var newPoints = [];
        for (var i = 0; i < getPoints().length; i ++)
        {
            var ptL = getPoints()[i];
            var ptS = toScreenCoordinates(ptL.x, ptL.y);
            ptS.x += ptOffset.x;
            ptS.y += ptOffset.y;
            ptL = toLogicalCoordinates(ptS.x, ptS.y);
            newPoints.push(ptL);
        }
        releaseCapture();
        parent.setPoints(newPoints);
    },
    g_polygon.update = function()
    {
        var pts = getPoints();
        if (pts.length == 0)
        {
            g_fAddingPoints = true;
        }
        for (var i = this.points.length; i < pts.length; i ++)
        {
            this.points[i] = polygonPoint(i);
        }
        for (var i = 0; i < this.points.length; i ++)
        {
            this.points[i].update();
        }
    }
    g_polygon.update();
    return g_polygon;
}

function polygonPoint(index)
{
    var ret = {};
    ret.index = index;
    function getOffset(event)
    {
        var ptOffset = { x : event.clientX - g_ptCapture.x, y : event.clientY - g_ptCapture.y };
        return constrainOffset(ptOffset, rcPoint(getPoint()));
    }
    function getPoint()
    {
        if (index >= getPoints().length)
            return null;
        var ptL = getPoints()[index];
        var ptS = toScreenCoordinates(ptL.x, ptL.y);
        return ptS;
    }
    ret.mouseDown = function(event)
    {
        captureMouse(this, event);
        event.cancelBubble = true;
        event.returnValue = false;
    }
    ret.mouseMove = function(event)
    {
        setCursor("crosshair");
        if (hasCapture(this))
        {
            var ptOffset = getOffset(event);
            this.offsetPosition(ptOffset.x, ptOffset.y);
        }
    }
    ret.mouseUp = function(event)
    {
        if (!hasCapture(this))
            return;
        var ptS = getPoint();
        var ptOffset = getOffset(event);
        ptS.x += ptOffset.x;
        ptS.y += ptOffset.y;
        ptL = toLogicalCoordinates(ptS.x, ptS.y);
        parent.setPoint(index, ptL);
        releaseCapture(this);
    }
    ret.hitTest = function(event)
    {
        if (!getPoint())
            return null;
        if (ptInRect(event.clientX, event.clientY, getOffsetRc(this.img)))
        {
            return this;
        }
    },
    ret.getPoint = function()
    {
        return getPoints()[index];
    }
    ret.offsetPosition = function(xOffset, yOffset)
    {
        var ptS = getPoint();
        if (!ptS)
        {
            this.img.style.display = 'none';
            return;
        }
        this.img.style.display = 'inline';
        this.img.style.left = ptS.x - 1 + xOffset;
        this.img.style.top = ptS.y - 1 + yOffset;
    },
    ret.update = function()
    {
        this.offsetPosition(0, 0);
    }
    ret.setLogicalPosition = function(pt)
    {
        this.setPosition(toScreenCoordinates(pt.x, pt.y));
    }
    ret.img = document.createElement("img");
    ret.img.src = handleSrc;
    ret.img.style.position = "absolute";
    ret.img.style.width = ret.img.style.height = 3;
    document.body.appendChild(ret.img);
    ret.update();
    return ret;
}


function ptInPoly(x, y, pts)
{
    var i, j;
    var contains = false;
    for (i = 0, j = pts.length - 1; i < pts.length; j = i++)
    {
        if ((pts[i].y <= y && y < pts[j].y || pts[j].y <= y && y < pts[i].y) &&
                (x < (pts[j].x - pts[i].x) * (y - pts[i].y) / (pts[j].y - pts[i].y) + pts[i].x))
            contains = !contains;
    }
    return contains;
}


function getPoints()
{
    return parent.getPoints();
}

function between(a, b, c)
{
    return a >= b && a <= c;
}
function within(a, b, r)
{
    return between(a, b - r, b + r);
}
function ptInRect(x, y, rc)
{
    if (!rc)
        return false;
    return between(x, rc.x, rc.x + rc.width) &&
           between(y, rc.y, rc.y + rc.height);
}

function hitTest(event)
{
    var x = event.clientX;
    var y = event.clientY;
    if (g_oCapture)
        return g_oCapture;
    return polygon().hitTest(event);
}

/*
Returns true if the mouse is over the first point of the point of the polygon,
so that adding this new point will close the polygon.
*/

function isClosingPoint(event)
{
    if (g_polygon.points.length == 0)
        return false;
    return g_polygon.points[0].hitTest(event) != null;
}

function mouseMove(el, event)
{
    parent.trackPoint(toLogicalCoordinates(event.clientX, event.clientY));
    if (g_fAddingPoints)
    {
        if (isClosingPoint(event))
        {
            setCursor("pointer");
            return;
        }
        setCursor("crosshair");
        return;
    }
    var o = hitTest(event);
    if (!o || !o.mouseMove)
    {
        setCursor("auto");
        return;
    }
    o.mouseMove(event);
}

function mouseDown(el, event)
{
    if (!isLeftButton(event))
        return;
    if (g_fAddingPoints)
        return;
    var o = hitTest(event);
    if (!o || !o.mouseDown)
        return;
    o.mouseDown(event);
}

function finishAddingPoints()
{
    g_fAddingPoints = false;
    updateImage();
}

function mouseUp(el, event)
{
    if (!isLeftButton(event))
        return;
    if (g_fAddingPoints)
    {
        if (isClosingPoint(event))
        {
            finishAddingPoints();
            return;
        }
        var ptL = toLogicalCoordinates(event.clientX, event.clientY);
        parent.setPoint(getPoints().length, ptL);
        return;
    }
    var o = hitTest(event);
    if (!o || !o.mouseUp)
        return;
    o.mouseUp(event);
}

function updateImage()
{
    var href = window.location.href;
    href = href.replace("graphWindow.view", "graphImage.view");
    var points = parent.getPoints();
    if (points)
    {
        var arr = [];
        for (var i = 0; i < points.length; i ++)
        {
            var coor = toScreenCoordinates(points[i].x, points[i].y);
            arr[i] = coor.x + "," + coor.y;
        }
        href += "&points=" + arr.join(",");
        if (g_fAddingPoints)
        {
            href += "&open=true";
        }
        polygon().update();
    }

    document.getElementById("graph").src = href;
    //href = href.replace("graphImage.view", "selectionImage.view");
    //document.getElementById("selection").src = href;
}
function translate(coor, width, max, log)
{
    if (coor < 0)
        return -1;
    if (coor == 0)
        return 0;
    if (!log)
    {
        return Math.round(max * coor / width);
    }
    return Math.round(Math.exp(coor / width * Math.log(max)));
}
function untranslate(val, width, max, log)
{
    if (val < 0)
        return -1;
    if (val == 0)
        return 0;
    if (!log)
    {
        return Math.round(val * width / max);
    }
    if (val <= 0)
        return 0;
    return Math.round(Math.log(val) / Math.log(max) * width);
}

function toLogicalCoordinates(x, y)
{
    return {
        x: translate(x - rcData.x, rcData.width, rangeX.max, rangeX.log),
        y: translate(rcData.y + rcData.height - y, rcData.height, rangeY.max, rangeY.log)
    }
}
function toScreenCoordinates(x, y)
{
    return { x : untranslate(x, rcData.width, rangeX.max, rangeX.log) + rcData.x,
            y: rcData.y + rcData.height - untranslate(y, rcData.height, rangeY.max, rangeY.log)
            }

}

function isLeftButton(event)
{
    return event.button == 0 || event.button == 1;
}
function setCursor(name)
{
    document.body.style.cursor = name;
}
function getOffsetRc(el)
{
    return { x: el.offsetLeft, y: el.offsetTop, width: el.offsetWidth, height: el.offsetHeight };
}