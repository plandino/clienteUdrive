.. _dependenciasycompilacion:

Documentacion de las librerias y la compilación
================================================

La aplicacion usa distintas librerias propias y externas de Android Studio, las cuales pueden ser facilmente instaladas mediante el SDK Manager de Android Studio o a través de las dependencias de Gradle.

SDK Manager
---------------------------------------------------------------------

Se puede acceder al SDK Manager de Android Studio mediante el icono ( |sdkmanager| ) en la barra de tareas o mediante "Tools" --> "Android" --> "SDK Manager".

.. |sdkmanager| image:: _static/sdkmanager.png

Dentro del SDK Manager hay que acceder al menu en la parte izquierda "Appearance & Behavior" --> "System Settings" --> "Android SDK".

Dentro de la solapa SDK Platforms tiene que estar descargada y actualizada la versión ``Android 5.1.1.``.

Dentro de la solapa SDK Tools, tienen que estar descargados los componentes:

.. code-block:: bash

	Android SDK Platform-Tooles 		23.0.1
	Android Support Repository, rev 24 	24.0.0
	Android Support Library, rev 23.1	23.1.0
	Google Play Services, rev 27 		27.0.0
	Google Repository, rev 22		22.0.0

Gradle
---------------------------------------------------------------------

Para configurar Gradle hay que acceder al archivo de configuracion "build.gradle (Module:app)", desde el Android Studio y agregar en las dependencias las siguientes librerias:

.. code-block:: bash

    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:22.2.1'
    compile 'com.loopj.android:android-async-http:1.4.4'
    compile 'com.google.android.gms:play-services:8.1.0'
    compile 'com.google.android.gms:play-services-maps:8.1.0'
    compile 'org.apache.httpcomponents:httpcore:4.4.1'
    compile 'com.github.shell-software:fab:1.1.2'
    compile 'com.github.rahatarmanahmed:circularprogressview:2.4.0'


Luego de incluir nuevas librerias en el archivo de configuracion de Gradle hay que sincronizar Gradle con el proyecto, haciendo click en el mensaje emergente "Sync now" que aparece arriba o haciendo click en el icono "Sync Project with Gradle Files" en la barra de tareas.

Compilación
---------------------------------------------------------------------

Para compilar simplemente haga click en el icono ( |play| ). 

.. |play| image:: _static/play.png

Emulación
---------------------------------------------------------------------

Para probar la aplicación usamos nuestros teléfonos, para poder realizar pruebas rapidas.

Usamos un telefono conectado mediante un cable USB con la computadora, para que Android Studio instale y ejecute la aplicación automáticamente al compilar y ejecutar el proyecto.

Para permitir que esto suceda debe:

En el teléfono
*********************************************************************

Setear el teléfono en modo Debugeable. Debe ir a "Ajustes" --> "Acerca del dispositivo" y hacer click 7 veces sobre el "Número de compilación", lo que activa las Opciones de desarrollador en el teléfono. Dentro de las "Opciones de desarrollador", debe activar la Depuración de USB.

En la computadora
*********************************************************************

Debe ejecutar:

.. code-block:: bash

    sudo gedit /etc/udev/rules.d/51-android.rules

Luego debe introducir la linea:

.. code-block:: bash

    SUBSYSTEM=="usb", ATTR{idVendor}=="0bb4", MODE="0666", GROUP="plugdev"

Donde el idVendor depende del fabricante del teléfono. Para consultar los distintos id y tener una explicación mas detallada de este procedimiento acceda `aquí <http://developer.android.com/intl/es/tools/device.html>`_  .

Finalmente ejecute:

.. code-block:: bash

    sudo chmod a+r /etc/udev/rules.d/51-android.rules

