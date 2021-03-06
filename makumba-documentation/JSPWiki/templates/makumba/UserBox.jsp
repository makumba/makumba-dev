<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
%>
  <%-- action buttons --%>
  <wiki:UserCheck status="notAuthenticated">
  <wiki:CheckRequestContext context='!login'>
    <wiki:Permission permission="login">
      <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param 
         name='redirect' value='<%=c.getEngine().encodeName(c.getName())%>'/></wiki:Link>" 
        title="<fmt:message key='actions.login.title'/>"><fmt:message key="actions.login"/></a>
    </wiki:Permission>
  </wiki:CheckRequestContext>
  </wiki:UserCheck>
  
  <wiki:UserCheck status="authenticated">
    <%-- manu: link to meta category for authenticated users --%>
    <wiki:LinkTo page="Meta" >mak:meta</wiki:LinkTo> - 
    <a href="<wiki:Link jsp='Logout.jsp' format='url' />" 
     title="<fmt:message key='actions.logout.title'/>"><fmt:message key="actions.logout"/></a> -
    <%-- manu: show my prefs only to logged-in users --%>
    <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
      <a href="<wiki:Link jsp='UserPreferences.jsp' format='url' ><wiki:Param name='redirect'
        value='<%=c.getEngine().encodeName(c.getName())%>'/></wiki:Link>"
        accesskey="p"
        title="<fmt:message key='actions.prefs.title'/>"><fmt:message key="actions.prefs" />
      </a> -
    </wiki:CheckRequestContext>
    </wiki:CheckRequestContext>

  </wiki:UserCheck>


