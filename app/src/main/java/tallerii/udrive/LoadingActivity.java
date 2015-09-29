package tallerii.udrive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

// ACTIVITY SIN USAR POR AHORA
public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        cambiarAElegir();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_all, menu);

        return true;
    }


    private void cambiarAElegir() {

        // create an Intent to take you over to a new DetailActivity
        Intent elegirSesionIntent = new Intent(this, ElegirSesionActivity.class);
        // start the next Activity using your prepared Intent
        startActivity(elegirSesionIntent);
        finish();

    }
}
