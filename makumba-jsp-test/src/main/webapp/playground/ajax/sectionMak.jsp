<%@ taglib uri="http://www.makumba.org/presentation" prefix="mak" %>
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Iterator"%>
<html>
<head>
<title>Section</title>
</head>

<body>
<button onClick="mak.event('myEvent1')">event1</button>
<button onClick="mak.event('myEvent2')">event2</button>
<button onClick="mak.event('myEvent3')">event3</button>
<br/><br/>

Div1: <mak:section name="div1" reloadOn="myEvent1">div1 content</mak:section><br/>
Div2: <mak:section name="div2" showOn="myEvent2">div2 content</mak:section><br/>
Div3: <mak:section name="div3" hideOn="myEvent3">div3 content</mak:section><br/>
Div4: <mak:section name="div4" reloadOn="myEvent3"><mak:list from="test.Person p"> <mak:value expr="p.indiv.name"/></mak:list></mak:section><br/>

<br/>
<button onClick="mak.event('myEvent4')">event4</button>
<br/><br/>
<mak:list from="test.Person p" id="2">
<mak:section name="divList" reloadOn="myEvent4"><mak:value expr="p.indiv.name" /></mak:section>
</mak:list>

<br/>
<button onClick="mak.event('myEvent5')">event5</button>
<br/><br/>
<mak:list from="test.Person p" id="3">
<button onClick="mak.event('myEvent5', '<mak:value expr="p.indiv.name" />')">event5 iteration <mak:value expr="p.indiv.name" /></button>
<mak:section name="divListId" reloadOn="myEvent5" iterationExpr = "p.indiv.name"><mak:value expr="p.indiv.name" /></mak:section>
</mak:list>

</body>
</html>