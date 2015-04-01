<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
</head>
<body>
<div class="pageContentDiv">
    <h1 class="pageHeader" style="text-align: center;">Cuentas Corrientes de usuario</h1>
    <div class="text-justify" style="margin: 0 auto 0 auto; font-size: 1.2em;">
        <p>
            En el momento en el que se da de alta en el sistema a todo usuario se le asocia un código <b>IBAN</b>, ese código
        tiene asociada una cuenta para <b>gastos libres</b> y cuentas para cada tipo de etiqueta. Es decir, un usuario tiene
        un codigo <b>IBAN</b> que tiene asociadas tantas cuentas como etiquetas.
        </p>
        <p>Cuando un usuario <b>RECIBE</b> un <b>ingreso</b> el importe se guarda en la cuenta para <b>gastos libres</b>
        asociada al código <b>IBAN</b> del usuario</p>
        <p>Cuando un usuario <b>RECIBE</b> un <b>ingreso CON etiqueta</b> el importe se guarda en la cuenta para esa etiqueta
        asociada al código <b>IBAN</b> del usuario</p>
        <p>Cuando un usuario <b>HACE</b> un <b>ingreso</b> el importe se extrae de la cuenta para <b>gastos libres</b>
            asociada al código <b>IBAN</b> del usuario</p>
        <p>Cuando un usuario <b>HACE</b> un <b>ingreso CON etiqueta</b> el importe se extrae de la cuenta para esa etiqueta
        asociada al código <b>IBAN</b> del usuario, si el importe excede el contenido de la cuenta para la <b>etiqueta</b>
        lo que falta se extrae de la cuenta para <b>gastos libres</b> asociada a ese <b>IBAN</b></p>
    </div>
</div>
</body>
</html>

