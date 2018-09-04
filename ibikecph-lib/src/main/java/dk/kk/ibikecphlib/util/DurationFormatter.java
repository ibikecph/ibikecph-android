package dk.kk.ibikecphlib.util;

import dk.kk.ibikecphlib.IBikeApplication;

public class DurationFormatter {
    public static String formatDuration(double seconds) {
        return String.format(IBikeApplication.getString("hour_minute_format"), (int) (seconds / 60 / 60), (int) ((seconds / 60) % 60));
        //durationText.setText(DurationFormatUtils.formatDuration(durationLeft, "**H:mm:ss**", true));
    }
}
