package com.oculus.sample.gles;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/license-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.util.Log;

import java.nio.ShortBuffer;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.oculus.sample.gles.ShaderProgram.TAG;

/*
 * Class for generating a sphere model for given input params
 * The generated class will have vertices and indices
 * Vertices data is composed of vertex coordinates in x, y, z followed by
 *  texture coordinates s, t for each vertex
 * Indices store vertex indices for the whole sphere.
 * Formula for generating sphere is originally coming from source code of
 * OpenGL ES2.0 Programming guide
 * which is available from http://code.google.com/p/opengles-book-samples/,
 * but some changes were made to make texture look right.
 */
public class Sphere {
    public static final int FLOAT_SIZE = 4;
    public static final int SHORT_SIZE = 2;
    private FloatBuffer mVertices;
    private ShortBuffer[] mIndices;
    private int[] mNumIndices;
    private int mTotalIndices;
    /*
     * @param nSlices how many slice in horizontal direction.
     *                The same slice for vertical direction is applied.
     *                nSlices should be > 1 and should be <= 180
     * @param x,y,z the origin of the sphere
     * @param r the radius of the sphere
     */
    public Sphere(int nSlices, float x, float y, float z, float r, int numIndexBuffers) {
        int iMax = nSlices + 1;
        int nVertices = iMax * iMax;
        if (nVertices > Short.MAX_VALUE) {
            // this cannot be handled in one vertices / indices pair
            throw new RuntimeException("nSlices " + nSlices + " too big for vertex");
        }
        mTotalIndices = nSlices * nSlices * 6;
        float angleStepI = ((float) Math.PI / nSlices);
        float angleStepJ = ((2.0f * (float) Math.PI) / nSlices);
        // 3 vertex coords + 2 texture coords
        mVertices = ByteBuffer.allocateDirect(nVertices * 5 * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mIndices = new ShortBuffer[numIndexBuffers];
        mNumIndices = new int[numIndexBuffers];
        // first evenly distribute to n-1 buffers, then put remaining ones to the last one.
        int noIndicesPerBuffer = (mTotalIndices / numIndexBuffers / 6) * 6;
        for (int i = 0; i < numIndexBuffers - 1; i++) {
            mNumIndices[i] = noIndicesPerBuffer;
        }
        mNumIndices[numIndexBuffers - 1] = mTotalIndices - noIndicesPerBuffer *
                (numIndexBuffers - 1);
        for (int i = 0; i < numIndexBuffers; i++) {
            mIndices[i] = ByteBuffer.allocateDirect(mNumIndices[i] * SHORT_SIZE)
                    .order(ByteOrder.nativeOrder()).asShortBuffer();
        }

        /*
        //ORIGINAL, EQUIRECTANGULAR:
        // calling put for each float took too much CPU time, so put by line instead
        float[] vLineBuffer = new float[iMax * 5];
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < iMax; j++) {
                int vertexBase = j * 5;
                float sini = (float) Math.sin(angleStepI * i);
                float sinj = (float) Math.sin(angleStepJ * j);
                float cosi = (float) Math.cos(angleStepI * i);
                float cosj = (float) Math.cos(angleStepJ * j);
                // vertex x,y,z
                vLineBuffer[vertexBase + 0] = x + r * sini * sinj;
                vLineBuffer[vertexBase + 1] = y + r * sini * cosj;
                vLineBuffer[vertexBase + 2] = z + r * cosi;
                // texture s,t
                vLineBuffer[vertexBase + 3] = (float) j / (float) nSlices; //1.0f - ( ((float) j) / (float) nSlices);
                vLineBuffer[vertexBase + 4] = (1.0f - i) / (float)nSlices; //(-nSlices+i+1) / (float)nSlices;
            }
            mVertices.put(vLineBuffer, 0, vLineBuffer.length);
        }
        */

        /*
        //CubeFace f1 = new CubeFace(new float[] {0,0,r}, new float[] {0,r,0}, new float[] {5.0f/6.0f, -1.0f/4.0f});
        //CubeFace f2 = new CubeFace(new float[] {0,0,-r}, new float[] {0,-r,0}, new float[] {1.0f/6.0f, -3.0f/4.0f});
        //CubeFace f4 = new CubeFace(new float[] {r,0,0}, new float[] {0,r,0}, new float[] {5.0f/6.0f, -3.0f/4.0f});
        // calling put for each float took too much CPU time, so put by line instead
        float[] vLineBuffer = new float[iMax * 5];
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < iMax; j++) {
                int vertexBase = j * 5;
                float sini = (float) Math.sin(angleStepI * i);
                float sinj = (float) Math.sin(angleStepJ * j);
                float cosi = (float) Math.cos(angleStepI * i);
                float cosj = (float) Math.cos(angleStepJ * j);
                //float tani = (float) sini/cosi;
                float ds = sini / cosi * cosj;
                float dt = sini / cosi * sinj;
                // vertex x,y,z
                float dx = x + r * sini * sinj;
                vLineBuffer[vertexBase + 0] = dx;
                float dy = y + r * sini * cosj;
                vLineBuffer[vertexBase + 1] = dy;
                float dz = z + r * cosi;
                vLineBuffer[vertexBase + 2] = dz;
                //float[] temp = f2.getSAndT(dx, dy, dz, i, j);

                if (Math.abs(ds) <= 1.0f && Math.abs(dt) <= 1.0f && i < iMax / 2) {
                    // top face
                    vLineBuffer[vertexBase + 3] = (float) ((5.0f / 6.0f) + ds / 6.0f);
                    vLineBuffer[vertexBase + 4] = (float) (1.0f / ((float) nSlices) - 0.25f + dt / 4.0f);
                //} else if(temp!=null) {
                //    vLineBuffer[vertexBase + 3] = temp[0];
                //    vLineBuffer[vertexBase + 4] = temp[1];
                //    Log.d(TAG, " temp: (" + temp[0] + ", " + temp[1]);
                //    // WHY IS THIS INFINITY!?!?!?!?
                } else {
                    // texture s,t
                    vLineBuffer[vertexBase + 3] = (float) j / (float) nSlices;
                    vLineBuffer[vertexBase + 4] = (1.0f - i) / (float) nSlices;
                }
                Log.d(TAG, "x: " + vLineBuffer[vertexBase + 0] +
                        " y: " + vLineBuffer[vertexBase + 1] +
                        " z: " + vLineBuffer[vertexBase + 2] +
                        " i: " + i +
                        " j: " + j);
                //" s: " + vLineBuffer[vertexBase + 3] +
                //" t: " + vLineBuffer[vertexBase + 4]);
            }
            mVertices.put(vLineBuffer, 0, vLineBuffer.length);
        }
        */

        Face.setFacesPerHeight(2);
        Face.setFacesPerWidth(3);
        Face.setnSlices(nSlices);
        CubeFace f1 = new CubeFace(new float[] {0,0,r}, new float[] {0,-r,0}, new float[] {5.0f/6.0f, -1.0f/4.0f});
        CubeFace f2 = new CubeFace(new float[] {0,0,-r}, new float[] {0,-r,0}, new float[] {1.0f/6.0f, -3.0f/4.0f});
        CubeFace f3 = new CubeFace(new float[] {-r,0,0}, new float[] {0,-r,0}, new float[] {3.0f/6.0f, -3.0f/4.0f});
        CubeFace f4 = new CubeFace(new float[] {r,0,0}, new float[] {0,r,0}, new float[] {5.0f/6.0f, -3.0f/4.0f});
        CubeFace f5 = new CubeFace(new float[] {0,-r,0}, new float[] {r,0,0}, new float[] {1.0f/6.0f, -1.0f/4.0f});
        CubeFace f6 = new CubeFace(new float[] {0,r,0}, new float[] {-r,0,0}, new float[] {3.0f/6.0f, -1.0f/4.0f});

        CubeFace[] faces = new CubeFace[] {f1, f2, f3, f4, f5, f6};

        // calling put for each float took too much CPU time, so put by line instead
        float[] vLineBuffer = new float[iMax * 5];
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < iMax; j++) {
                int vertexBase = j * 5;
                float sini = (float) Math.sin(angleStepI * i);
                float sinj = (float) Math.sin(angleStepJ * j);
                float cosi = (float) Math.cos(angleStepI * i);
                float cosj = (float) Math.cos(angleStepJ * j);

                // vertex x,y,z
                float dx = x + r * sini * sinj;
                float dy = y + r * sini * cosj;
                float dz = z + r * cosi;
                vLineBuffer[vertexBase + 0] = dx;
                vLineBuffer[vertexBase + 1] = dy;
                vLineBuffer[vertexBase + 2] = dz;
                float ds = - sini / cosi * cosj;
                float dt = sini / cosi * sinj;

                for(CubeFace f : faces) {
                    //remember, things may get overwritten if you have > 1 face in faces
                    float[] temp = f.getSAndT(dx, dy, dz, ds, dt);
                    if(temp!=null){
                        vLineBuffer[vertexBase + 3] = temp[0];
                        vLineBuffer[vertexBase + 4] = temp[1];
                        break;
                    }
                    //else {
                        //vLineBuffer[vertexBase + 3] = (float) j / (float) nSlices;
                        //vLineBuffer[vertexBase + 4] = (1.0f - i) / (float) nSlices;
                    //}
                }
            }
            mVertices.put(vLineBuffer, 0, vLineBuffer.length);
        }

        short[] indexBuffer = new short[max(mNumIndices)];
        int index = 0;
        int bufferNum = 0;
        for (int i = 0; i < nSlices; i++) {
            for (int j = 0; j < nSlices; j++) {
                int i1 = i + 1;
                int j1 = j + 1;
                if (index >= mNumIndices[bufferNum]) {
                    // buffer ready for moving to target
                    mIndices[bufferNum].put(indexBuffer, 0, mNumIndices[bufferNum]);
                    // move to the next one
                    index = 0;
                    bufferNum++;
                }
                indexBuffer[index++] = (short) (i * iMax + j);
                indexBuffer[index++] = (short) (i1 * iMax + j);
                indexBuffer[index++] = (short) (i1 * iMax + j1);
                indexBuffer[index++] = (short) (i * iMax + j);
                indexBuffer[index++] = (short) (i1 * iMax + j1);
                indexBuffer[index++] = (short) (i * iMax + j1);
            }
        }
        mIndices[bufferNum].put(indexBuffer, 0, mNumIndices[bufferNum]);
        mVertices.position(0);
        for (int i = 0; i < numIndexBuffers; i++) {
            mIndices[i].position(0);
        }
    }
    public FloatBuffer getVertices() {
        return mVertices;
    }
    public int getVerticesStride() {
        return 5*FLOAT_SIZE;
    }
    public ShortBuffer[] getIndices() {
        return mIndices;
    }
    public int[] getNumIndices() {
        return mNumIndices;
    }
    public int getTotalIndices() {
        return mTotalIndices;
    }
    private int max(int[] array) {
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }
        return max;
    }
}
