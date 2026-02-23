package Project.model;

import java.util.ArrayList;

//a sorting class. there is just arraylist mergesort right now.
public class Sorting<T extends Comparable<T>> {

    //just mergesort
    public final ArrayList<T> mergeSort(ArrayList<T> arr) {
        if (arr.size() <= 1) {
            return arr;
        }
        ArrayList<T> a1 = new ArrayList<>(arr.size());
        ArrayList<T> a2 = new ArrayList<>(arr.size()/2);

        for (int i = 0; i < arr.size(); i++) {
            if (i < arr.size() / 2) {
                a1.add(arr.get(i));
            } else {
                a2.add(arr.get(i));
            }
        }

        a1 = mergeSort(a1);
        a2 = mergeSort(a2);
        return merge(a1, a2);
    }

    //utility for mergesort
    public final  ArrayList<T> merge(ArrayList<T> a1, ArrayList<T> a2) {
        ArrayList<T> c = new ArrayList<>(a1.size() + a2.size());
        while (!a1.isEmpty() && !a2.isEmpty()) {
            if (a1.getFirst().compareTo(a2.getFirst()) < 0) {
                c.add(a1.getFirst());
                a1.removeFirst();
            } else {
                c.add(a2.getFirst());
                a2.removeFirst();
            }
        }
        if (!a1.isEmpty()) {
            c.addAll(a1);
        } else c.addAll(a2);
        return c;
    }
}
