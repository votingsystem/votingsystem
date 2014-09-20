<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <style>

    </style>
</head>
<body>
    <div class="pageContentDiv" style="max-width: 1300px; margin: 0px auto 0px auto; padding: 0px 30px 0px 30px;">
        <div class="text-justify" style="margin: 0 auto 0 auto; font-size: 1.2em;">
            <h3 class="pageHeader">Vickets, moneda electrónica basada en arquitecturas de clave pública.</h3>
            <ul>
                <li>Con <b>Autoridad Certificadora</b> y <b>Servidor de Sellos de Tiempo</b> propios.</li>
                <li>Operativas transparentes y seguras.</li>
                <li>Todos los intercambios de moneda quedan reflejados en documentos <b>firmados electrónicamente y con sello de tiempo</b>
                permitiendo la trazabilidad de las operaciones.</li>
                <li>Permite <b>intercambios de moneda anónimos</b>.</li>
                <li>Auditorías remotas de millones de transaciones en minutos.</li>
            </ul>
        </div>

        <div class="text-justify" style="margin: 20px auto 0 auto;font-size: 1.2em;">
            <h3 class="pageHeader">Preguntas frecuentes</h3>
            <p><b>¿Qué es un Vicket?</b> Es un certificado electrónico anónimo (sin información que pueda relacionarlo con el
            propietario) que tiene asociado un valor y que permite <b>intercambios monetarios anónimos de forma segura</b>.</p>
            <p><b>¿Cómo se produce un intercambio de Vickets?</b> El usuario que desea hacer una transacción utiliza los
            Vickets para firmar electrónicamente un documento con los datos del beneficiario.
            El valor del intercambio corresponde al valor asociado al Vicket con el que se firma el documento.</p>
            <p><b>¿Cómo se que no van a utilizar los mismos Vickets en repetidas ocasiones?</b> No puedes saberlo, pero en
            caso de duda cada intercambio lleva incorporado un Sello de Tiempo. Si aparecen varios documentos firmados con
            el mismo Vicket sólo tiene valor el que tiene el <b>Sello de Tiempo</b> más antiguo.</p>
            <p><b>¿Quien puede emitir Vickets?</b> El sistema es libre, cualquiera puede instalarlo. Depende del usuario el
            confiar o no en los Vickets emitidos por una determinada organización. Puede ser utilizado por un centro
            comercial para cupones y descuentos, para la gestión (y control) de entradas en eventos, como moneda alternativa...</p>
            <p><b>¿Por qué llevan asociados algunos ingresos una etiqueta?</b> Los ingresos que llevan asociados una <b>etiqueta</b>
            sirven para que el destinatario sólo pueda emplear ese dinero en servicios que tengan asociada esa <b>etiqueta</b>.</p>
            <p><b>¿Por qué llevan asociados algunos ingresos una fecha de caducidad?</b> Los ingresos que llevan asociados
            <b>una fecha de caducidad</b> sirven para obligar a los usuarios a que empleen ese dinero antes de la <b>fecha
             de caducidad</b> del mismo. En caso contrario el dinero con <b>fecha de caducidad</b> que no haya sido utilizado
            pasará a ser propiedad del sistema.
            </p>
            <p><b>¿Por qué puede interesarme retirar efectivo?</b> Los pagos pueden ser anónimos o no. Los pagos
            <b>NO anónimos</b> quedan reflejados en un documento firmado que identifica de forma inequívoca al pagador.
            Los pagos <b>ANONIMOS</b> están efectuados con certificados electrónicos que no tienen ninguna información del pagador,
            estos certificados se solicitan con un documento firmado en el que si que figura la identidad. Si
            se realiza un pago anónimo sin retirar efectivo existe la posibilidad de que se pueda relacionar un certificado
            anónimo con su solicitud por la proximidad de los sellos de tiempo. Si alguien hace un pago anónimo de 132,47 euros
            a las 7:45 quedará también reflejado que a las 7:45 alguien perfectamente identificado solicitó 132,47 euros.
            En cambio si se retiran 20 euros en efectivo para gastarlos en otro momento se evitarán ese tipo de relaciones.
            </p>
        </div>

        <div class="text-justify" style="margin: 20px auto 300px auto;font-size: 1.2em;">
            <h3 class="pageHeader">Alquimia digital. Estímulo de actividad mediante monedas electrónicas</h3>
            <p><b>La moneda electrónica sirve para poder añadir nuevas funcionalidades a la moneda tradicional</b></p>
            <p>Lo que necesitan las empresas es demanda efectiva, es decir, que sus potenciales compradores tengan más
            ingresos para gastar. Es solo el incremento de gasto por parte de los consumidores lo que puede hacer que
            aumente la actividad.</p>
            <p>Es una forma sencilla, segura y transparente de poner dinero en circulación permitiendo establecer las dinámicas que se
            estimen oportunas modificando parámetros como:
                <ul>
                    <li>Las <b>fecha de  caducidad</b> de los Vickets emitidos. Pueden emitirse Vickets que tengan una vida de
                    una hora, un día, un mes, un año ...</li>
                    <li>Añadiendo <b>etiquetas</b> a los Vickets que hagan que sólo puedan ser empleados en los
                    <b>productos/proyectos/eventos/establecimientos</b> que tengan asociada esa etiqueta.</li>
                </ul>
            </p>
        </div>

    </div>
</body>
</html>
<asset:script>

</asset:script>

