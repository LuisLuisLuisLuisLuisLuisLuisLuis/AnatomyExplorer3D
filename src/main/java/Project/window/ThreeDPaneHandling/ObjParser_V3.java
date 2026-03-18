package Project.window.ThreeDPaneHandling;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.*;
import java.util.*;

public class ObjParser_V3 {

    public static TriangleMesh load(InputStream inputStream) throws IOException{
        IntegerProperty integerProperty = new SimpleIntegerProperty(0);
        IntegerProperty nosafe = new SimpleIntegerProperty(0);
        // OBJ raw data
        var objVertices = new ArrayList<Float>();
        var objNormals = new ArrayList<Float>();
        var objTexCoords = new ArrayList<Float>();

        // Final mesh data (deduplicated)
        var meshPoints = new ArrayList<Float>();
        var meshNormals = new ArrayList<Float>();
        var meshTexCoords = new ArrayList<Float>();
        var meshFaces = new ArrayList<Integer>();

        // Deduplication map
        Map<VertexKey, Integer> vertexMap = new HashMap<>();

        boolean hasNormals = false;
        boolean hasTexCoords = false;

        try (var br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {

                String[] tokens = line.trim().split("\\s+");
                if (tokens.length == 0) continue;

                switch (tokens[0]) {
                    case "v":
                        objVertices.add(Float.parseFloat(tokens[1]));
                        objVertices.add(Float.parseFloat(tokens[2]));
                        objVertices.add(Float.parseFloat(tokens[3]));
                        break;

                    case "vn":
                        hasNormals = true;
                        objNormals.add(Float.parseFloat(tokens[1]));
                        objNormals.add(Float.parseFloat(tokens[2]));
                        objNormals.add(Float.parseFloat(tokens[3]));
                        break;

                    case "vt":
                        hasTexCoords = true;
                        objTexCoords.add(Float.parseFloat(tokens[1]));
                        objTexCoords.add(1 - Float.parseFloat(tokens[2])); // flip V
                        break;

                    case "f":
                        int n = tokens.length - 1;

                        int[] vIndices = new int[n];
                        int[] tIndices = new int[n];
                        int[] nIndices = new int[n];

                        for (int i = 0; i < n; i++) {
                            String[] parts = tokens[i + 1].split("/");

                            vIndices[i] = Integer.parseInt(parts[0]) - 1;

                            if (parts.length >= 2 && !parts[1].isEmpty()) {
                                tIndices[i] = Integer.parseInt(parts[1]) - 1;
                            } else {
                                tIndices[i] = -1; // mark as "no texcoord"
                            }

                            nIndices[i] = (parts.length == 3 && !parts[2].isEmpty())
                                    ? Integer.parseInt(parts[2]) - 1
                                    : 0;
                        }

                        // triangulate (fan)
                        for (int i = 1; i < n - 1; i++) {
                            addFaceVertex(vIndices[0], tIndices[0], nIndices[0],
                                    objVertices, objTexCoords, objNormals,
                                    meshPoints, meshTexCoords, meshNormals,
                                    meshFaces, vertexMap, hasNormals, hasTexCoords, integerProperty, nosafe);

                            addFaceVertex(vIndices[i], tIndices[i], nIndices[i],
                                    objVertices, objTexCoords, objNormals,
                                    meshPoints, meshTexCoords, meshNormals,
                                    meshFaces, vertexMap, hasNormals, hasTexCoords, integerProperty, nosafe);

                            addFaceVertex(vIndices[i + 1], tIndices[i + 1], nIndices[i + 1],
                                    objVertices, objTexCoords, objNormals,
                                    meshPoints, meshTexCoords, meshNormals,
                                    meshFaces, vertexMap, hasNormals, hasTexCoords, integerProperty, nosafe);
                        }
                        break;
                }
            }
        }

        if (meshTexCoords.isEmpty()) {
            meshTexCoords.add(0.0f);
            meshTexCoords.add(0.0f);
        }

        TriangleMesh mesh = new TriangleMesh();

        mesh.getPoints().setAll(toFloatArray(meshPoints));
        mesh.getTexCoords().setAll(toFloatArray(meshTexCoords));
        mesh.getFaces().setAll(toIntArray(meshFaces));

        if (hasNormals && !meshNormals.isEmpty()) {
            mesh.getNormals().setAll(toFloatArray(meshNormals));
            mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);
        }
        System.err.println("Saved " + integerProperty.get() + " out of " + (nosafe.get() + integerProperty.get()) + " times");
        System.err.println("unique vertices: " + vertexMap.size() + ". normals count: " + meshNormals.size() / 3);

        return mesh;
    }

    private static void addFaceVertex(
            int v, int vt, int vn,
            List<Float> objVertices,
            List<Float> objTexCoords,
            List<Float> objNormals,
            List<Float> meshPoints,
            List<Float> meshTexCoords,
            List<Float> meshNormals,
            List<Integer> meshFaces,
            Map<VertexKey, Integer> vertexMap,
            boolean hasNormals,
            boolean hasTexCoords,
            IntegerProperty dedup,
            IntegerProperty nosafe
    ) {

        VertexKey key = new VertexKey(v, hasTexCoords ? vt : -1, hasNormals ? vn : -1);

        Integer index = vertexMap.get(key);
        if (index == null) {
            nosafe.set(nosafe.get()+1);
            index = vertexMap.size();
            vertexMap.put(key, index);

            // position
            meshPoints.add(objVertices.get(3 * v));
            meshPoints.add(objVertices.get(3 * v + 1));
            meshPoints.add(objVertices.get(3 * v + 2));

            // texcoord
            if (hasTexCoords) {
                meshTexCoords.add(objTexCoords.get(2 * vt));
                meshTexCoords.add(objTexCoords.get(2 * vt + 1));
            }

            // normal
            if (hasNormals) {
                meshNormals.add(objNormals.get(3 * vn));
                meshNormals.add(objNormals.get(3 * vn + 1));
                meshNormals.add(objNormals.get(3 * vn + 2));
            }
        } else {
            dedup.set(dedup.get()+1);
        }

        // JavaFX face format: (pointIndex, texCoordIndex)
        meshFaces.add(index);

        if (hasNormals) meshFaces.add(index);

        // texCoord index
        if (hasTexCoords) {
            meshFaces.add(index);
        } else {
            meshFaces.add(0); // always reference at least the single dummy texcoord
        }
    }

    private static float[] toFloatArray(List<Float> list) {
        var array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private static int[] toIntArray(List<Integer> list) {
        var array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private static class VertexKey {
        final int v, vt, vn;

        VertexKey(int v, int vt, int vn) {
            this.v = v;
            this.vt = vt;
            this.vn = vn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VertexKey)) return false;
            VertexKey key = (VertexKey) o;
            return v == key.v && vt == key.vt && vn == key.vn;
        }

        @Override
        public int hashCode() {
            return Objects.hash(v, vt, vn);
        }
    }
}
