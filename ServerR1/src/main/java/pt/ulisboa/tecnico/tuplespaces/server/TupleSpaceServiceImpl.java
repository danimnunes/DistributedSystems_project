package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
// xu liskov
// import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaTotalOrder;
// import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;

import java.util.List;
import java.util.regex.Matcher;

public class TupleSpaceServiceImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {
    private final TupleSpace tupleSpace;

    public TupleSpaceServiceImpl() {
        this.tupleSpace = new TupleSpace();
    }

    public TupleSpace getTupleSpace() {
        return tupleSpace;
    }

    @Override
    public void put(TupleSpacesReplicaTotalOrder.PutRequest request,
            StreamObserver<TupleSpacesReplicaTotalOrder.PutResponse> responseObserver) {
        try {
            String newTuple = request.getNewTuple();
            synchronized (this) {
                while (tupleSpace.getNextRequest() != request.getSeqNumber()) {
                    wait();
                    if (tupleSpace.getNextRequest() == request.getSeqNumber()) {
                        break;
                    }
                }
            }
            int putResult = getTupleSpace().put(newTuple);
            if (putResult == 0) {
                getTupleSpace().incrementNextRequest();
            }

            synchronized (this) {
                notifyAll();
            }

            TupleSpacesReplicaTotalOrder.PutResponse response = TupleSpacesReplicaTotalOrder.PutResponse.newBuilder()
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException | InterruptedException e) {
            // Handle your exception and convert it to gRPC Status
            Status status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            responseObserver.onError(new StatusRuntimeException(status));
        }
    }

    @Override
    public void read(TupleSpacesReplicaTotalOrder.ReadRequest request,
            StreamObserver<TupleSpacesReplicaTotalOrder.ReadResponse> responseObserver) {
        try {
            String searchPattern = request.getSearchPattern();
            String value = getTupleSpace().read(searchPattern);

            TupleSpacesReplicaTotalOrder.ReadResponse response = TupleSpacesReplicaTotalOrder.ReadResponse.newBuilder()
                    .setResult(value)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            Status status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            responseObserver.onError(new StatusRuntimeException(status));
        }
    }

    @Override
    public void take(TupleSpacesReplicaTotalOrder.TakeRequest request,
            StreamObserver<TupleSpacesReplicaTotalOrder.TakeResponse> responseObserver) {
        try {
            String searchPattern = request.getSearchPattern();
            synchronized (this) {
                while (tupleSpace.getNextRequest() != request.getSeqNumber()) {
                    wait();
                    if (tupleSpace.getNextRequest() == request.getSeqNumber()) {
                        break;
                    }
                }
            }
            String tuple = getTupleSpace().take(searchPattern);
            getTupleSpace().incrementNextRequest();
            synchronized (this) {
                notifyAll();
            }
            if (tuple == null) {
                synchronized (this) {
                    while (!checkCanTake(request)) {
                        wait();
                        if (checkCanTake(request)) {
                            tuple = getTupleSpace().take(searchPattern);
                            break;
                        }
                    }
                }
            }

            TupleSpacesReplicaTotalOrder.TakeResponse response = TupleSpacesReplicaTotalOrder.TakeResponse.newBuilder()
                    .setResult(tuple)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException | InterruptedException e) {
            Status status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            responseObserver.onError(new StatusRuntimeException(status));
        }
    }

    private boolean checkCanTake(TupleSpacesReplicaTotalOrder.TakeRequest request) {
        for (int i = 0; i < getTupleSpace().getTakeRequests().size(); i++) {
            int sequenceNumber = getTupleSpace().getTakeRequests().get(i).getSeqNumber();
            if (sequenceNumber == request.getSeqNumber()
                    && getTupleSpace().getTakeRequests().get(i).canTake()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getTupleSpacesState(TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest request,
            StreamObserver<TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse> responseObserver) {
        try {
            List<String> tuples = getTupleSpace().getTupleSpacesState();

            // Create a builder for the response message
            TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse.Builder responseBuilder = TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse
                    .newBuilder();

            // Add each tuple from the list to the response message
            for (String tuple : tuples) {
                responseBuilder.addTuple(tuple);
            }

            // Build the response message
            TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse response = responseBuilder.build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            // Handle other exceptions and convert them to gRPC Status
            Status status = Status.INTERNAL.withDescription("Internal Server Error: " + e.getMessage());
            responseObserver.onError(new StatusRuntimeException(status));
        }
    }
}
