package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientServiceNameServer;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.NameServer;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.List;

public class ClientMain {
    public static void main(String[] args) {

        String host = "localhost";
        int port = 5001;

        ClientServiceNameServer clientServiceNameServer = new ClientServiceNameServer(host, port);

        List<String> serverHosts = new ArrayList<>();
        List<String> serverPorts = new ArrayList<>();

        try {
            for (char identifier = 'A'; identifier <= 'C'; identifier++) {
                NameServer.LookupServerResponse response = clientServiceNameServer.lookup("TupleSpaces",
                        String.valueOf(identifier));

                // Check if the response contains server info
                if (!response.getServerInfoList().isEmpty()) {
                    NameServer.ServerInfo serverInfo = response.getServerInfo(0); // Assuming there's only one server
                    serverHosts.add(serverInfo.getAddress().getHost());
                    serverPorts.add(String.valueOf(serverInfo.getAddress().getPort()));
                } else {
                    System.err.println(
                            "No server info available for identifier " + identifier + " from the naming server.");
                }
            }
        } catch (StatusRuntimeException e) {
            // Handle gRPC status exceptions
            System.err.println("Error communicating with naming server: " + e.getStatus().getDescription());
            // Shutdown the client service for the naming server
            clientServiceNameServer.shutdown();
            return;
        }

        // Proceed with command processing
        CommandProcessor parser = new CommandProcessor(
                new ClientService(serverHosts.toArray(new String[0]), serverPorts.toArray(new String[0])));
        parser.parseInput();

        // Shutdown the client service for the naming server
        clientServiceNameServer.shutdown();
    }
}
