package no.ntnu.idata2304.smarthome.controlpanel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests anomaly detection thresholds for sensor readings. */
class AnomalyDetectorTest {

  @Test
  void detectsColdTemperatureBelowFiveDegrees() {
    assertTrue(AnomalyDetector.isTemperatureColdAnomaly(4.9));
    assertFalse(AnomalyDetector.isTemperatureColdAnomaly(5.0));
  }

  @Test
  void detectsHotTemperatureAboveThirtyFiveDegrees() {
    assertFalse(AnomalyDetector.isTemperatureHotAnomaly(35.0));
    assertTrue(AnomalyDetector.isTemperatureHotAnomaly(35.1));
  }

  @Test
  void detectsHighHumidityAboveEightyPercent() {
    assertFalse(AnomalyDetector.isHumidityHighAnomaly(80.0));
    assertTrue(AnomalyDetector.isHumidityHighAnomaly(80.1));
  }
}