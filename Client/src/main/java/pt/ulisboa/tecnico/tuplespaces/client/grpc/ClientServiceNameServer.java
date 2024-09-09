package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServerServiceGrpc.NameServerServiceBlockingStub;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServer;
import io.grpc.StatusRuntimeException;


public class ClientServiceNameServer {

    private final ManagedChannel channel;


    private final NameServerServiceBlockingStub blockingStub;

    public ClientServiceNameServer(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = NameServerServiceGrpc.newBlockingStub(channel);
    }

    public NameServerServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public void shutdown() {
        channel.shutdown();
    }

    public NameServer.LookupServerResponse lookup(String servicename, String qualifier) throws StatusRuntimeException {
        NameServer.LookupServerRequest lookupRequest = NameServer.LookupServerRequest.newBuilder()
                .setServiceName(servicename)
                .setQualifier(qualifier)
                .build();

        try {
            return blockingStub.lookup(lookupRequest);
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }
}
