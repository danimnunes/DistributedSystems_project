package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServer;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServerServiceGrpc;
import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(ServerMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length != 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port> <qualifier>");
            return;
        }

        // get the port and qualifier
        final int port = Integer.parseInt(args[0]);
        final String qualifier = args[1];

        // Create a managed channel to communicate with the Python server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 5001)
                .usePlaintext() // Use insecure communication (plaintext) for simplicity
                .build();

        // Create a blocking stub to make synchronous calls to the Python server
        NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);

        NameServer.ServerAddress address = NameServer.ServerAddress.newBuilder().setHost("localhost").setPort(port)
                .build();
        // Example of making a remote call to the Python server
        try {
            stub.register(NameServer.RegisterRequest.newBuilder()
                    .setServiceName("TupleSpaces")
                    .setQualifier(qualifier)
                    .setAddress(address).build());
        } catch (Exception e) {
            channel.shutdown();
            System.err.println("Error unregistering server from naming server: " + e.getMessage());
        }

        final BindableService impl = new TupleSpaceServiceImpl();

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(impl).build();

        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Add a shutdown hook to catch termination signals (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down server");
                stub.delete(NameServer.DeleteServerRequest.newBuilder().setServiceName("TupleSpaces")
                        .setAddress(address).build());
                server.shutdown();
                channel.shutdown();
            } catch (Exception e) {
                System.err.println("Error unregistering server from naming server: " + e.getMessage());
            }
        }));

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();

    }
}
