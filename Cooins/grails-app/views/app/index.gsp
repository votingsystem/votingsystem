<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
    <div class="pageContentDiv">
        <div class="text-justify" style="margin: 0 auto 0 auto; font-size: 1.2em;">
            <h3 class="pageHeader">cooins, moneda electrónica basada en arquitecturas de clave pública.</h3>
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
            <p><b>¿Qué es un cooin?</b> Es un certificado electrónico anónimo (sin información que pueda relacionarlo con el
            propietario) que tiene asociado un valor y que permite <b>intercambios monetarios anónimos de forma segura</b>.</p>
            <p><b>¿Cómo se produce un intercambio de cooins?</b>
                El intercambio anónimo de moneda se puede producir de dos maneras:
            <ul>
                <li>
                    El usuario que desea hacer una transacción utiliza los <b>cooins</b> para firmar electrónicamente un
                documento con los datos del beneficiario. El valor del intercambio corresponde al valor asociado al
                    <b>cooin</b> con el que se firma el traspaso. <b>De esta forma el intercambio quedará reflejado en los
                registros del sistema y la identidad del receptor será pública, la identidad del pagador seguirá siendo
                anónima</b>
                </li>
                <li>
                    Mediante el intercambio directo de <b>cooins</b>. Un usuario puede dar directamente a otro un <b>cooin</b>
                    que no haya sido utilizado para que el receptor lo emplee cuando estime oportuno.<b> De esta forma el sistema
                nunca puede saber que se ha producido el intercambio. La identidad del receptor y pagador permanecen
                anónimas</b>
                </li>
            </ul>
        </p>
            <p><b>¿Cómo se que no van a utilizar los mismos cooins en repetidas ocasiones?</b> No puedes saberlo, pero en
            caso de duda cada intercambio lleva incorporado un Sello de Tiempo. Si aparecen varios documentos firmados con
            el mismo cooin sólo tiene valor el que tiene el <b>Sello de Tiempo</b> más antiguo.</p>
            <p><b>¿Quien puede emitir cooins?</b> El sistema es libre, cualquiera puede instalarlo. Depende del usuario el
            confiar o no en los cooins emitidos por una determinada organización. Puede ser utilizado por un centro
            comercial para cupones y descuentos, para la gestión (y control) de entradas en eventos, como moneda alternativa...</p>
            <p><b>¿Por qué llevan asociados algunos ingresos una etiqueta?</b> Los ingresos que llevan asociados una <b>etiqueta</b>
            sirven para que el destinatario sólo pueda emplear ese dinero en servicios que tengan asociada esa <b>etiqueta</b>.</p>
            <p><b>¿Por qué llevan asociados algunos ingresos una fecha de caducidad?</b> Cuando se marca un ingreso como
            <b>caducable</b> la fecha de caducidad corresponde a las <b>24:00 horas del domingo de la semana en curso</b>.
            Los ingresos que llevan asociados <b>una fecha de caducidad</b> sirven para obligar a los usuarios a que
            empleen ese dinero antes de la <b>fecha de caducidad</b> del mismo, en caso contrario ese dinero pasará al sistema.
            </p>
            <p><b>¿Por qué puede interesarme retirar efectivo?</b> Los pagos pueden ser anónimos o no. Los pagos
            <b>NO anónimos</b> quedan reflejados en un documento firmado que identifica de forma inequívoca al pagador.
            Los pagos <b>ANONIMOS</b> están efectuados con certificados electrónicos que no tienen ninguna información del pagador,
            estos certificados se solicitan con un documento firmado en el que si que figura la identidad. Si
            se realiza un pago anónimo sin retirar efectivo existe la posibilidad de que se pueda relacionar un certificado
            anónimo con su solicitud por la proximidad de los sellos de tiempo. Si alguien hace un pago anónimo de 132,47 euros
            a las 7:45 quedará también reflejado que entre las 7:00 y la 8:00 alguien perfectamente identificado solicitó 132,47 euros.
            En cambio si se retiran 20 euros en efectivo para gastarlos en otro momento se evitan ese tipo de relaciones.
            </p>
        </div>

        <div class="text-justify" style="margin: 20px auto ;font-size: 1.2em;">
            <h3 class="pageHeader">Retirada de efectivo</h3>
            La retirada de efectivo es una variedad del pago anónimo en la que el usuario se descarga la moneda, con las
            mismas consecuencias que acarrea operar con una moneda física. Si la pierdes no hay forma de recuperarla.
            <p><b>Retirada de efectivo con fecha de límite.</b> Cuando se retira efectivo con fecha límite, el usuario
            se compromete a gastar ese efectivo antes del domingo a las 24:00 horas. El receptor de un pago anónimo
            con un <b>cooin</b> con fecha límite también debe gastar ese dinero antes del límite.
            </p>
            <p><b>Retirada de efectivo sin fecha de límite.</b> La duración máxima de un cooin sin fecha límite es un año.
            Si el usuario no desea consumir ese cooin puede cangearlo sin problemas siempre antes de que supere su
            fecha de caducidad.
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
                    <li>Las <b>fecha de  caducidad</b> de los cooins emitidos. Pueden emitirse cooins que tengan una vida de
                    una hora, un día, un mes, un año ...</li>
                    <li>Añadiendo <b>etiquetas</b> a los cooins que hagan que sólo puedan ser empleados en los
                    <b>productos/proyectos/eventos/establecimientos</b> que tengan asociada esa etiqueta.</li>
                </ul>
            </p>
        </div>

    </div>
</body>
</html>

