package org.openrndr.shape.tessellation;

import org.openrndr.math.Vector2;

import java.util.ArrayList;

public class Primitive {
    /**
     * The OpenGL constant defining the type of this primitive
     *
     */
    public final int type;

    /**
     * A list of the indices of the vertices required to draw this primitive.
     *
     */
    public final ArrayList<Vector2> positions = new ArrayList<>();

    public Primitive(int type) {
        this.type = type;
    }


}