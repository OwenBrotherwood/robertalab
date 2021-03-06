package de.fhg.iais.roberta.mode.sensor.arduino;

import de.fhg.iais.roberta.inter.mode.sensor.ISoundSensorMode;

public enum SoundSensorMode implements ISoundSensorMode {
    SOUND( "sound", "getSample" );

    private final String halJavaMethodName;
    private final String[] values;

    private SoundSensorMode(String halJavaMethodName, String... values) {
        this.halJavaMethodName = halJavaMethodName;
        this.values = values;
    }

    /**
     * @return name that Lejos is using for this mode
     */
    public String getLejosModeName() {
        return this.values[0];
    }

    //    @Override
    //    public String getHalJavaMethod() {
    //        return this.halJavaMethodName;
    //    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}