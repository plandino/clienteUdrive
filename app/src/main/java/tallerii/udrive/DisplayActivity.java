package tallerii.udrive;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        String extensionArchivo = getIntent().getStringExtra("extensionArchivo");
        String filePath = getIntent().getStringExtra("filePath");
        String nombreArchivo = getIntent().getStringExtra("nombreArchivo");
        TextView textView = (TextView) findViewById(R.id.textview);
        StringBuffer stringBuffer = new StringBuffer();

        if(extensionArchivo.equals("jpg") || extensionArchivo.equals("png") || extensionArchivo.equals("jpeg")){
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);
        } else if(extensionArchivo.equals("txt") ){

            try {

                String path = filePath;
                File file = new File(path);
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String inputString;

                while ((inputString = inputReader.readLine()) != null) {
                    if(stringBuffer.length() < 4194304){
                        stringBuffer.append(inputString + "\n");
                    }
                }
                textView.setText(stringBuffer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}