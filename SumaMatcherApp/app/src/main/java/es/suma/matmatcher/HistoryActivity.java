package es.suma.matmatcher;

/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
 *
 *
 * SumaMatcher App is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * version 3 as published by the Free Software Foundation
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import es.suma.matmatcher.R;

import es.suma.matmatcher.util.FileUtils;
import es.suma.matmatcher.util.Logger;
import es.suma.matmatcher.model.AlprDetection;
import es.suma.matmatcher.model.AlprStore;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private Context appCtx;
    private final Logger LOGGER = new Logger();
    List<AlprDetection> m_results=null;
    TableLayout resultTable ;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        resultTable=findViewById(R.id.resultTable);
        ActionBar actionBar = getSupportActionBar();
        spinner=(ProgressBar)findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        appCtx=getApplicationContext();
        queryHistory();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        return true;
    }

    private void queryHistory(){

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -5);
        Date date = cal.getTime();
        DetectionsViewModel model = new ViewModelProvider(this).get(DetectionsViewModel.class);
        model.getData(appCtx, date).observe(this, detections -> {
            m_results=detections;
            while (resultTable.getChildCount() > 1)
                resultTable.removeView(resultTable.getChildAt(resultTable.getChildCount() - 1));
            for (int i=0; i<detections.size(); i++){
                addResult(detections.get(i));
            }
            spinner.setVisibility(View.INVISIBLE);
            if (detections.size()<=0){
                Toast.makeText(appCtx, getResources().getString(R.string.message_history),
                        Toast.LENGTH_LONG).show();


            }

        });
    }


    @SuppressLint("SetTextI18n")
    void addResult (AlprDetection result){

        resultTable.setVisibility(View.VISIBLE);
        TableLayout.LayoutParams rowLayoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        rowLayoutParams.setMargins(2,2,2,2);
        TableRow.LayoutParams cellLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        TableRow tableRow = new TableRow(appCtx);
        tableRow.setLayoutParams(rowLayoutParams);
        tableRow.setGravity(Gravity.LEFT);

        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date w_date;
        try {
            w_date= df1.parse(result.getmDatetime());
        } catch (ParseException e) {
            e.printStackTrace();
            LOGGER.e("Error. fecha incorrecta. En resultado="+result.getmId());
            w_date=null;
        }

        TextView cellValue = new TextView(appCtx);
        if ( w_date!=null){
            Calendar cal = Calendar.getInstance();
            cal.setTime(w_date);
            int w_dia = cal.get(Calendar.DAY_OF_MONTH);
            int w_mes= cal.get(Calendar.MONTH)+1;
            String w_time = String.format("%02d/%02d", w_dia, w_mes);
            cellValue.setText(w_time);
        }
        else{
            cellValue.setText("-/-");
        }
        cellValue.setTypeface(null, Typeface.BOLD);
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);

        if ( w_date!=null){
            Calendar cal = Calendar.getInstance();
            cal.setTime(w_date);
            int w_hora = cal.get(Calendar.HOUR_OF_DAY);
            int w_minutos= cal.get(Calendar.MINUTE);
            String w_time = String.format("%02d:%02d", w_hora, w_minutos);
            cellValue.setText(w_time);
        }
        else{
            cellValue.setText("00:00");
        }

        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        ImageView im = new ImageView(this);
        Bitmap w_detectionBitmap = result.getCropImage();
        im.setImageBitmap(w_detectionBitmap);
        im.setScaleType(ImageView.ScaleType.FIT_XY);
        im.setPadding(2, 2, 2, 2);
        im.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        im.getLayoutParams().height = 50;
        im.getLayoutParams().width = 50;
        im.requestLayout();
        im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tableRow.addView(im);

        cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);
        cellValue.setText(result.getmPlate());
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);
        String w_confidence=String.format("%.0f", result.getmConfidence());
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        w_confidence+="%";
        cellValue.setText(w_confidence);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        tableRow.setClickable(true);
        tableRow.setTag(result.getmId());
        tableRow.setOnClickListener(v -> {

            String w_tag=v.getTag().toString();

            AlprDetection w_detection = findDetection (w_tag, m_results);
            if (w_detection!=null) {
                showDetail(w_detection);
            }
            else
            {
                Toast.makeText(appCtx, "Opción no disponible po que no se ha encontrado la entrada de información. ID "+w_tag,
                    Toast.LENGTH_SHORT).show();

            }
        });
        resultTable.addView(tableRow);

        resultTable.invalidate();
    }

    private void showDetail(AlprDetection result) {
        String w_pathname= appCtx.getApplicationInfo().dataDir + File.separator + "images";
        String w_filename= result.getmFilename();
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("id", result.getmId());
        intent.putExtra("pathname", w_pathname);
        intent.putExtra("filename", w_filename);
        startActivity(intent);
    }


    public AlprDetection findDetection(
            String id, List<AlprDetection> detections) {
        if ( detections==null ){
            return null;
        }
        for (AlprDetection detection : detections) {
            if (detection.getmId().equals(id)) {
                return detection;
            }
        }
        return null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        switch (item.getItemId()) {
            case R.id.opcion_deletehistory:
                deleteHistory();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }


    private void deleteHistory() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.message_deletehistory)
                .setTitle(R.string.title_deletehistory);

        builder.setPositiveButton(R.string.title_yes, (dialog, id) -> {
            String androidDataDir = getApplicationInfo().dataDir;
            final File pathImg = new File(androidDataDir + File.separator + "images");
            if (!FileUtils.deleteDirectory(pathImg)) {
                LOGGER.e("Error. On delete dir=" + pathImg);
            }
            AlprStore.getInstance(appCtx).deleteAllDetections();
            runOnUiThread(() -> {
                queryHistory();
            });

        });
        builder.setNegativeButton(R.string.title_no, (dialog, id) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }



    public static class DetectionsViewModel extends ViewModel {

        private MutableLiveData<List<AlprDetection>> detections;


        public LiveData<List<AlprDetection>> getData(Context appCtx, Date date) {
            if (detections == null) {
                detections = new MutableLiveData<>();
            }
            loadData(appCtx, date);
            return detections;
        }

        private void loadData(Context appCtx, Date date) {

            AsyncTask.execute(() -> {
                List<AlprDetection> w_detections = AlprStore.getInstance(appCtx).queryDetection(date);
                detections.postValue(w_detections);
            });
        }
    }


}