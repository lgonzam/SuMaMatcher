/*
 *
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


package es.suma.matmatcher.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Size;


public class AlprConfig {
    private static final String DEFAULT_PREVIEW_SIZE = "720x720";

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
    private final boolean m_debugMode;
    private final String m_colorMode;
    private float m_zoom;

    public static AlprConfig create(Context appCtx) {
        return new AlprConfig(appCtx);
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
        if (m_previewSize.getHeight()==m_previewSize.getWidth() && m_previewSize.getWidth()<=1088) {
            m_cropSize = m_previewSize;
        }
        else{
            m_cropSize = new Size(720, 720);
        }
        String w_tmpMode = w_prefs.getString("function-mode", "0");
        m_functionMode = Integer.parseInt(w_tmpMode);
        String w_tmpConf = w_prefs.getString("alpr-min-confidence", "80");
        m_alprMinConfidence = Float.parseFloat(w_tmpConf) / 100.0f;
        String w_country = w_prefs.getString("alpr-country", "eu");
        if ( w_country.equalsIgnoreCase("es")){
            m_alprCountry= "eu";
            m_alprRegion = w_prefs.getString("alpr-region", "es");
        }else if ( w_country.equalsIgnoreCase("eu")) {
            m_alprCountry= "eu";
            m_alprRegion = "";
        }
        else{
            m_alprCountry= "eu";
            m_alprRegion = "";
        }
        String w_tmpTopR = w_prefs.getString("alpr-top-results", "4");
        m_alprTopResults = Integer.parseInt(w_tmpTopR);
        String w_timeSkip = w_prefs.getString("time-skip-same-license", "30000");
        m_timeSkipMs = Integer.parseInt(w_timeSkip);
        String w_timeWait= w_prefs.getString("time-wait-next-license", "4000");
        m_timeWaitMs = Integer.parseInt(w_timeWait);
        String w_textToSpeech =  w_prefs.getString("text_to_speech", "true");
        m_textToSpeech = Boolean.parseBoolean(w_textToSpeech);
        String w_debugMode =  w_prefs.getString("debug-mode", "false");
        m_debugMode= Boolean.parseBoolean(w_debugMode);
        m_colorMode =  w_prefs.getString("color-mode", "rgb");
        String w_zoom =  w_prefs.getString("zoom", "1");
        m_zoom = Integer.parseInt(w_zoom);

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

    public boolean getM_debugMode() {
        return m_debugMode;
    }

    public String getM_colorMode (){
        return m_colorMode;
    }

    public float getZoom() {
        return m_zoom;
    }
}
