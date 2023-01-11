/*
 * 2022 Suma Gestión Tributaria. Unidad Proyectos Especiales.
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

package es.suma.matmatcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import es.suma.matmatcher.ui.BoxTracker;
import es.suma.matmatcher.model.AlprDetection;
import es.suma.matmatcher.model.AlprStore;

import java.io.File;
import java.util.Locale;

public class HistoryDetailActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historydetail);

        ImageView imageView = findViewById(R.id.imageView);
        TextView textDate = findViewById(R.id.textDate);
        TextView textPlate = findViewById(R.id.textPlate);
        TextView textConfidence = findViewById(R.id.textConfidence);
        TextView textInfo = findViewById(R.id.textInfo);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Context appCtx = this;

        Intent intent = getIntent();

        String w_id = intent.getStringExtra("id");
        AlprDetection detection = AlprStore.getInstance(appCtx).queryDetection(w_id);
        String w_pathname = intent.getStringExtra("pathname");
        String w_filename = intent.getStringExtra("filename");
        Bitmap w_image = BitmapFactory.decodeFile(w_pathname + File.separator + w_filename);
        Bitmap w_mutableBitmap = w_image.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(w_mutableBitmap);
        BoxTracker boxTracker = new BoxTracker();
        boxTracker.trackResult(detection, w_image, detection.getmLocation(), null, 0);
        boxTracker.draw(w_mutableBitmap);

        textDate.setText( detection.getmFormatDatetime());
        textPlate.setText( detection.getmPlate());
        String w_confidence=String.format(Locale.getDefault(), "%.0f", detection.getmConfidence()) + " %";
        textConfidence.setText(w_confidence);
        String w_info = detection.getInfo();
        if (w_info==null || w_info.equalsIgnoreCase(""))
            w_info=getResources().getString(R.string.message_plate_noinfo);
        textInfo.setText( w_info);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

}
