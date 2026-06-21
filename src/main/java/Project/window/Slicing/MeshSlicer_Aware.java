package Project.window.Slicing;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.*;
import java.util.function.Function;

public class MeshSlicer_Aware {

    public static TriangleMesh slicePositiveSide(
            TriangleMesh input,
            double nx, double ny, double nz,
            double d,
            SimpleBooleanProperty modified
    ) {
        modified.set(false);
        SimpleIntegerProperty edgeRedundancyCounter = new SimpleIntegerProperty(0);
        SimpleIntegerProperty edgeCounter = new SimpleIntegerProperty(0);
        VertexFormat vertexFormat = input.getVertexFormat();

        // --- Determine face layout ---
        // crucial for correctly parsing the faces because:
        // POINT_NORMAL_TEXCOORD means every vertex in a face is represented by 3 ints (so 9 ints for a face of 3 vertices), while
        // POINT_TEXCOORD means every face is represented by 2 ints (so 6 ints for a face of 3 vertices)
        final boolean hasNormals = (vertexFormat == VertexFormat.POINT_NORMAL_TEXCOORD);
        // -> set the step size
        final int VERT_STRIDE = hasNormals ? 3 : 2;
        final int FACE_STRIDE = 3 * VERT_STRIDE; // 6 or 9

        float[] inPts   = input.getPoints().toArray(null);
        float[] inTex   = input.getTexCoords().toArray(null);
        float[] inNorms = hasNormals ? input.getNormals().toArray(null) : null;
        int[] inFaces = input.getFaces().toArray(null);

        // --- Output buffers ---
        ArrayList<Float> outPts = new ArrayList<>();
        ArrayList<Float> outTex = new ArrayList<>();
        ArrayList<Float> outNorms = hasNormals ? new ArrayList<>() : null;
        ArrayList<Integer>outFaces = new ArrayList<>();

        // Copy texcoords (if empty, add dummy)
        if (inTex.length == 0) {
            outTex.add(0f);
            outTex.add(0f);
        } else {
            for (float texcoord : inTex) outTex.add(texcoord);
        }

        // Copy normals table if present
        if (hasNormals) {
            if (inNorms.length == 0) {
                // dummy normal
                outNorms.add(0f);
                outNorms.add(1f);
                outNorms.add(0f);
            } else {
                for (float v : inNorms) outNorms.add(v);
            }
        }

        // Map original point index -> new point index
        // this means you can also check if an original point/Vertex v (with index i) has been yet added to the list of outgoing points (where it may have index j)
        Map<Integer,Integer> pointMap = new HashMap<>();

        class Dist {
            double eval(double x,double y,double z) {
                return nx*x + ny*y + nz*z - d;
            }
        }
        Dist dist = new Dist();

        // --- Add an original vertex to the list of outgoing points, but only if it's not already been added---
        Function<Integer, Integer> addPoint = new Function<>() {
            /**
             * Retrieve a point of inPts and if not already present in outPts: add it to outPts and create an entry
             * [pIdx, new index of point in outPts] in pointMap.
             * @param pIdx index of a point in the list of inPts. Note that a point consists of three numbers and
             *             inPts stores for every point those three numbers one after another. Thus, the three numbers
             *             making up point i will be found at indices i*3, i*3+1 and i*3+2.
             * @return the already existing or now computed value associated with the specified key (pIdx)
             */
            @Override
            public Integer apply(Integer pIdx) {
                return pointMap.computeIfAbsent(pIdx, idx -> {
                    int b = idx * 3;
                    outPts.add(inPts[b]);
                    outPts.add(inPts[b+1]);
                    outPts.add(inPts[b+2]);
                    return (outPts.size()/3) - 1;   // divide by 3 again since we don't want the exact index of the numbers in outPts
                });                                 // but we want the rank of the point, which will have to be multiplied by 3 again
            }                                       // to access the correct values in outPts.
        };


        // --- Add intersection vertex ---
        class AddIntersection {
            /**
             * Create a new point at the intersection between the line a->b and the plane given by F.
             * @param a Rank (not index) of point A in inPts
             * @param b Rank (not index) of point B in inPts.
             * @param sa Distance of A to Point F.
             * @param sb Distance of B to Point F.
             * @return The rank I of that point in outPts. Obtain the coords of that point at outPts[I*3], [I*3 + 1] and [I*3 + 2].
             */
            int apply(int a, int b, double sa, double sb) {

                int ia = a*3, ib = b*3; // list indices
                double x0 = inPts[ia],   y0 = inPts[ia+1], z0 = inPts[ia+2];    // retrieve the point coords from inPts
                double x1 = inPts[ib],   y1 = inPts[ib+1], z1 = inPts[ib+2];

                // next, find the intersection between A->B and the plane given by Point F.
                // it is given by A + t * (B-A) for this t:
                double t = sa / (sa - sb);

                double xi = x0 + t*(x1-x0); // create a new point using t.
                double yi = y0 + t*(y1-y0);
                double zi = z0 + t*(z1-z0);

                outPts.add((float)xi);  // add the new point to the outPts
                outPts.add((float)yi);
                outPts.add((float)zi);

                return (outPts.size()/3) - 1;   // the rank of the new point (by which it can be accessed in outPts via newIdx * 3)
            }
        }
        AddIntersection addIntersection = new AddIntersection();


        class AddFace {
            /**
             * conveniance class to write a triangle (face) to outFaces. uses the correct format.
             * @param p0 Index of point p0. Same for p1, p2.
             * @param n0 Index of Normal for p0. Same for n1, n2.
             * @param t0 Index of texture coordinate of p0 in outTex. Same for p1, p2.
             */
            void apply(int p0,int n0,int t0,
                         int p1,int n1,int t1,
                         int p2,int n2,int t2) {
                // choose the right format
                if (!hasNormals) {
                    // POINT_TEXCOORD
                    outFaces.add(p0); outFaces.add(t0);
                    outFaces.add(p1); outFaces.add(t1);
                    outFaces.add(p2); outFaces.add(t2);
                } else {
                    // POINT_NORMAL_TEXCOORD
                    outFaces.add(p0); outFaces.add(n0); outFaces.add(t0);
                    outFaces.add(p1); outFaces.add(n1); outFaces.add(t1);
                    outFaces.add(p2); outFaces.add(n2); outFaces.add(t2);
                }
            }
        }
        AddFace addFace = new AddFace();


        // --- Main loop over triangles ---
        for (int f = 0; f < inFaces.length; f += FACE_STRIDE) {

            // Read faces according to format: p -> point, n -> normal, t -> texcoord. (All indices for the lists)
            int p0 = inFaces[f];
            int n0 = hasNormals ? inFaces[f+1] : 0;
            int t0 = hasNormals ? inFaces[f+2] : inFaces[f+1];

            int p1 = inFaces[f+VERT_STRIDE];
            int n1 = hasNormals ? inFaces[f+VERT_STRIDE+1] : 0;
            int t1 = hasNormals ? inFaces[f+VERT_STRIDE+2] : inFaces[f+VERT_STRIDE+1];

            int p2 = inFaces[f+2*VERT_STRIDE];
            int n2 = hasNormals ? inFaces[f+2*VERT_STRIDE+1] : 0;
            int t2 = hasNormals ? inFaces[f+2*VERT_STRIDE+2] : inFaces[f+2*VERT_STRIDE+1];

            // Signed distances for all three Points to the fixed given Point.
            double s0 = dist.eval(inPts[p0*3], inPts[p0*3+1], inPts[p0*3+2]);
            double s1 = dist.eval(inPts[p1*3], inPts[p1*3+1], inPts[p1*3+2]);
            double s2 = dist.eval(inPts[p2*3], inPts[p2*3+1], inPts[p2*3+2]);

            boolean in0 = s0 >= 0;  // find out on which side they lie. ('inside' or not, keep inside points)
            boolean in1 = s1 >= 0;
            boolean in2 = s2 >= 0;

            int inside = (in0?1:0)+(in1?1:0)+(in2?1:0); // count how many of them are kept.

            if (!modified.get()) modified.set(inside != 3);

            if (inside == 0) continue;

            // This triangle can be kept entirely.
            if (inside == 3) {
                addFace.apply(
                        addPoint.apply(p0), n0, t0,
                        addPoint.apply(p1), n1, t1,
                        addPoint.apply(p2), n2, t2
                );
                continue;
            } else if (inside == 1) {   // only one of them can be kept.


                int pin, nin, tin;          // Placeholder for the single inside vertex,
                int pout1, nout1, tout1;    // the other two will be discarded.
                int pout2, nout2, tout2;
                double sin, sout1, sout2;   // distance placeholders

                // fill in the placeholder values depending on which of the points is the one to keep.
                if (in0) {
                    pin=p0; nin=n0; tin=t0; sin=s0;
                    pout1=p1; nout1=n1; tout1=t1; sout1=s1;
                    pout2=p2; nout2=n2; tout2=t2; sout2=s2;
                }
                else if (in1) {
                    pin=p1; nin=n1; tin=t1; sin=s1;
                    pout1=p2; nout1=n2; tout1=t2; sout1=s2;
                    pout2=p0; nout2=n0; tout2=t0; sout2=s0;
                }
                else {
                    pin=p2; nin=n2; tin=t2; sin=s2;
                    pout1=p0; nout1=n0; tout1=t0; sout1=s0;
                    pout2=p1; nout2=n1; tout2=t1; sout2=s1;
                }

                // Keep the inside vertex
                int vIn = addPoint.apply(pin);

                // Two new points must be computed. They lie where the plane given by F intersects with pin->pout1/2
                // they will create a new (smaller) triangle with pin.
                int vI1 = addIntersection.apply(pin, pout1, sin, sout1);
                int vI2 = addIntersection.apply(pin, pout2, sin, sout2);

                // Intersection attributes (simple placeholders)
                int nI = 0;
                int tI = 0;

                // Add the new triangle.
                addFace.apply(
                        vIn, nin, tin,
                        vI1, nI,  tI,
                        vI2, nI,  tI
                );
                continue;

            } else if (inside == 2) {

                int pout, nout, tout;   //Placeholders for the two kept vertices and the discarded one.
                int pin1, nin1, tin1;
                int pin2, nin2, tin2;

                double sout, sin1, sin2;

                // fill in
                if (!in0) {
                    pout=p0; nout=n0; tout=t0; sout=s0;
                    pin1=p1; nin1=n1; tin1=t1; sin1=s1;
                    pin2=p2; nin2=n2; tin2=t2; sin2=s2;
                }
                else if (!in1) {
                    pout=p1; nout=n1; tout=t1; sout=s1;
                    pin1=p2; nin1=n2; tin1=t2; sin1=s2;
                    pin2=p0; nin2=n0; tin2=t0; sin2=s0;
                }
                else {
                    pout=p2; nout=n2; tout=t2; sout=s2;
                    pin1=p0; nin1=n0; tin1=t0; sin1=s0;
                    pin2=p1; nin2=n1; tin2=t1; sin2=s1;
                }

                // Keep inside vertices
                int vIn1 = addPoint.apply(pin1);
                int vIn2 = addPoint.apply(pin2);

                // Compute two new points at the intersections with the plane
                int vI1 = addIntersection.apply(pin1, pout, sin1, sout);
                int vI2 = addIntersection.apply(pin2, pout, sin2, sout);

                // Intersection attributes placeholders
                int nI = 0;
                int tI = 0;

                // Two kept points and two new ones create a quad, which must be split into two triangles.

                // Triangle 1: pin1, pin2, I2
                addFace.apply(
                        vIn1, nin1, tin1,
                        vIn2, nin2, tin2,
                        vI2,  nI,   tI
                );

                // Triangle 2: pin1, I2, I1
                addFace.apply(
                        vIn1, nin1, tin1,
                        vI2,  nI,   tI,
                        vI1,  nI,   tI
                );

                continue;
            }

        }


        TriangleMesh out = new TriangleMesh(vertexFormat);

        out.getPoints().setAll(toFloatArray(outPts));
        out.getTexCoords().setAll(toFloatArray(outTex));

        if (hasNormals) {
            out.getNormals().setAll(toFloatArray(outNorms));
        }

        out.getFaces().setAll(toIntArray(outFaces));

        // Smooth shading
        int faceCount = outFaces.size() / FACE_STRIDE;
        int[] smoothing = new int[faceCount];
        Arrays.fill(smoothing, 1);
        out.getFaceSmoothingGroups().setAll(smoothing);

        return out;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i=0;i<list.size();i++) arr[i]=list.get(i);
        return arr;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i=0;i<list.size();i++) arr[i]=list.get(i);
        return arr;
    }

}
