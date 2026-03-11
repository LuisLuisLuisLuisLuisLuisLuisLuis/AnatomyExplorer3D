package Project.model;

import Project.model.ANode;
import javafx.scene.control.TreeItem;

import java.util.*;

public class EnableDisableBP3DV3Parts {


    /**
     * Creates a mapping of node ID to fileID for all nodes that I import from BP3D 3.0 into my tree.
     * @return the dict.
     */
    public static HashMap<String, Set<String>> createIdToFileIDsMap() {
        HashMap<String, Set<String>> idToFileIDs = new HashMap<>();
        for (String line : importedNodesFileList.split("\n")) {
            String[] fields = line.split("\t");
            Set<String> fileIDs = idToFileIDs.getOrDefault(fields[0], new HashSet<>());
            fileIDs.add(fields[2]);
            idToFileIDs.put(fields[0], fileIDs);
        }
        return idToFileIDs;
    }

    /**
     * Takes a mapping of node id to fileID and adds those fileIDs to the matching ANodes in the tree.
     * @param root root of tree
     * @param idToFileID Mapping of node id to a set of fileIDs
     */
    public static void addV3FilesToTree(TreeItem<ANode> root, HashMap<String, Set<String>> idToFileID) {
        if (idToFileID.containsKey(root.getValue().conceptId())) {
            root.getValue().fileIds().addAll(idToFileID.get(root.getValue().conceptId()));
        }
        for (TreeItem<ANode> child : root.getChildren()) addV3FilesToTree(child, idToFileID);
    }

    /**
     * Takes a mapping of node id to fileID and removes those fileIDs from the matching ANodes in the tree.
     * @param root root of tree
     * @param idToFileIDs Mapping of node id to a set of fileIDs
     */
    public static void removeV3FilesFromTree(TreeItem<ANode> root, HashMap<String, Set<String>> idToFileIDs) {
        if (idToFileIDs.containsKey(root.getValue().conceptId())) {
            root.getValue().fileIds().removeAll(idToFileIDs.get(root.getValue().conceptId()));
        }
        for (TreeItem<ANode> child : root.getChildren()) removeV3FilesFromTree(child, idToFileIDs);
    }

    private static final String importedNodesFileList = """
FMA46841	orbicularis oris	FMA46841
FMA55606	right nasalis	FMA55606
FMA55607	left nasalis	FMA55607
FMA46839	right risorius	FMA46839
FMA46840	left risorius	FMA46840
FMA49012	right medial pterygoid	FMA49012
FMA49013	left medial pterygoid	FMA49013
FMA46836	left buccinator	FMA46836
FMA46835	right buccinator	FMA46835
FMA46812	right zygomaticus major	FMA46812
FMA46813	left zygomaticus major	FMA46813
FMA55611	left procerus	FMA55611
FMA55610	right procerus	FMA55610
FMA46824	left levator anguli oris	FMA46824
FMA46823	right levator anguli oris	FMA46823
FMA55609	left depressor septi nasi	FMA55609
FMA55608	right depressor septi nasi	FMA55608
FMA46814	right zygomaticus minor	FMA46814
FMA46815	left zygomaticus minor	FMA46815
FMA46785	palpebral part of right orbicularis oculi	FMA46785
FMA46786	palpebral part of left orbicularis oculi	FMA46786
FMA46782	orbital part of right orbicularis oculi	FMA46782
FMA46783	orbital part of left orbicularis oculi	FMA46783
FMA49001	superficial part of right masseter	FMA49001
FMA49002	superficial part of left masseter	FMA49002
FMA49004	deep part of right masseter	FMA49004
FMA49005	deep part of left masseter	FMA49005
FMA46806	right levator labii superioris	FMA46806
FMA46807	left levator labii superioris	FMA46807
FMA46804	left levator labii superioris alaeque nasi	FMA46804
FMA46803	right levator labii superioris alaeque nasi	FMA46803
FMA46818	left depressor labii inferioris	FMA46818
FMA46817	right depressor labii inferioris	FMA46817
FMA46830	left depressor anguli oris	FMA46830
FMA46829	right depressor anguli oris	FMA46829
FMA46759	right frontalis	FMA46759
FMA46760	left frontalis	FMA46760
FMA46764	left temporoparietalis	FMA46764
FMA46763	right temporoparietalis	FMA46763
FMA49024	upper head of right lateral pterygoid	FMA49024
FMA49025	upper head of left lateral pterygoid	FMA49025
FMA46762	left occipitalis	FMA46762
FMA46761	right occipitalis	FMA46761
FMA49023	lower head of left lateral pterygoid	FMA49023
FMA49022	lower head of right lateral pterygoid	FMA49022
FMA46826	right mentalis	FMA46826
FMA46827	left mentalis	FMA46827
FMA22782	left spinalis cervicis	FMA22782
FMA22781	right spinalis cervicis	FMA22781
FMA46286	vertical intermediate part of left longus colli	FJ1601
FMA46285	vertical intermediate part of right longus colli	FMA46285
FMA46288	inferior oblique part of left longus colli	FJ1557
FMA46287	inferior oblique part of right longus colli	FMA46287
FMA46284	superior oblique part of left longus colli	FJ1600
FMA46283	superior oblique part of right longus colli	FMA46283
FMA46309	right longus capitis	FMA46309
FMA46310	left longus capitis	FMA46310
FMA46313	right rectus capitis anterior	FMA46313
FMA46314	left rectus capitis anterior	FMA46314
FMA46317	right rectus capitis lateralis	FMA46317
FMA46318	left rectus capitis lateralis	FMA46318
FMA46768	aponeurosis of epicranius	FMA46768
FMA21964	right inguinal ligament	FMA21964
FMA74140	set of dorsal interossei of right foot	FMA74140
FMA51142	right extensor digitorum brevis	FMA51142
FMA13358	right latissimus dorsi	FMA13358
FMA13359	left latissimus dorsi	FMA13359
FMA22878	right multifidus	FMA22878
FMA22879	left multifidus	FMA22879
FMA46442	tendinous arch of levator ani	FMA46442
FMA74141	set of dorsal interossei of left foot	FMA74141
FMA51143	left extensor digitorum brevis	FMA51143
FMA21965	left inguinal ligament	FMA21965
BP49	left superior parietal lobule precuneus	BP49
FMA72702	left accessory short gyrus	FMA72702
BP50	right superior parietal lobule precuneus	BP50
FMA72701	right accessory short gyrus	FMA72701
FMA61822	white matter structure of cerebral hemisphere	FMA61822
""";
}
