package app.audioboss;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;

import androidx.annotation.NonNull;

public class DataObject {
    public static CellProcessor[] rawObjectProcessor = new CellProcessor[]{
            new ParseDouble(),//maxValue
            new ParseDouble(),//minValue
    };
    double maxValue;
    double minValue;

    public DataObject(double maxValue, double minValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    public static String getHeader() {
        return "maxValue, minValue\n";
    }

    public static String[] getHeaders() {
        return new String[]{"maxValue", "minValue"};
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s,%s\n", maxValue, minValue);
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }
}
