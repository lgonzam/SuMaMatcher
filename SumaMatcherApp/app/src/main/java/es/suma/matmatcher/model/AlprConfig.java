/*
 *
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


package es.suma.matmatcher.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Size;


public class AlprConfig {
    private static final String DEFAULT_PREVIEW_SIZE = "1024x720";

    private final int m_functionMode;
    private final float m_alprMinConfidence;
    private final String m_alprCountry;
    private final String m_alprRegion;
    private final int m_alprTopResults;
    private final Size m_previewSize;
    private final Size m_cropSize;
    private final int m_timeSkipMs;
    private final int m_timeWaitMs;
    private final String m_defaultPreviewSize;
    private final boolean m_textToSpeech;


    public static AlprConfig create(Context appCtx) {
        AlprConfig alprConfig = new AlprConfig(appCtx);
        return alprConfig;
    }

    private AlprConfig(Context appCtx) {
        m_defaultPreviewSize = DEFAULT_PREVIEW_SIZE;

        SharedPreferences w_prefs = PreferenceManager.getDefaultSharedPreferences(appCtx);

        // [640x480, 352x288, 320x240, 176x144, 1280x720, 1280x960]
        String w_tmpPreviewSize = w_prefs.getString("preview-size", m_defaultPreviewSize);
        String[] w_previewSize = w_tmpPreviewSize.split("x");
        int w_previewSizeW = Integer.parseInt(w_previewSize[0]);
        int w_previewSizeH = Integer.parseInt(w_previewSize[1]);
        m_previewSize = new Size(w_previewSizeW, w_previewSizeH);
        m_cropSize = m_previewSize;
        String w_tmpMode = w_prefs.getString("function-mode", "0");
        m_functionMode = Integer.parseInt(w_tmpMode);
        String w_tmpConf = w_prefs.getString("alpr-min-confidence", "80");
        m_alprMinConfidence = Float.parseFloat(w_tmpConf) / 100.0f;
        m_alprCountry = w_prefs.getString("alpr-country", "eu");
        m_alprRegion = w_prefs.getString("alpr-region", "es");
        String w_tmpTopR = w_prefs.getString("alpr-top-results", "4");
        m_alprTopResults = Integer.parseInt(w_tmpTopR);
        String w_timeSkip = w_prefs.getString("time-skip-same-license", "30000");
        m_timeSkipMs = Integer.parseInt(w_timeSkip);
        String w_timeWait= w_prefs.getString("time-wait-next-license", "4000");
        m_timeWaitMs = Integer.parseInt(w_timeWait);
        String w_textoToSpeech =  w_prefs.getString("text_to_speech", "true");
        m_textToSpeech = Boolean.parseBoolean(w_textoToSpeech);

    }

    public int getM_functionMode() {
        return m_functionMode;
    }

    public float getM_alprMinConfidence() {
        return m_alprMinConfidence;
    }

    public String getM_alprCountry() {
        return m_alprCountry;
    }

    public String getM_alprRegion() {
        return m_alprRegion;
    }

    public int getM_alprTopResults() {
        return m_alprTopResults;
    }

    public Size getM_previewSize() {
        return m_previewSize;
    }

    public Size getM_cropSize() {
        return m_cropSize;
    }

    public int getM_timeSkipMs() {
        return m_timeSkipMs;
    }

    public int getM_timeWaitMs() {
        return m_timeWaitMs;
    }

    public String getM_defaultPreviewSize() {
        return m_defaultPreviewSize;
    }

    public boolean getM_textToSpeech() {
        return m_textToSpeech;
    }
}
