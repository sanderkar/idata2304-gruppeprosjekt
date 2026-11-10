package no.ntnu.idata2304.smarthome.controlpanel;

/**
 * This class will be used to detect anomalies
 *
 */
public class AnomalyDetector {

    public static boolean isTemperatureColdAnomaly(double temperature) {
        return temperature < 5.0;
    }

    public static boolean isTemperatureHotAnomaly(double temperature) {
        return temperature > 35.0;
    }

    public static boolean isHumidityHighAnomaly(double humidity) {
        return humidity > 80.0;
    }
}
