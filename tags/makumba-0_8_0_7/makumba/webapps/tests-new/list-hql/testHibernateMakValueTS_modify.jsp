<%@page contentType="text/html"%>
<%@page pageEncoding="utf-8"%>
<html>
<head><title>Field types</title></head>
<body>

<%@taglib uri="http://www.makumba.org/list-hql" prefix="mak" %>


<mak:object from="test.Person p join p.indiv i" where="i.name='john'">
  testTS_modify!<mak:value expr="p.TS_create" />!endTS_modify<br>  
</mak:object>

</body>
</html>