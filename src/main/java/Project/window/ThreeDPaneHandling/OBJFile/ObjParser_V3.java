package Project.window.ThreeDPaneHandling.OBJFile;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.*;
import java.util.*;

/*
Adapted from Huson (2025)
 */
public class ObjParser_V3 {

    public static TriangleMesh load(InputStream inputStream, boolean useNormals) throws IOException {

        // OBJ data
        var objVertices = new ArrayList<Float>();
        var objNormals = new ArrayList<Float>();
        var objTexCoords = new ArrayList<Float>();

        var meshFaces = new ArrayList<Integer>();

        boolean hasNormals = false;

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
                        if (!useNormals) break;
                        hasNormals = true;
                        objNormals.add(Float.parseFloat(tokens[1]));
                        objNormals.add(Float.parseFloat(tokens[2]));
                        objNormals.add(Float.parseFloat(tokens[3]));
                        break;

                    case "vt":
                        objTexCoords.add(Float.parseFloat(tokens[1]));
                        objTexCoords.add(1 - Float.parseFloat(tokens[2]));
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
                                tIndices[i] = 0;
                            }

                            nIndices[i] = (parts.length == 3 && !parts[2].isEmpty())
                                    ? Integer.parseInt(parts[2]) - 1
                                    : 0;
                        }

                        // this loop triangulates faces that consist of > 3 points, i.e. turns a polygon into triangles
                        // because trianglemesh wants triangles.
                        for (int i = 1; i < n - 1; i++) {
                            if (hasNormals) Collections.addAll(meshFaces,
                                    vIndices[0], nIndices[0], tIndices[0],
                                    vIndices[i], nIndices[i], tIndices[i],
                                    vIndices[i + 1], nIndices[i + 1], tIndices[i + 1]);
                            else Collections.addAll(meshFaces,
                                    vIndices[0], tIndices[0],
                                    vIndices[i], tIndices[i],
                                    vIndices[i + 1], tIndices[i + 1]);

                        }
                        break;
                }
            }
        }

        if (objTexCoords.isEmpty()) {
            objTexCoords.add(0.0f);
            objTexCoords.add(0.0f);
        }

        TriangleMesh mesh = new TriangleMesh();

        mesh.getPoints().setAll(toFloatArray(objVertices));
        mesh.getTexCoords().setAll(toFloatArray(objTexCoords));
        mesh.getFaces().setAll(toIntArray(meshFaces));

        if (hasNormals && !objNormals.isEmpty()) {
            mesh.getNormals().setAll(toFloatArray(objNormals));
            mesh.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);
        }

        return mesh;
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

}