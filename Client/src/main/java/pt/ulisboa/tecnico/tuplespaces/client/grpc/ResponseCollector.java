package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.List;

public class ResponseCollector<T> {
    private List<T> responses = new ArrayList<>();

    private int numResponsesFailed = 0;

    public ResponseCollector() {
    }

    synchronized public void addResponse(T response) {
        responses.add(response);
        notifyAll();
    }

    synchronized public List<T> getResponses() {
        return responses;
    }

    synchronized public T getFirstResponse() {
        return responses.get(0);
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (responses.size() + numResponsesFailed < n)
            wait();
    }

    public int getNumResponsesFailed() {
        return numResponsesFailed;
    }

    synchronized void incrementNumResponsesFailed(){
        this.numResponsesFailed++;
    }
}