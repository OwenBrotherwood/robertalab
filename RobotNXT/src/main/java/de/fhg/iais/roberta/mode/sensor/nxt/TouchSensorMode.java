package de.fhg.iais.roberta.mode.sensor.nxt;

import de.fhg.iais.roberta.inter.mode.sensor.ITouchSensorMode;

public enum TouchSensorMode implements ITouchSensorMode {
    TOUCH( "isPressed", "touch" );
    private final String halJavaMethodName;
    private final String[] values;

    private TouchSensorMode(String halJavaMethodName, String... values) {
        this.halJavaMethodName = halJavaMethodName;
        this.values = values;
    }

    public String getLejosModeName() {
        return this.values[0];
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}