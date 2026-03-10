package Project.window.SupportingUI.TextSearch;

import Project.model.ANode;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Pattern;

public class SearchTree {

    private final String id;
    public String getId() {return id;}

    private StringProperty query = new SimpleStringProperty(""); //query which this class is currently working with if its a string
    private Pattern pattern;    //query if its regex
    private SimpleBooleanProperty regex = new SimpleBooleanProperty(false); //are we searching with regex?

    private final TreeView<ANode> treeView; //treeview to be searched
    private ObservableList<FoundText> undoStack = FXCollections.observableList(new LinkedList<>());
    private ObservableList<FoundText> redoStack = FXCollections.observableList(new LinkedList<>());
    private boolean changingTheTree = false;    // prevents infinite listener firing
    private SimpleBooleanProperty hasPrevious = new SimpleBooleanProperty(); //so that someone else can listen and use this as binding for buttons
    private SimpleBooleanProperty hasNext = new SimpleBooleanProperty();    //so that someone else can listen and use this as binding for buttons
    private SimpleBooleanProperty foundSomething = new SimpleBooleanProperty(true); //did the last search find something? (if no it would not make sense to click next button)

    public SimpleBooleanProperty getHasPreviousObservable() {
        return hasPrevious;
    }
    public SimpleBooleanProperty getCanNextObservable() {
        return hasNext;
    }

    private boolean busy = false;   //are we currently working? dont think its actually necessary

    private LinkedList<TreeItem<ANode>> bulkSelectList = new LinkedList<>();


    /**
     * Initialize by giving a treeview to work with.
     */
    public SearchTree(TreeView<ANode> treeView) {
        this.treeView = treeView;
        this.id = treeView.getId();
        treeView.getSelectionModel().getSelectedItems().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (!changingTheTree) {
                    clearThis();    //if the tree selection changes from outside, then the undo-redo should be cleared
                }
            }
        });
        hasPrevious.bind(Bindings.greaterThan(2, Bindings.size(undoStack)));    //initialize the observables
        hasNext.bind(query.isEmpty().not().and(foundSomething));


        //commented out debug stuff
        /*
        undoStack.addListener(new ListChangeListener<FoundText>() {
            @Override
            public void onChanged(Change<? extends FoundText> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        System.out.println("UndoList was extended to size " + undoStack.size() + " by:");
                        for (FoundText foundText : c.getAddedSubList()) {
                            System.out.println(foundText.treeItem().getValue().name() + " at pos " + foundText.counter());
                        }
                    }
                    if (c.wasRemoved()) {
                        System.out.println("UndoList was shortened to size " + undoStack.size() + " by:");
                        for (FoundText foundText : c.getRemoved()) {
                            System.out.println(foundText.treeItem().getValue().name() + " at pos " + foundText.counter());
                        }
                    }
                }
            }
        });
         */

        /*
        //more debug listener
        redoStack.addListener(new ListChangeListener<FoundText>() {
            @Override
            public void onChanged(Change<? extends FoundText> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        System.out.println("Redolist was extended to size " + redoStack.size() + " by:");
                        for (FoundText foundText : c.getAddedSubList()) {
                            System.out.println(foundText.treeItem().getValue().name() + " at pos " + foundText.counter());
                        }
                    }
                    if (c.wasRemoved()) {
                        System.out.println("Redolist was shortened to size " + redoStack.size() + " by:");
                        for (FoundText foundText : c.getRemoved()) {
                            System.out.println(foundText.treeItem().getValue().name() + " at pos " + foundText.counter());
                        }
                    }
                }
            }
        });
         */
    }


    /**
     * Clear everything in here.
     */
    private void clearThis() {
        undoStack.clear();
        redoStack.clear();
        query.set("");
    }

    /**
     * Find a query in the treeView.
     * @param query: String
     * @param all: select all matching treeItems?
     * @param regex: is the query to be treated as regex?
     * @return true if not busy
     */
    public boolean find(String query, boolean all, boolean regex) {

        if (!busy) {
            if (!all && Objects.equals(this.query.get(), query) && foundSomething.get() && (regex == this.regex.get())) next(); //if were in a next-scenario, call next() instead of new search
            else { //new search for query
                clearThis();
                this.regex.set(regex);
                if (regex) {
                    try {
                        this.pattern = Pattern.compile(query);
                    } catch (Exception e) {
                        return false;
                    }
                }
                this.query.set(query);
                busy = true;
                this.foundSomething.set(find(treeView.getRoot(), query, 0, new Counter(), all));
                if (all) bulkSelect();
                busy = false;
            }
            return true;
        }
        else return false;
    }

    /**
     * Recursive method to find one occurrence of the query in the tree by DFS.
     * @param treeItem: root of the tree (or current treeItem during recursion).
     * @param query: String
     * @param skip: how many treeItems to skip initially during the search.
     * @param counter: counts how many treeItems have already been visited.
     * @return true if query is found.
     */
    private boolean find(TreeItem<ANode> treeItem, String query, int skip, Counter counter, boolean all) {
        counter.plus();
        if (counter.get() >= skip) {
            //System.out.println("Checking " + treeItem.getValue().name() + " at pos " + counter.get());
            //if (treeItem.getValue().name().contains(query)) {
            if (match(treeItem.getValue().toString(), query, this.regex.get())) {
                //System.out.println("yes found"); //shows that all items are found!
                if (!all) {
                    undoStack.add(new FoundText(treeItem, counter.get()));
                    select(treeItem, true);
                    return true;
                } else {
                    bulkSelectList.add(treeItem);
                }

            }
        } // else System.out.println("Skipping " + treeItem.getValue().name() + "at pos " + counter.get());

        for (TreeItem<ANode> child : treeItem.getChildren()) {
            if (!all) {
                if (find(child, query, skip, counter, all)) return true;
            } else find(child, query, skip, counter, all);
        }
        return false;
    }

    public boolean match(String s1, String s2, boolean regex) {
        if (regex) { //discriminating between search styles because internet says contains() is faster
            return pattern.matcher(s1).find();
        } else return s1.contains(s2);
    }

//    private void bulkSelect() {
//        changingTheTree = true;
//        clearTreeSelection();
//        for (TreeItem<ANode> treeItem : bulkSelectList) {
//            expandParents(treeItem);    // if i dont do this before it fails to select nodes of collapsed parents.
//                                        // For some reason it also doesnt work if i merge these two loops....
//        }
//        for (TreeItem<ANode> treeItem : bulkSelectList) {
//            treeView.getSelectionModel().select(treeItem);
//        }
//        changingTheTree = false;
//        bulkSelectList.clear();
//    }

    private void bulkSelect() {
        changingTheTree = true;
        clearTreeSelection();
        if (bulkSelectList.isEmpty()) return;
        for (TreeItem<ANode> treeItem : bulkSelectList) expandParents(treeItem);


        if (bulkSelectList.size() == 1) {
            treeView.getSelectionModel().select(treeView.getRow(bulkSelectList.getFirst()));
            return;
        }
        int[] indicesToSelect = new int[bulkSelectList.size()-1];
        for (int i = 1; i < bulkSelectList.size(); i++) indicesToSelect[i-1] = treeView.getRow(bulkSelectList.get(i));
        treeView.getSelectionModel().selectIndices(treeView.getRow(bulkSelectList.getFirst()), indicesToSelect);
        changingTheTree = false;
        bulkSelectList.clear();
    }

    private void expandParents(TreeItem<ANode> treeItem) {
        TreeItem<ANode> parent = treeItem.getParent();
        while (parent != null) {
            parent.setExpanded(true);
            parent = parent.getParent();
        }
    }

    /**
     * Selecting a treeItem and scrolling to it while checked by changingTheTree.
     */
    private void select(TreeItem<ANode> treeItem, boolean clear) {
        changingTheTree = true;
        if (clear) treeView.getSelectionModel().clearSelection();
        treeView.getSelectionModel().select(treeItem);
        changingTheTree = false;
        treeView.scrollTo(treeView.getRow(treeItem));
    }

    /**
     * Clears the selection of the tree while checked by changingTheTree.
     */
    private void clearTreeSelection() {
        changingTheTree = true;
        treeView.getSelectionModel().clearSelection();
        changingTheTree = false;
    }

    /**
     * Go to previous hit.
     */
    public void previous() {
        if (undoStack.size() > 1) {
            clearTreeSelection();
            redoStack.add(undoStack.removeLast());
            select(undoStack.getLast().treeItem(), true);
            foundSomething.set(true);
        }
    }

    /**
     * Attempt to go to next hit, which is either in the redostack or can maybe be found by searching after the last hit in the tree.
     */
    public void next() {
        if (busy) return;
        if (!redoStack.isEmpty()) {
            clearTreeSelection();
            select(redoStack.getLast().treeItem(), true);
            undoStack.add(redoStack.removeLast());
        } else {
            if (query.isEmpty().get()) return;
            //System.out.println("Finding next after " + undoStack.getLast().treeItem().getValue().name() + " at pos " + undoStack.getLast().counter());
            FoundText previous = undoStack.getLast();
            busy = true;
            this.foundSomething.set(find(treeView.getRoot(), query.get(), previous.counter()+1, new Counter(), false));
            busy = false;
        }
    }

    /**
     * counter class. because Integers aren't referenced but only copied.
     */
    private class Counter {
        private int counter = 0;
        public final void plus() {
            counter++;
        }
        public int get() {
            return counter;
        }
    }
}
