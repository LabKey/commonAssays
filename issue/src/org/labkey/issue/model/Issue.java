/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.issue.model;

import org.labkey.api.data.Container;
import org.labkey.api.data.Entity;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.MemTracker;

import javax.ejb.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


@javax.ejb.Entity
@Table(name = "Issues")
public class Issue extends Entity implements Serializable, Cloneable
{
    public static String statusOPEN = "open";
    public static String statusRESOLVED = "resolved";
    public static String statusCLOSED = "closed";

    protected byte[] _ts;
    protected int issueId;
    protected String title;
    protected String status;
    protected Integer assignedTo;
    protected String type;

    protected String area;
    protected int priority = 3;
    protected String milestone;
    protected String buildFound;

    protected String tag;

    protected Integer resolvedBy;
    protected Date resolved;
    protected String resolution;
    protected Integer duplicate;

    protected Integer closedBy;
    protected Date closed;

    protected String string1;
    protected String string2;
    protected Integer int1;
    protected Integer int2;

    protected Collection<Issue.Comment> comments = new ArrayList<Issue.Comment>();
    protected Collection<Issue.Comment> added = null;

    protected String _notifyList;

    public Issue()
    {
        assert MemTracker.put(this);
    }


    public void Change(User u)
    {
        beforeUpdate(u);
    }

    public void Open(Container c, User u)
    {
        Open(c, u, false);
    }

    public void Open(Container c, User u, boolean fSave)
    {
        if (0 == getCreatedBy())
        {
            beforeInsert(u, c.getId());
            priority = 3;
        }
        Change(u);

        status = statusOPEN;

        //on reopen display, set resolvedBy to assignedTo
        //on reopen save, use current value of assignedTo
        if (!fSave && null != resolvedBy)
            assignedTo = resolvedBy;
    }

    public void beforeResolve(User u)
    {
        status = statusRESOLVED;
        resolution = "Fixed";

        resolvedBy = u.getUserId(); // Current user
        resolved = new Date();      // Current date

        assignedTo = getCreatedBy();
    }


    public void Resolve(User u)
    {
        Change(u);
        status = statusRESOLVED;

        resolvedBy = getModifiedBy();
        resolved = getModified();
    }


    public void Close(User u)
    {
        Change(u);
        status = statusCLOSED;

        closedBy = getModifiedBy();
        setClosed(getModified());
        // UNDONE: assignedTo is not nullable in database
        // UNDONE: let application enforce non-null for open/resolved bugs
        // UNDONE: currently AssignedTo list defaults to Guest (user 0)
        assignedTo = new Integer(0);
    }


    @Id(generate = GeneratorType.AUTO)
    public int getIssueId()
    {
        return issueId;
    }


    public void setIssueId(int issueId)
    {
        this.issueId = issueId;
    }

    public String getTitle()
    {
        return title;
    }


    public void setTitle(String title)
    {
        this.title = title;
    }


    public String getStatus()
    {
        return status;
    }


    public void setStatus(String status)
    {
        this.status = status;
    }


    public Integer getAssignedTo()
    {
        return assignedTo;
    }


    public void setAssignedTo(Integer assignedTo)
    {
        if (null != assignedTo && assignedTo.intValue() == 0) assignedTo = null;
        this.assignedTo = assignedTo;
    }


    @Transient
    public String getAssignedToName()
    {
        return UserManager.getDisplayName(assignedTo);
    }


    public String getType()
    {
        return type;
    }


    public void setType(String type)
    {
        this.type = type;
    }


    public String getArea()
    {
        return area;
    }


    public void setArea(String area)
    {
        this.area = area;
    }


    public int getPriority()
    {
        return priority;
    }


    public void setPriority(int priority)
    {
        this.priority = priority;
    }


    public String getMilestone()
    {
        return milestone;
    }


    public void setMilestone(String milestone)
    {
        this.milestone = milestone;
    }


/*
    public String getBuildFound()
    {
        return buildFound;
    }


    public void setBuildFound(String buildFound)
    {
        this.buildFound = buildFound;
    }
*/


    @Transient
    public String getCreatedByName()
    {
        return UserManager.getDisplayName(getCreatedBy());
    }


    public String getTag()
    {
        return tag;
    }


    public void setTag(String tag)
    {
        this.tag = tag;
    }


    public Integer getResolvedBy()
    {
        return resolvedBy;
    }


    public void setResolvedBy(Integer resolvedBy)
    {
        if (null != resolvedBy && resolvedBy.intValue() == 0) resolvedBy = null;
        this.resolvedBy = resolvedBy;
    }


    @Transient
    public String getResolvedByName()
    {
        return UserManager.getDisplayName(getResolvedBy());
    }


    public Date getResolved()
    {
        return resolved;
    }


    public void setResolved(Date resolved)
    {
        this.resolved = resolved;
    }


    public String getResolution()
    {
        return resolution;
    }


    public void setResolution(String resolution)
    {
        this.resolution = resolution;
    }


    public Integer getDuplicate()
    {
        return duplicate;
    }


    public void setDuplicate(Integer duplicate)
    {
        if (null != duplicate && duplicate.intValue() == 0) duplicate = null;
        this.duplicate = duplicate;
    }


    public Integer getClosedBy()
    {
        return closedBy;
    }


    public void setClosedBy(Integer closedBy)
    {
        if (null != closedBy && closedBy.intValue() == 0) closedBy = null;
        this.closedBy = closedBy;
    }


    @Transient
    public String getClosedByName()
    {
        return UserManager.getDisplayName(getClosedBy());
    }


    public Date getClosed()
    {
        return closed;
    }


    public void setClosed(Date closed)
    {
        this.closed = closed;
    }


    public String getString2()
    {
        return string2;
    }

    public void setString2(String string2)
    {
        this.string2 = string2;
    }

    public String getString1()
    {
        return string1;
    }

    public void setString1(String string1)
    {
        this.string1 = string1;
    }

    public Integer getInt2()
    {
        return int2;
    }

    public void setInt2(Integer int2)
    {
        this.int2 = int2;
    }

    public Integer getInt1()
    {
        return int1;
    }

    public void setInt1(Integer int1)
    {
        this.int1 = int1;
    }

    @OneToMany(fetch = FetchType.EAGER, targetEntity = "Issues.model.Issue$Comment", cascade = CascadeType.ALL)
    @JoinColumn(name = "issueId")
    public Collection<Issue.Comment> getComments()
    {
//        if (null == comments)
//            comments = new ArrayList();
        return comments;
    }

    public Issue.Comment getLastComment()
    {
        if (null == comments)
            return null;
        Object[] a = comments.toArray();
        return a.length == 0 ? null : (Issue.Comment)a[a.length-1];
    }



    public void setComments(Collection<Issue.Comment> comments)
    {
        this.comments = comments;
    }


    @Transient
    public String getModifiedByName()
    {
        return UserManager.getDisplayName(getModifiedBy());
    }


    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }


    // UNDONE: MAKE work in Table version
    public Comment addComment(User user, String text)
    {
        Comment comment = new Comment();
        comment.setIssue(this);
        comment.setComment(text);
        comment.setCreated(new Date());
        comment.setCreatedBy(user.getUserId());

        comments.add(comment);
        if (null == added)
            added = new ArrayList<Issue.Comment>(1);
        added.add(comment);
        return comment;
    }

    public void setNotifyList(String notifyList)
    {
        _notifyList = notifyList;
    }

    public String getNotifyList()
    {
        return _notifyList;
    }

    /* CONSIDER: use Announcements/Notes instead of special Comments class */

    @javax.ejb.Entity
    @Table(name = "Comments")
    public static class Comment implements Serializable
    {
        private Issue issue;
        private int commentId;
        Date created;
        int createdBy;
        String comment;

        @ManyToOne
        @JoinColumn(name = "issueId")
        public Issue getIssue()
        {
            return issue;
        }

        public void setIssue(Issue issue)
        {
            this.issue = issue;
        }

        @Id(generate = GeneratorType.AUTO)
        public int getCommentId()
        {
            return commentId;
        }

        public void setCommentId(int commentId)
        {
            this.commentId = commentId;
        }

        public Date getCreated()
        {
            return created;
        }

        @Transient
        public String getCreatedByName()
        {
            return UserManager.getDisplayName(getCreatedBy());
        }

        public void setCreated(Date created)
        {
            this.created = created;
        }

        public int getCreatedBy()
        {
            return createdBy;
        }

        public void setCreatedBy(int createdBy)
        {
            this.createdBy = createdBy;
        }

        public String getComment()
        {
            return comment;
        }

        public void setComment(String comment)
        {
            this.comment = comment;
        }
    }
}
