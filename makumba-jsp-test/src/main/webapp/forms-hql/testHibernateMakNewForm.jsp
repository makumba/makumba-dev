<%@page contentType="text/html"%>
<%@page pageEncoding="utf-8"%>
<html>
<head><title>Field types</title></head>
<body>

<%@taglib uri="http://www.makumba.org/view-hql" prefix="mak" %>

testNewFormStart!<mak:newForm type="test.Person" action="testHibernateMakNewForm.jsp" method="post" clientSideValidation="false">!endNewFormStart
        testName!<mak:input name="indiv.name"/>!endName
        testSubmit!<input type="submit">!endSubmit
testNewFormEnd!</mak:newForm>!endNewFormEnd

</body>
</html>