package tallerii.udrive;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ElegirSesionActivity extends AppCompatActivity implements View.OnClickListener{

    Button nuevoUsuarioButton;
    Button iniciarSesionButton;

    private int requestCodeUno = 1;

    SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("ELEGIR_SESION: ", "Se inicio la ElegirSesionActivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegir_sesion);

        // Boton para registrar un usuario nuevo
        nuevoUsuarioButton = (Button) findViewById(R.id.registrarse);
        nuevoUsuarioButton.setOnClickListener(this);

        // Boton para iniciar sesion con un usuario viejo
        iniciarSesionButton = (Button) findViewById(R.id.iniciar_sesion);
        iniciarSesionButton.setOnClickListener(this);

        // Accedo a los datos guardados
        mSharedPreferences = getSharedPreferences(MyDataArrays.SESION_DATA, MODE_PRIVATE);

        // Leo los datos guardados
        String name = mSharedPreferences.getString(MyDataArrays.USERNAME, "");
        String token = mSharedPreferences.getString(MyDataArrays.TOKEN, "");

        if( (token.length() > 0 ) && (name.length() > 0 ) ){
            pasarAlMain(token, name);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ip, menu);
        return true;
    }

    @Override
    public void onClick(View v) {

        // Me fijo que boton apretaron
        switch(v.getId()) {
            case R.id.registrarse:

                Log.i("ELEGIR_SESION: ", "Aprete el boton para registrar un nuevo usuario, cambio a " +
                        "la activity correspondiente.");

                // Creo un Intent para pasar a la RegistrarseActivity
                Intent registrarseIntent = new Intent(this, RegistrarseActivity.class);
                // Arranco la activity con el Intent
                startActivityForResult(registrarseIntent,requestCodeUno);
                break;
            case R.id.iniciar_sesion:

                Log.i("ELEGIR_SESION: ", "Aprete el boton para iniciar sesion con un usuario existente" +
                        ", cambio a la activity correspondiente.");

                // Creo un Intent para pasar a la IniciarSesionActivity
                Intent iniciarSesionIntent = new Intent(this, IniciarSesionActivity.class);
                // Arranco la activity con el Intent
                startActivityForResult(iniciarSesionIntent,requestCodeUno);
                break;
            default:
                throw new RuntimeException("Unknow button ID");
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // Menu para el movimiento normal por carpetas y archivos
            case R.id.ingresar_ip:
                Log.i("ELEGIR_SESION: ", "Hice click en el boton para ingresar ip.");
                ingresarIP();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void ingresarIP(){
        Log.i("ELEGIR_SESION: ", "Voy a crear un alert dialog para que ingrese su IP.");

        // Muestro una ventana emergente para que introduzca el nombre de la carpeta a crear
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Configurar IP");
        alert.setMessage("Introduzca su IP por favor");

        final EditText ip = new EditText(this);

        // Luego creo un LinearLayout y los configuro para ordenar todos los items anteriores y
        // mostrarlos bien en el AlertDialog TODO: unificar esto
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float pxTodp = 300 / getResources().getDisplayMetrics().density;
        layout.setPadding((int) pxTodp, 0, (int) pxTodp, 0);

        layout.addView(ip);
        alert.setView(layout);

        // El boton "Crear" crea la carpeta
        alert.setPositiveButton("Listo!", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("ELEGIR_SESION: ", "Hice click en Listo!. La IP introducida es: \"" + ip.getText().toString() + "\".");
                MyDataArrays.setIP(ip.getText().toString());
//                agregarCarpeta(ip.getText().toString());
            }
        });

        // Un boton de cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("ELEGIR_SESION: ", "Hice click en Cancelar.");
            }
        });

        alert.show();

        Log.i("ELEGIR_SESION: ", "Mostre un dialogo para que introduzca la IP.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Me fijo con que numero mande a iniciar la Activity
        if (requestCode == requestCodeUno) {
            // Me fijo que el resultado sea OK
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    /**
     * Paso a la MainActivity luego de recibir el token de la sesion.
     * Le mando los siguientes parametros a traves del Intent.
     *
     * @param token que recibi desde el servidor
     * @param user username del usuario con el que inicie sesion
     */
    private void pasarAlMain(String token, String user){

        Log.i("ELEGIR_SESION: ", "Voy a pasar a la MainActivity.");

        // Creo un Intent para pasar al main
        Intent mainIntent = new Intent(this, MainActivity.class);


        Log.d("ELEGIR_SESION: ", "Mando al MainActivity el user: \"" + user + "\".");
        Log.d("ELEGIR_SESION: ", "Mando al MainActivity el token: \"" + token + "\".");

        // Agrego la informacion que quiero al Intent
        mainIntent.putExtra("username", user);
        mainIntent.putExtra("token", token);

        // Inicio la actividad con el Intent
        startActivity(mainIntent);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}