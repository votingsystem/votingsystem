<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
    <div class="pageContentDiv">
        <h1 class="pageHeader" style="text-align: center;">Transacciones</h1>
        <div class="text-justify" style="margin: 0 auto 0 auto; font-size: 1.2em;">
            <h3 class="pageHeader">Preguntas frecuentes</h3>
            <p><b>¿Qué es significa que un ingreso tenga asociada una etiqueta?</b> Que el receptor sólo puede emplear el
            importe en productos o servicios asociados a esa etiqueta.
            </p>
            <p><b>¿Qué es la etiqueta WILDTAG?</b> Es la etiqueta asociada a '<b>gastos generales</b>', etiqueta comodín,
            este dinero se puede emplear en cualquier etiqueta.
            </p>
            <p><b>¿Qué pasa cuando se recibe un ingreso asociado a una etiqueta?</b>
            Se comprueba si durante el ciclo semanal el usuario a extradido de su cuenta de '<b>gastos generales</b>' algún importe
            asociado a esa etiqueta, en caso afirmativo se reingresa el importe gastado y
            el sobrante se ingresa en la cuenta que el usuario tiene asociada a esa etiqueta
            </p>
            <p><b>¿Qué pasa cuando se extrae dinero asociándolo a una etiqueta?</b>
            Se comprueba si el usuario tiene disponible sufciente en su cuenta asociada  a esa etiqueta y en caso
            afirmativo se extrae el dinero de esa cuenta, si el disponible en la cuenta es inferior al importe se extrae
            lo que falte de la cuenta asociada a '<b>gastos generales</b>' (si se dispone de saldo suficiente)
            </p>
            <p><b>¿Qué es un ingreso 'caducable'?</b> Es un ingreso cuyo importe debe gastarse antes de las 24:00 horas
            del domingo de la semana en curso, lo que no se gaste se ingresa en el sistema. Los ingresos <b>caducables</b>
            tienen que estar asociados obligatoriamente a una etiqueta, la unica etiqueta que no permite movimientos
            caducables es la etiqueta WILDTAG (la etiqueta asociada a <b>gastos generales</b>).
            </p>
        </div>
    </div>
</body>
</html>

