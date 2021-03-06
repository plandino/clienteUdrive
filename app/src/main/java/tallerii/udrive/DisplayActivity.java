package tallerii.udrive;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Muestra el archivo descargado en caso de que se descargue un archivo de texto o una foto.
 * Ademas permite que se seleccione una nueva foto, en caso de querer actualizar la foto de perfil.
 */
public class DisplayActivity extends AppCompatActivity {

    private boolean esEditable;
    private String pathFotoNueva = "";
    private String pathFotoVieja = "";
    private static final int PICKFILE_RESULT_CODE = 1;
    private boolean cambieFoto = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("DISPLAY: ", "Se inicio la DisplayACtivity.");

        super.onCreate(savedInstanceState);

        esEditable = getIntent().getBooleanExtra("editable", false);

        setContentView(R.layout.activity_display);

        String extensionArchivo = getIntent().getStringExtra("extensionArchivo");
        if(extensionArchivo == null){
            extensionArchivo = "noSeObtuvo";
        }
        String filePath = getIntent().getStringExtra("filePath");
        pathFotoVieja = filePath;
        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
        TextView textView = (TextView) findViewById(R.id.textview);
        StringBuilder stringBuffer = new StringBuilder();

        Log.d("DISPLAY: ", "La extenision del archivo es: " + extensionArchivo);
        Log.d("DISPLAY: ", "El path interno donde se guardo el archivo es: " + filePath);
        Log.d("DISPLAY: ", "El nombre del archivo es: " + nombreArchivo);


        // Si descargo un archivo con las siguientes extensiones es una foto.
        if(extensionArchivo.equals("jpg") || extensionArchivo.equals("png") || extensionArchivo.equals("jpeg") || (esEditable)){

            Log.i("DISPLAY: ", "Voy a mostrar una foto en la DisplayACtivity.");

            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);

            // Sino, reviso si es un archivo de texto
        } else if(extensionArchivo.equals("txt") ){

            Log.i("DISPLAY: ", "Voy a mostrar un texto en la DisplayACtivity.");

            try {
                File file = new File(filePath);
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String inputString;

                while ((inputString = inputReader.readLine()) != null) {
                    if(stringBuffer.length() < 4194304){
                        stringBuffer.append(inputString);
                    }
                }
                textView.setText(stringBuffer.toString());
            } catch (IOException e) {

                Log.e("DISPLAY: ", "No pude abrir el archivo de texto en la DisplayActivity.");
                Log.e("DISPLAY: ", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        if(esEditable){
            getMenuInflater().inflate(R.menu.menu_foto_perfil, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_all, menu);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // Menu para el movimiento normal por carpetas y archivos
            case R.id.editar:
                Log.i("DISPLAY: ", "Hice click en el boton de buscar archivo.");
                seleccionarArchivo();
                return true;
            case android.R.id.home:
                Log.i("DISPLAY: ", "Hice click en el boton para atras en la barra.");
                this.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Crea un menu para seleccionar que foto subir.
     */
    private void seleccionarArchivo(){

        Log.i("DISPLAY: ", "Voy a seleccionar un archivo para subir.");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i("DISPLAY: ", "La activity anterior me dio un resultado.");
        Log.d("DISPLAY: ", "El request code es: \"" + requestCode + "\".");

        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode==RESULT_OK){

                    Log.i("DISPLAY: ", "El resultado de la activity es OK.");
                    Log.i("DISPLAY: ", "Volvi de buscar una nueva foto de perfil.");

                    if (data == null){
                        Log.i("DISPLAY: ", "No hay data en el resultado de la activity.");
                        return;
                    }

                    Uri selectedImageUri = data.getData();

                    // Tengo que obtener el path del archivo. Uso el FilePathGetter para traducir
                    // de una Uri a un file path absoluto
                    pathFotoNueva = FilePathGetter.getPath(getApplicationContext(), selectedImageUri);
                    Log.d("DISPLAY: ", "El path de la foto de perfil seleccionada es: " + pathFotoNueva);

                    cambieFoto = true;

                    Bitmap bitmap = BitmapFactory.decodeFile(pathFotoNueva);
                    ImageView imageView = (ImageView) findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);

                    Log.i("DISPLAY: ", "Mostre la nueva foto de perfil.");
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {

        Log.i("DISPLAY: ", "Aprete para atras.");
        if((esEditable) && (cambieFoto)){
            confirmarNuevafoto();
        } else {
            finish();
        }
    }

    /**
     * Muestra un AlertDialog preguntando al usuario si esta seguro de cambiar la foto de perfil.
     */
    private void confirmarNuevafoto(){
        Log.i("DISPLAY: ", "Voy a crear un Alert Dialog preguntando al usuario si realmente desea cambiar la foto de perfil.");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Cambiar foto");
        alert.setMessage("¿Esta seguro que desea subir esta nueva foto de perfil?");

        // El boton Sobreescribir sube el archivo igual
        alert.setPositiveButton("Si", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d("DISPLAY: ", "Hice click en Si.");
                Log.i("DISPLAY: ", "Voy a cambiar la foto en PerfilActivity.");
                Log.i("DISPLAY: ", "Le agrego al Intent de regreso el path a la foto nueva: \"" + pathFotoNueva + "\".");
                Log.i("DISPLAY: ", "Le agrego al Intent de regreso que SI es una nueva foto: \"" + true + "\".");

                Intent returnIntent = new Intent();
                returnIntent.putExtra("pathFotoNueva", pathFotoNueva);
                returnIntent.putExtra("nuevaFoto", true);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });

        // Un boton para cancelar, que no hace nada (se cierra la ventana emergente)
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.i("DISPLAY: ", "Hice click en Cancelar.");
                Log.i("DISPLAY: ", "No voy a cambiar la foto en PerfilActivity.");
                Log.i("DISPLAY: ", "Le agrego al Intent de regreso el path a la foto nueva: \"" + pathFotoNueva + "\".");
                Log.i("DISPLAY: ", "Le agrego al Intent de regreso que NO es una nueva foto: \"" + false + "\".");

                Intent returnIntent = new Intent();
                returnIntent.putExtra("pathFotoNueva", pathFotoVieja);
                returnIntent.putExtra("nuevaFoto", false);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });

        alert.show();

        Log.i("DISPLAY: ", "Mostre un dialogo de advertencia para cambiar la foto de perfil.");
    }
}