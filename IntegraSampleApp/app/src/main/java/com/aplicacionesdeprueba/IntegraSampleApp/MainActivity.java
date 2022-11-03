package com.aplicacionesdeprueba.IntegraSampleApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    final String RECMAT_APPNAME = "es.suma.matmatcher";
    final String RECMAT_ACTIVITYNAME = "es.suma.matmatcher.CameraActivity";

    Context appCtx;

    ImageView m_imageView;
    TextView m_plateTxt;
    Button m_launchButton;

    private final ActivityResultLauncher<Intent> m_configResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if ( result.getResultCode() == RESULT_CANCELED ){
                    return ;
                }
                boolean detected = result.getData().getBooleanExtra("detected", false);
                if (!detected){
                    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                    Bitmap empty = Bitmap.createBitmap(m_imageView.getWidth(), m_imageView.getHeight(), conf);
                    m_imageView.setImageBitmap(empty);
                    m_plateTxt.setText("-");
                    Toast.makeText(appCtx, "No se reconocieron matriculas.",
                            Toast.LENGTH_SHORT).show();

                    return;
                }

                String plate = result.getData().getStringExtra("plate");
                double confidence = result.getData().getDoubleExtra ("confidence", 0);
                String w_plate = String.format("%s", plate);
                m_plateTxt.setText(w_plate);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appCtx = this.getApplicationContext();
        PackageManager pm = appCtx.getPackageManager();
        boolean isInstalled = isPackageInstalled(RECMAT_APPNAME, pm);

        m_imageView = findViewById(R.id.imageView);
        m_plateTxt = findViewById(R.id.plateTxt);
        m_launchButton = findViewById(R.id.launchButton);
        m_launchButton.setEnabled(isInstalled);
        m_launchButton.setText("Detectar matricula");
        m_launchButton.setOnClickListener(view -> {
            Button theButton = (Button) view;
            String text = theButton.getText().toString();
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(RECMAT_APPNAME, RECMAT_ACTIVITYNAME));
            intent.putExtra("functionMode", 0);
            m_configResultLauncher.launch(intent);
        });


        if (! isInstalled){
            Toast.makeText(appCtx, "No se encuentra instalada la app recmat en el dispositivo",
                    Toast.LENGTH_SHORT).show();

        }



    }


    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}




