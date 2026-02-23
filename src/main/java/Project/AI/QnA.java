package Project.AI;

import javafx.beans.property.SimpleStringProperty;

/**
 * Defines a Question and its Answer.
 */
public class QnA {
    private final SimpleStringProperty Q;

    public String getQ() {
        return Q.get();
    }

    public SimpleStringProperty qProperty() {
        return Q;
    }

    public String getA() {
        return A.get();
    }

    public SimpleStringProperty aProperty() {
        return A;
    }

    private final SimpleStringProperty A;

    public QnA(String Q, String A) {
        this.Q = new SimpleStringProperty(Q);
        this.A = new SimpleStringProperty(A);
    }
}
