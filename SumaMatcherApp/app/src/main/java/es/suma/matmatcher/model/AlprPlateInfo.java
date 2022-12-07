/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
 *
 * This file is part of RecMatApp.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class AlprPlateInfo {
    public AlprPlateInfo(String mPlate, String mInfo, Date mExpires) {
        this.mId = UUID.randomUUID().toString();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        this.mDatetime = df.format(new Date());
        this.mPlate = mPlate;
        this.mInfo = mInfo;
        this.mExpires = df.format(mExpires);
    }

    public AlprPlateInfo(String mId, String mDatetime, String mPlate, String mInfo, String mExpires) {
        this.mId = mId;
        this.mDatetime = mDatetime;
        this.mPlate = mPlate;
        this.mInfo = mInfo;
        this.mExpires = mExpires;
    }
    private final String mId;
    private final String mDatetime;
    private final String mPlate;
    private String mInfo;
    private String mExpires;

    public String getmId() {
        return mId;
    }

    public String getmDatetime() {
        return mDatetime;
    }

    public String getmPlate() {
        return mPlate;
    }

    public String getmInfo() {
        return mInfo;
    }

    public String getmExpires() {
        return mExpires;
    }

    public void setmInfo(String info) {
        mInfo=info;
    }

    public void setmExpires(String expires) {
        mExpires = expires;
    }
}
