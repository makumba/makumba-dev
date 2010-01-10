<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<fmt:setBundle basename="templates.default"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html id="top" xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>
    <fmt:message key="view.title.view">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
  </title>
  <wiki:Include page="commonheader.jsp"/>
  <wiki:CheckVersion mode="notlatest">
    <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckVersion>
  <wiki:CheckRequestContext context="diff|info">
    <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckRequestContext>
  <wiki:CheckRequestContext context="!view">
    <meta name="robots" content="noindex,follow" />
  </wiki:CheckRequestContext>
</head>

<body class="view">

<div id="wikibody" class="${prefs.Orientation}">
 
  <wiki:Include page="Header.jsp" />

<%-- manu: different class for the content div if there is no menu for the page (i.e. if it is not in a category) --%>
<c:set var="currentCategory"><wiki:Plugin plugin="InsertCategoryMenu" args="showCurrentCategory='true'"/></c:set>
<c:set var="currentCategoryMenu"><wiki:Plugin plugin="InsertCategoryMenu" args="showCurrentCategoryMenu='true'"/></c:set>
<c:set var="contentDivClass"><c:choose><c:when test="${empty currentCategoryMenu}">noSideMenu</c:when><c:otherwise>sideMenu</c:otherwise></c:choose></c:set>

  <div id="content" class="${contentDivClass}">

    <div id="page">
      <wiki:Include page="PageActionsTop.jsp"/>
      <wiki:Content/>
      <wiki:Include page="PageActionsBottom.jsp"/>
    </div>

    <wiki:Include page="Favorites.jsp"/>

	<div class="clearbox"></div>
  </div>

  <wiki:Include page="Footer.jsp" />

</div>

</body>
</html>