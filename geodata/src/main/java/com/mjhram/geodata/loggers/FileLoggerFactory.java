/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mjhram.geodata.loggers;

import android.content.Context;

import com.mjhram.geodata.common.AppSettings;
import com.mjhram.geodata.common.MyInfo;
import com.mjhram.geodata.common.Session;
import com.mjhram.geodata.common.Utilities;
import com.mjhram.geodata.loggers.customurl.CustomUrlLogger;

import java.util.ArrayList;
import java.util.List;

public class FileLoggerFactory {
    private static List<IFileLogger> GetFileLoggers(Context context) {

        List<IFileLogger> loggers = new ArrayList<IFileLogger>();

        if (AppSettings.shouldLogToCustomUrl()) {
            float batteryLevel = Utilities.GetBatteryLevel(context);
            String androidId = Utilities.GetAndroidId(context);
            loggers.add(new CustomUrlLogger(AppSettings.getCustomLoggingUrl(), Session.getSatelliteCount(), batteryLevel, androidId));
        }
        return loggers;
    }

    public static void Write(Context context, MyInfo loc) throws Exception {
        for (IFileLogger logger : GetFileLoggers(context)) {
            logger.Write(loc);
        }
    }
}
