package org.openrndr.shape.tessellation;

import org.openrndr.math.Vector2;

import java.util.ArrayList;

public class Tessellator extends GLUtessellatorImpl {

    public ArrayList<Primitive> primitives = new ArrayList<>();

    public Tessellator() {
        GLUtessellatorCallbackAdapter callback = new GLUtessellatorCallbackAdapter() {
            @Override
            public void begin(int type) {
                Tessellator.this.primitives.add(new Primitive(type));
            }

//            @Override
//            public void vertexD(Object vertexData) {
//                if (vertexData != null) {
//                    double[] coords = (double[]) vertexData;
//                    primitives.get(primitives.size() - 1).positions.add(new Vector2(coords[0], coords[1]));
//                } else {
//                    System.out.println("yo vertexData is null!");
//                }
//            }


            @Override
            public void vertexData(Object vertexData, Object polygonData) {

                double[] data = (double[]) vertexData;

                primitives.get(primitives.size()-1).positions.add(new Vector2(data[0], data[1]));

            }

            @Override
            public void error(int errnum) {
                throw new RuntimeException("GLU Error " + GLU.gluErrorString(errnum));
            }


            @Override
            public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
                double[] result = new double[3];
                result[0] = coords[0];
                result[1] = coords[1];
                result[2] = coords[2];
                outData[0] = coords;
            }
        };
        this.gluTessCallback(GLU.GLU_TESS_BEGIN, callback);
        this.gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, callback);
        this.gluTessCallback(GLU.GLU_TESS_ERROR, callback);
        this.gluTessCallback(GLU.GLU_TESS_COMBINE, callback);
    }

}
