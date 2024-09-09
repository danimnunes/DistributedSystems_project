package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
// xu liskov
// import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
// import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaStub;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;

// total order
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaStub;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder;

import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

public class ClientService {

    /*
     * This should include a method that builds a channel and stub,
     * as well as individual methods for each remote operation of this service.
     */

    private final ManagedChannel[] channels;
    private final TupleSpacesReplicaStub[] stubs;

    private final ManagedChannel sequencerChannel;
    private final SequencerGrpc.SequencerBlockingStub sequencerStub;

    private final int numServers = 3;
    OrderedDelayer delayer;

    long seed = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    Random random = new Random(seed);

    public ClientService(String[] hosts, String[] ports) {

        delayer = new OrderedDelayer(numServers);

        if (hosts.length != numServers || ports.length != numServers) {
            throw new IllegalArgumentException(
                    "Devem ser fornecidos exatamente " + numServers + " host(s) e " + numServers + " port(s)");
        }

        this.channels = new ManagedChannel[numServers];
        this.stubs = new TupleSpacesReplicaStub[numServers];

        // sequencer
        sequencerChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
        sequencerStub = SequencerGrpc.newBlockingStub(sequencerChannel);

        for (int i = 0; i < numServers; i++) {
            channels[i] = ManagedChannelBuilder.forAddress(hosts[i], Integer.parseInt(ports[i])).usePlaintext().build();
            stubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
        }
    }

    public int getNumServers() {
        return numServers;
    }

    /*
     * This method allows the command processor to set the request delay assigned to
     * a given server
     */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);

        System.out.println("[Debug only]: After setting the delay, I'll test it");
        for (Integer i : delayer) {
            System.out.println("[Debug only]: Now I can send request to stub[" + i + "]");
        }
        System.out.println("[Debug only]: Done.");
    }

    public void shutdown() {
        for (ManagedChannel channel : channels) {
            channel.shutdown();
        }
        sequencerChannel.shutdown();
    }

    public TupleSpacesReplicaStub[] getStubs() {
        return stubs;
    }

    public TupleSpacesReplicaTotalOrder.PutResponse put(String tupleString) {

        // obter numero de sequencia
        SequencerOuterClass.GetSeqNumberResponse seqNumberResponse = sequencerStub
                .getSeqNumber(SequencerOuterClass.GetSeqNumberRequest.newBuilder().build());

        // enviar pedido ao servidor
        TupleSpacesReplicaTotalOrder.PutRequest request = TupleSpacesReplicaTotalOrder.PutRequest.newBuilder()
                .setNewTuple(tupleString)
                .setSeqNumber(seqNumberResponse.getSeqNumber())
                .build();

        ResponseCollector<TupleSpacesReplicaTotalOrder.PutResponse> c = new ResponseCollector<>();

        for (Integer id : delayer) {
            stubs[id].put(request, new ClientObserver<>(c));
        }

        try {
            c.waitUntilAllReceived(numServers);
            if (c.getNumResponsesFailed() > 0) {
                throw new RuntimeException("One or more put requests failed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return c.getFirstResponse();
    }

    public TupleSpacesReplicaTotalOrder.ReadResponse read(String searchPattern) {
        TupleSpacesReplicaTotalOrder.ReadRequest request = TupleSpacesReplicaTotalOrder.ReadRequest.newBuilder()
                .setSearchPattern(searchPattern)
                .build();

        ResponseCollector<TupleSpacesReplicaTotalOrder.ReadResponse> c = new ResponseCollector<>();

        for (Integer id : delayer) {
            stubs[id].read(request, new ClientObserver<>(c));
        }

        try {
            c.waitUntilAllReceived(1);
            if (c.getNumResponsesFailed() > 0) {
                throw new RuntimeException("One or more take requests failed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return c.getFirstResponse();
    }

    public List<TupleSpacesReplicaTotalOrder.TakeResponse> take(String searchPattern) {

        // obter o sequence number
        SequencerOuterClass.GetSeqNumberResponse seqNumberResponse = sequencerStub
                .getSeqNumber(SequencerOuterClass.GetSeqNumberRequest.newBuilder().build());

        // enviar pedido ao servidor
        TupleSpacesReplicaTotalOrder.TakeRequest request = TupleSpacesReplicaTotalOrder.TakeRequest.newBuilder()
                .setSearchPattern(searchPattern)
                .setSeqNumber(seqNumberResponse.getSeqNumber())
                .build();

        ResponseCollector<TupleSpacesReplicaTotalOrder.TakeResponse> c = new ResponseCollector<>();

        for (Integer id : delayer) {
            stubs[id].take(request, new ClientObserver<>(c));
        }

        try {
            c.waitUntilAllReceived(numServers);
            if (c.getNumResponsesFailed() > 0) {
                throw new RuntimeException("One or more take requests failed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return c.getResponses();
    }

    public TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse getTupleSpacesState(int qualifier) {
        TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest request = TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest
                .getDefaultInstance();

        ResponseCollector<TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse> c = new ResponseCollector<>();
        stubs[qualifier].getTupleSpacesState(request, new ClientObserver<>(c));

        try {
            c.waitUntilAllReceived(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return c.getFirstResponse();
    }

}
