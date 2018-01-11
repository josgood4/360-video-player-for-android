package com.oculus.sample.gles;

import static java.lang.Math.sqrt;

/**
 * Created by josgood on 1/6/18.
 */

public class CubeFace extends Face {

    public CubeFace(float[] centerVector, float[] angleVector, float[] origin) {
        super(centerVector, angleVector, origin);
    }

    @Override
    public boolean isInBounds(float ds, float dt, float i) {
        return Math.abs(ds) <= 1.0f && Math.abs(dt) <= 1.0f && i < Math.PI/2.0f;
    }

}
