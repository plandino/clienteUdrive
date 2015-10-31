package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class ElegirSesionActivity extends AppCompatActivity implements View.OnClickListener{

    Button nuevoUsuarioButton;
    Button iniciarSesionButton;
    private int requestCodeUno = 1;

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all, menu);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Me fijo con que numero mande a iniciar la Activity
        if (requestCode == requestCodeUno) {
            // Me fijo que el resultado sea OK
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }
}