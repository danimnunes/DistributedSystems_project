package pt.ulisboa.tecnico.tuplespaces.server;

public class Request {

    private final int seqNumber;
    private final String tuple;

    private boolean canTake = false;

    public Request(int seqNumber, String tuple) {
        this.seqNumber = seqNumber;
        this.tuple = tuple;
    }

    public boolean canTake() {
        return canTake;
    }

    public void setCanTake(boolean canTakeStatus) {
        this.canTake = canTakeStatus;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public String getTuple() {
        return tuple;
    }

}
