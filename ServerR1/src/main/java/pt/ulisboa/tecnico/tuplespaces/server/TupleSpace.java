package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TupleSpace {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private final List<String> tuples;

    private int nextRequest = 1;
    private final List<Request> takeRequests;

    public TupleSpace() {
        this.tuples = new ArrayList<>();
        this.takeRequests = new ArrayList<>();
    }

    public int getNextRequest() {
        return nextRequest;
    }

    public void incrementNextRequest() {
        this.nextRequest++;
    }

    public List<String> getTuples() {
        return tuples;
    }

    public List<Request> getTakeRequests() {
        return takeRequests;
    }

    public synchronized int put(String tuple) throws IllegalArgumentException {
        if (inputIsValid(tuple)) {
            getTuples().add(tuple);

            Pattern pattern = Pattern.compile(tuple);
            int lowestSeqNumberIndex = -1;

            for (int i = 0; i < takeRequests.size(); i++) {
                Request request = takeRequests.get(i);
                Matcher matcher = pattern.matcher(request.getTuple());
                if (matcher.find()) {
                    if (lowestSeqNumberIndex == -1
                            || request.getSeqNumber() < takeRequests.get(lowestSeqNumberIndex).getSeqNumber()) {
                        if (lowestSeqNumberIndex != -1) {
                            takeRequests.get(lowestSeqNumberIndex).setCanTake(false);
                        }
                        lowestSeqNumberIndex = i;
                    } else {
                        request.setCanTake(false);
                    }
                }
            }

            if (lowestSeqNumberIndex != -1) {
                takeRequests.get(lowestSeqNumberIndex).setCanTake(true);
                return 1;
            } else {
                return 0;
            }
        } else {
            throw new IllegalArgumentException("Invalid Entry: " + tuple);
        }
    }

    public synchronized String read(String searchPattern) throws IllegalArgumentException {
        if (inputIsValid(searchPattern)) {
            Pattern pattern = Pattern.compile(searchPattern);
            while (true) {
                for (String tuple : tuples) {
                    Matcher matcher = pattern.matcher(tuple);
                    if (matcher.find()) {
                        return tuple;
                    }
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid search pattern: " + searchPattern);
        }
    }

    public synchronized String take(String searchPattern) throws IllegalArgumentException {
        if (inputIsValid(searchPattern)) {
            Pattern pattern = Pattern.compile(searchPattern);
            for (String tuple : new ArrayList<>(getTuples())) {
                Matcher matcher = pattern.matcher(tuple);
                if (matcher.find()) {
                    getTuples().remove(tuple);
                    // Verificar se há pedidos de take pendentes correspondentes a este tuplo
                    for (int i = 0; i < takeRequests.size(); i++) {
                        Request request = takeRequests.get(i);
                        if (pattern.matcher(request.getTuple()).find()) {
                            takeRequests.remove(i);
                            return tuple;
                        }
                    }
                    return tuple;
                }
            }
            // Nenhum tuplo correspondente disponível, colocar o pedido de take na lista de
            // espera
            takeRequests.add(new Request(nextRequest, searchPattern));
            return null; // Retorna null para indicar que o pedido está na lista de espera
        } else {
            throw new IllegalArgumentException("Invalid search pattern: " + searchPattern);
        }
    }

    public synchronized List<String> getTupleSpacesState() {
        return new ArrayList<>(getTuples());
    }

    private boolean inputIsValid(String input) {
        return input.substring(0, 1).equals(BGN_TUPLE) && input.endsWith(END_TUPLE);
    }

}
