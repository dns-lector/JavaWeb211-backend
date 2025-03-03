<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>JSP</h1>
        <h2>Вирази</h2>
        <%= 2 + 3 %>
        <form action="product" method="post" enctype="multipart/form-data">
            <input name="field1" value="value 1"/>
            <input type="file" name="file1"/>
            <button>send</button>
        </form>
    </body>
</html>
