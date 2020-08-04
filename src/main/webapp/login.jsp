<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"     uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt"   uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"    uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="s"     uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form"  uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec"   uri="http://www.springframework.org/security/tags" %>
<c:set var="pageTitle" value="FAZEMU" />
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="<c:url value="/webjars/bootstrap/4.1.3/css/bootstrap.min.css" />">
        <link rel="stylesheet" href="<c:url value="/webjars/font-awesome/5.2.0/css/all.min.css" />">
        <title>${pageTitle}</title>
    </head>
    <body class="bg-secondary">
        <div class="container mt-5">
            <div class="card text-center">
                <div class="card-header">
                    <h2><i class="fas fa-file-invoice-dollar fa-lg fa-fw"></i> ${pageTitle}</h2>
                </div>
                <div class="card-body">
                    <form action="/login" method="POST" class="form-signin">
                        <sec:csrfInput />
                        <c:if test="${param.error != null}">
                            <div class="alert alert-danger">
                                Login inv&aacute;lido!
                            </div>
                        </c:if>
                        <div class="form-group">
                            <label for="username" class="sr-only">Usu&aacute;rio</label>
                            <div class="input-group">
                                <div class="input-group-prepend">
                                    <div class="input-group-text"><i class="fas fa-user fa-lg fa-fw"></i></div>
                                </div>
                                <input type="text" id="username" name="username" class="form-control" placeholder="Usuario" required autofocus>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="password" class="sr-only">Senha</label>
                            <div class="input-group">
                                <div class="input-group-prepend">
                                    <div class="input-group-text"><i class="fas fa-key fa-lg fa-fw"></i></div>
                                </div>
                                <input type="password" id="password" name="password" class="form-control" placeholder="Senha"> <%-- SEM REQUIRED POR ORA --%>
                            </div>
                        </div>
                        <button class="btn btn-lg btn-primary btn-block" type="submit"><i class="fas fa-sign-in-alt fa-lg"></i> Efetuar login</button>
                    </form>
                </div>
            </div>
        </div>
    </body>
</html>