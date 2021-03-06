package de.fhg.iais.roberta.mode.sensor.nxt;

import de.fhg.iais.roberta.inter.mode.sensor.IGyroSensorMode;

public enum GyroSensorMode implements IGyroSensorMode {
    RATE( "getGyroSensorRate", "Rate" ), ANGLE( "getGyroSensorAngle", "Angle" ), RESET( "resetGyroSensor" );

    private final String[] values;
    private final String halJavaMethodName;

    private GyroSensorMode(String halJavaMethodName, String... values) {
        this.halJavaMethodName = halJavaMethodName;
        this.values = values;
    }

    /**
     * @return name that Lejos is using for this mode
     */
    public String getLejosModeName() {
        return this.values[0];
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}