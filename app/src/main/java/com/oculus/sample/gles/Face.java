package com.oculus.sample.gles;

import android.util.Log;

import static com.oculus.sample.gles.ShaderProgram.TAG;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;

/**
 * Created by josgood on 1/6/18.
 */

public class Face {

    // CLASS VARIABLES - SET THESE FIRST!!!
    private static int FACES_PER_WIDTH;
    private static int FACES_PER_HEIGHT;
    private static int N_SLICES;

    public static void setFacesPerWidth(int facesPerWidth) {
        FACES_PER_WIDTH = facesPerWidth;
    }

    public static void setFacesPerHeight(int facesPerHeight) {
        FACES_PER_HEIGHT = facesPerHeight;
    }

    public static void setnSlices(int nSlices) {
        N_SLICES = nSlices;
    }

    public static float getnSlices(){
        return N_SLICES;
    }

    // INSTANCE VARIABLES
    private float[] centerVector = new float[3];
    private float centerLength;
    private float[] angleVec = new float[3];
    private float angleVecLength;
    private float[] crossVec = new float[3]; //cross product of centerVec and angleVec
    // origin is of the form (s,t),
    //   where s ranges from 0 to 1 from the left side to the right
    //   and   t ranges from 0 to -1 from top to bottom
    //   (the above ranges describe what one needs to pass as a parameter in the below constructor,
    //     the actual t is adjusted by 1/nSlices)
    private float[] origin = new float[2];

    // CONSTRUCTOR
    // MAKE SURE centerVector AND angleVector ARE PERPENDICULAR!!!!!!
    public Face(float[] centerVector, float[] angleVector, float[] origin) {
        this.centerVector[0] = centerVector[0];
        this.centerVector[1] = centerVector[1];
        this.centerVector[2] = centerVector[2];
        centerLength = (float) sqrt(centerVector[0]*centerVector[0] + centerVector[1]*centerVector[1] + centerVector[2]*centerVector[2]);
        angleVec[0] = angleVector[0];
        angleVec[1] = angleVector[1];
        angleVec[2] = angleVector[2];
        angleVecLength = (float) sqrt(angleVector[0]*angleVector[0] + angleVector[1]*angleVector[1] + angleVector[2]*angleVector[2]);
        this.origin[0] = origin[0];
        this.origin[1] = (1.0f/(float)N_SLICES) + origin[1];
        crossVec = normalize(crossProduct(this.centerVector, angleVec));
    }

    public float[] getCenterVector() { return centerVector; }

    // OTHER METHODS
    // Override this method in subclasses
    public boolean isInBounds(float ds, float dt, float i){
        return true;
    }

    public float[] getdSdT(float x, float y, float z, float ds0, float dt0) {
        float[] temp = new float[] {x, y, z};
        float i = angleFromCenter(temp);
        float j = angleAroundCenter(temp);
        float ds = (float) (Math.tan(i) * Math.cos(j));
        float dt = (float) -(Math.tan(i) * Math.sin(j));

        /*
        if(centerVector[2]==500.f && z>250.f) {
            if (Math.abs((ds) - ds0) < 0.01) {
                Log.d(TAG, "ds looks GOOD, it's " + (ds));
            } else {
                Log.d(TAG, "ds looks BAD, it's " + (ds) + " should be " + ds0);
            }
            if (Math.abs((dt) - dt0) < 0.01) {
                Log.d(TAG, "dt looks GOOD, it's " + (dt));
            } else {
                Log.d(TAG, "dt looks BAD, it's " + (dt) + " should be " + dt0);
            }
        }
        */

        if(isInBounds(ds, dt, i)) {
            return new float[] {ds, dt};
        }
        else {
            return null;
        }
    }

    public float[] getSAndT(float x, float y, float z, float i0, float j0) {
        float[] result = getdSdT(x, y, z, i0, j0);
        if(result==null) return null;
        return new float[] {origin[0] + result[0] / (float) (FACES_PER_WIDTH * 2), origin[1] + result[1] / (float) (FACES_PER_HEIGHT * 2)};
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    public static float[] crossProduct(float[] v1, float[] v2) {
        return new float[] {v1[1]*v2[2] - v1[2]*v2[1], v1[2]*v2[0] - v1[0]*v2[2], v1[0]*v2[1] - v1[1]*v2[0]};
    }

    // generates "i", essentially "pitch" if centerVector was the vertical axis
    public float angleFromCenter(float[] v) {
        float dot = v[0]*centerVector[0] + v[1]*centerVector[1] + v[2]*centerVector[2];
        float vLen = (float) sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return (float) acos(dot / (vLen*centerLength));
    }

    public float angleFromAngleVec(float[] a) {
        float dot = a[0]*angleVec[0] + a[1]*angleVec[1] + a[2]*angleVec[2];
        float aLen = (float) sqrt(a[0]*a[0] + a[1]*a[1] + a[2]*a[2]);
        return (float) acos(dot / (aLen*angleVecLength));
    }

    // subtract (the projection of v1 onto v2) from v1
    public static float[] projectOntoPerp(float[] v1, float[] v2) {
        float dot = (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]);
        float lens = (float) Math.sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v1[2]) * (float) Math.sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2]);
        float coeff = dot / lens;
        float[] projV = new float[3];
        projV[0] = v1[0] - (coeff*v2[0]);
        projV[1] = v1[1] - (coeff*v2[1]);
        projV[2] = v1[2] - (coeff*v2[2]);
        return projV;
    }

    public static float[] normalize(float[] v){
        float length = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        float[] ret = new float[3];
        ret[0] = v[0]/length;
        ret[1] = v[1]/length;
        ret[2] = v[2]/length;
        return ret;
    }

    // generates "j", essentially "yaw" if centerVector was vertical and angleVec the positive X-axis
    public float angleAroundCenter(float[] v) {
        // project v onto the plane defined by centerVector
        float[] proj1 = projectOntoPerp(v, centerVector);
        /*
        if(centerVector[2]==500.f && v[2]>250.f) {
            Log.d(TAG, "with CENTER VEC: " + centerVector[0] + ", " + centerVector[1] + ", " + centerVector[2]);
            Log.d(TAG, "with ANGLE VEC : " + angleVec[0] + ", " + angleVec[1] + ", " + angleVec[2]);
            Log.d(TAG, "projectOntoPerp: " + proj1[0] + ", " + proj1[1] + ", " + proj1[2]);
        }
        */
        float[] proj2 = projectOntoPerp(proj1, angleVec);
        float result = angleFromAngleVec(proj1);
        float[] norm = normalize(proj2);
        // crossVec normalized in constructor
        if((Math.abs(norm[0]-crossVec[0])<1 && //|| proj2[0]-crossVec[0]>-0.01) &&
                Math.abs(norm[1]-crossVec[1])<1 && //|| proj2[1]-crossVec[1]>-0.01) &&
                Math.abs(norm[2]-crossVec[2])<1)) { // || proj2[2]-crossVec[2]>-0.01))){
            //Log.d(TAG, "time to flip????");
            result *= -1.f;
        }

        return result;
    }

}
