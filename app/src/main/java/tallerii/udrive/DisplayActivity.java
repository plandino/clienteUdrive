package tallerii.udrive;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Esta clase sirve para mostrar el archivo descargado en caso de que se descargue
 * un archivo de texto o una foto. Los demas archivos no se muestran.
 */
public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("DISPLAY: ", "Se inicio la DisplayACtivity.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        String extensionArchivo = getIntent().getStringExtra("extensionArchivo");
        String filePath = getIntent().getStringExtra("filePath");
        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
        TextView textView = (TextView) findViewById(R.id.textview);
        StringBuilder stringBuffer = new StringBuilder();

        Log.d("DISPLAY: ", "La extenision del archivo es: " + extensionArchivo);
        Log.d("DISPLAY: ", "El path interno donde se guardo el archivo es: " + filePath);
        Log.d("DISPLAY: ", "El nombre del archivo es: " + nombreArchivo);


        // Si descargo un archivo con las siguientes extensiones es una foto.
        if(extensionArchivo.equals("jpg") || extensionArchivo.equals("png") || extensionArchivo.equals("jpeg")){

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
}