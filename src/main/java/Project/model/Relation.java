package Project.model;

import java.io.*;
import java.util.LinkedList;

/*
Author: Luis Reimer, Niklas Gerbes
 */
public record Relation(String parentID, String parentName, String childID, String childName) {

    // Function to load a tab seperated values file of format:
    // parentID \t parentName \t childID \t childName
    // and returns a list of instantiated Relation objects
    public static LinkedList<Relation> loadFromFile(File file) {
        LinkedList<Relation> relations = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                String parentID = parts[0].trim();
                String parentName = parts[1].trim();
                String childID = parts[2].trim();
                String childName = parts[3].trim();
                relations.add(new Relation(parentID, parentName, childID, childName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return relations;
    }
    public static LinkedList<Relation> loadFromFile(InputStream inputStream) {
        LinkedList<Relation> relations = new LinkedList<>();
        String filecontents = TreeLoader.readInputStream(inputStream);
        String[] lines = filecontents.split("\n");
        for (int i = 1; i < lines.length; i++) { // start from 1, skip header
            String line = lines[i];
            if (line.isBlank()) continue;
            String[] parts = line.split("\t");

            //edge case where there's an entry that represents only a node without relationship.
            //necessary since this is the way single childless root of a tree is saved. otherwise it would not be saved since it has no relationships.
            if (parts.length == 2) relations.add(new Relation(parts[0], parts[1], "", ""));
            else if (parts.length >3) {
                String parentID = parts[0].trim();
                String parentName = parts[1].trim();
                String childID = parts[2].trim();
                String childName = parts[3].trim();
                relations.add(new Relation(parentID, parentName, childID, childName));
            } else continue;
        }
        return relations;
    }
}
