package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.stub.StreamObserver;


public class ClientObserver<T> implements StreamObserver<T> {
    private final ResponseCollector<T> collector;

    public ClientObserver(ResponseCollector<T> c) {
        collector = c;
    }

    @Override
    public void onNext(T response) {
        collector.addResponse(response);
    }


    @Override
    public void onError(Throwable throwable) {
        collector.incrementNumResponsesFailed();
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
    }
}
