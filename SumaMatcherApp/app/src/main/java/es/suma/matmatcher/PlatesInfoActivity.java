package es.suma.matmatcher;

/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
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


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import es.suma.matmatcher.model.AlprPlateInfo;
import es.suma.matmatcher.model.AlprStore;
import es.suma.matmatcher.util.Logger;

public class PlatesInfoActivity extends AppCompatActivity {

    private final String FORMAT_DATE_UTC = "yyyy-MM-dd'T'HH:mm:ssZ";
    private final String FORMAT_DATE_USR = "dd/MM/yyyy";
    private Context appCtx;
    private final Logger LOGGER = new Logger();
    List<AlprPlateInfo> m_results=null;
    TableLayout resultTable ;
    private ProgressBar spinner;

    private final ActivityResultLauncher<String[]> m_getDocument = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            result -> {

                if (result != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setMessage(R.string.message_deleteinfoplates)
                            .setTitle(R.string.title_deleteinfoplates);

                    builder.setPositiveButton(R.string.title_yes, (dialog, id) -> {
                        AlprStore.getInstance(appCtx).deleteAllPlateInfo();

                        parseAndLoadFile(result);


                    });
                    builder.setNegativeButton(R.string.title_no, (dialog, id) -> {

                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plates_info);
        resultTable=findViewById(R.id.resultTable);
        ActionBar actionBar = getSupportActionBar();
        spinner=findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        appCtx=getApplicationContext();

        queryPlates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.platesinfo_menu, menu);
        return true;
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.option_newplateinfo:
                showEditInfo(resultTable, null);
                return true;

            case R.id.option_loadfile:
                loadFile();
                return true;

            case R.id.option_deleteplates:
                deletePlates();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    void queryPlates (){
        Date date = new Date(System.currentTimeMillis());

        spinner.setVisibility(View.VISIBLE);
        PlatesInfoViewModel model = new ViewModelProvider(this).get(PlatesInfoViewModel.class);
        model.getData(appCtx, date).observe(this, platesInfo -> {
            m_results=platesInfo;
            while (resultTable.getChildCount() > 1)
                resultTable.removeView(resultTable.getChildAt(resultTable.getChildCount() - 1));

            for (int i=0; i<platesInfo.size(); i++){
                addResult(platesInfo.get(i));
            }
            spinner.setVisibility(View.INVISIBLE);
            if (platesInfo.size()<=0){
                Toast.makeText(appCtx, getResources().getString(R.string.message_infoplates),
                        Toast.LENGTH_LONG).show();


            }
        });
    }

    @SuppressLint("SetTextI18n")
    void addResult (AlprPlateInfo result){

        resultTable.setVisibility(View.VISIBLE);
        TableLayout.LayoutParams rowLayoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        rowLayoutParams.setMargins(2,2,2,2);
        TableRow.LayoutParams cellLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        TableRow tableRow = new TableRow(appCtx);
        tableRow.setLayoutParams(rowLayoutParams);
        tableRow.setGravity(Gravity.LEFT);

        TextView cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);
        cellValue.setText(result.getmPlate());
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setText(result.getmInfo());
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);

        cellValue = new TextView(appCtx);
        cellValue.setTypeface(null, Typeface.BOLD);
        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        Date w_date=null;
        try {
            if ( result.getmExpires()!=null && !result.getmExpires().equalsIgnoreCase("")){
                w_date= df1.parse(result.getmExpires());
            }
        } catch (ParseException e) {
            e.printStackTrace();
            LOGGER.e(e.getMessage()+" "+result.getmId());
        }

        DateFormat df2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String w_oneDate=df2.format(Objects.requireNonNull(w_date));
        cellValue.setText(w_oneDate);
        cellValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        cellValue.setLayoutParams(cellLayoutParams);
        cellValue.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
        tableRow.addView(cellValue);


        tableRow.setClickable(true);
        tableRow.setTag(result.getmId());

        tableRow.setOnClickListener(view -> {
                    String w_tag=view.getTag().toString();

                    AlprPlateInfo w_plateInfo = AlprStore.getInstance (this).queryPlateinfoById(w_tag);
                    showEditInfo(view, w_plateInfo);
                });


        resultTable.addView(tableRow);

        resultTable.invalidate();
    }

    private void showEditInfo(View view, AlprPlateInfo plateInfo) {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.activity_plateinfodetail, findViewById(R.id.opcion_viewplatesinfo));
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        EditText w_plate = popupView.findViewById(R.id.textPlate);
        EditText w_info = popupView.findViewById(R.id.textInfo);
        EditText w_expires = popupView.findViewById(R.id.textExpires);
        Date w_expiresDate = parseDateFromString(w_expires.getText().toString(),FORMAT_DATE_UTC);
        Button w_delete =  popupView.findViewById(R.id.btnDelete);
        Button w_cancel =  popupView.findViewById(R.id.btnCancel);
        Button w_accept =  popupView.findViewById(R.id.btnAccept);

        if ( plateInfo!=null ) {
            w_plate.setEnabled(false);
            w_plate.setText(plateInfo.getmPlate());
            w_info.setText(plateInfo.getmInfo());
            String w_expiresDateFormatUsr = formatStringFromDate(w_expiresDate, FORMAT_DATE_USR);
            w_expires.setText(w_expiresDateFormatUsr);
        }
        else{
            w_delete.setVisibility(View.INVISIBLE);
        }

        w_accept.setOnClickListener(v -> {
            String w_plateText = w_plate.getText().toString();

            if ( w_plateText==null || w_plateText.length()<5){
                Toast.makeText(appCtx, getString(R.string.message_error_plate),
                        Toast.LENGTH_LONG).show();
                return;

            }
            String w_infoText = w_info.getText().toString();
            if ( w_infoText==null || w_infoText.length()<10){
                Toast.makeText(appCtx, getString(R.string.message_error_plateinfo),
                        Toast.LENGTH_LONG).show();
                return;

            }
            String w_expiresDateFormatUsr = w_expires.getText().toString();
            Date w_expiresDateUsr = parseDateFromString(w_expiresDateFormatUsr,FORMAT_DATE_USR);
            if ( w_expiresDateFormatUsr!=null && w_expiresDateFormatUsr.length()>0){
                if ( w_expiresDateUsr==null){
                    Toast.makeText(appCtx, getString(R.string.message_error_expires),
                            Toast.LENGTH_LONG).show();
                    return;

                }
            }


            if ( plateInfo!=null ){
                plateInfo.setmInfo(w_infoText);
                String w_expiresStringFormatUTC = formatStringFromDate(w_expiresDateUsr, FORMAT_DATE_UTC);
                plateInfo.setmExpires(w_expiresStringFormatUTC);
                AlprStore.getInstance(this).updatePlateInfo(plateInfo);

            }
            else{

                AlprPlateInfo w_plateInfo = new AlprPlateInfo(w_plateText,
                                                              w_infoText, w_expiresDateUsr);
                AlprStore.getInstance(this).insertPlateInfo(w_plateInfo);
            }
            runOnUiThread(this::queryPlates);
            resultTable.invalidate();
            popupWindow.dismiss();
        });

        w_delete.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.message_deleteinfoplates)
                    .setTitle(R.string.title_deleteinfoplates);

            builder.setPositiveButton(R.string.title_yes, (dialog, id) -> {
                AlprStore.getInstance(this).deletePlateInfo(plateInfo);
                runOnUiThread(this::queryPlates);
                resultTable.invalidate();
                popupWindow.dismiss();
            });
            builder.setNegativeButton(R.string.title_no, (dialog, id) -> {

            });

            AlertDialog dialog = builder.create();
            dialog.show();
        });


        w_cancel.setOnClickListener(v -> {
            popupWindow.dismiss();
        });


    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }


    public static class PlatesInfoViewModel extends ViewModel {

        private MutableLiveData<List<AlprPlateInfo>> platesInfo;


        public LiveData<List<AlprPlateInfo>> getData(Context appCtx, Date date) {
            if (platesInfo == null) {
                platesInfo = new MutableLiveData<>();
            }
            loadData(appCtx, date);
            return platesInfo;
        }

        private void loadData(Context appCtx, Date date) {
            AsyncTask.execute(() -> {
                List<AlprPlateInfo> w_platesInfo = AlprStore.getInstance(appCtx).queryPlateInfo(date);
                platesInfo.postValue(w_platesInfo);
            });
        }
    }



    public void loadFile() {
        m_getDocument.launch(new String[]{"*/*"});
    }

    public void deletePlates() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.message_deleteinfoplates)
                .setTitle(R.string.title_deleteinfoplates);

        builder.setPositiveButton(R.string.title_yes, (dialog, id) -> {
            AlprStore.getInstance(appCtx).deleteAllPlateInfo();
            runOnUiThread(this::queryPlates);

        });
        builder.setNegativeButton(R.string.title_no, (dialog, id) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.show();


    }

    protected void parseAndLoadFile(Uri url) {

        AsyncTask.execute(() -> {
            try {

                InputStream csvInput = appCtx.getContentResolver().openInputStream(url);
                CSVParser csvParser = new CSVParserBuilder().withSeparator(';').build();
                CSVReader csvReader =
                        new CSVReaderBuilder(new InputStreamReader(csvInput)).withCSVParser(csvParser).build();

                boolean w_showError=true;
                String[] nextLine;
                int w_linea =0;
                while ((nextLine = csvReader.readNext()) != null) {
                    w_linea++;
                    String w_Plate = null;
                    String w_Info = "";
                    String w_Expires ;
                    if (nextLine.length >=1){
                        w_Plate=nextLine[0];
                        w_Plate=w_Plate.trim();
                        if ( w_Plate.equalsIgnoreCase("")) continue;
                    }
                    if (nextLine.length >=2){
                        w_Info=nextLine[1];
                        w_Info=w_Info.trim();
                    }

                    Date w_date ;
                    Calendar wCal = Calendar.getInstance();
                    wCal.set(Calendar.YEAR, 2222);
                    wCal.set(Calendar.MONTH, 11);
                    wCal.set(Calendar.DAY_OF_MONTH, 11);
                    w_date = wCal.getTime();
                    if (nextLine.length >=3){
                        w_Expires=nextLine[2];
                        w_Expires=w_Expires.trim();
                        if (!w_Expires.equalsIgnoreCase("")){
                            DateFormat df1 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            try {
                                w_date= df1.parse(nextLine[2]);
                            } catch (ParseException e) {
                                e.printStackTrace();
                                String w_error = getString(R.string.message_error_date)+ w_linea;
                                LOGGER.e(w_error+" "+e.getMessage());
                                if ( w_showError ){
                                    Toast.makeText(appCtx, w_error,
                                            Toast.LENGTH_LONG).show();
                                    w_showError=false;
                                }

                            }
                        }
                    }

                    AlprPlateInfo w_plateInfo = new AlprPlateInfo(w_Plate, w_Info, w_date);
                    AlprStore.getInstance(appCtx).insertPlateInfo(w_plateInfo);

                }
            } catch (IOException | CsvValidationException e) {
                e.printStackTrace();
                LOGGER.e("Error."+e.getMessage());

            }

            runOnUiThread(this::queryPlates);

        });
    }

    private Date parseDateFromString(String dateText, String format) {
        Date newDate= new Date();

        if ( dateText!=null ) {
            DateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
            try {
                newDate = dateFormat.parse(dateText);
            } catch (ParseException e) {
                e.printStackTrace();

            }
        }
        return newDate;
    }


    private String formatStringFromDate(Date oneDate, String format) {
        String newDateString= new String();

        if ( oneDate!=null ) {
            DateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
            newDateString = dateFormat.format(oneDate);
        }
        return newDateString;
    }

}