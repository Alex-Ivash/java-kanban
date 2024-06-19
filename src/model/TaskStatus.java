package model;

public enum TaskStatus {
    NEW(0),
    IN_PROGRESS(1),
    DONE(2);

    public final int bitSetIndex;

    TaskStatus(int bitSetIndex) {
        this.bitSetIndex = bitSetIndex;
    }
}
