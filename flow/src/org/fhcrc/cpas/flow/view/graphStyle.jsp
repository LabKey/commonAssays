<%@ page import="org.fhcrc.cpas.view.WebTheme" %>
<%@ page import="org.fhcrc.cpas.view.ThemeFont" %>
<style type="text/css">
    .graphView TD {
        font-family: verdana;
        font-size: <%=ThemeFont.getThemeFont().getNormalSize()%>;
        vertical-align: top;
        padding-right: 4px;
        white-space:nowrap;
    }

    .graphView A {
        color: #003399;
        text-decoration: none;
    }

    .graphView .ms-vh {
        background-color:<%=WebTheme.getTheme().getEditFormColor()%>;
    }
</style>