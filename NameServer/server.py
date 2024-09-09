import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

from concurrent import futures
import grpc
from threading import Thread
from typing import Dict, List
from NameServer_pb2_grpc import add_NameServerServiceServicer_to_server, NameServerServiceServicer
import NameServer_pb2 as pb2
from NameServer_pb2 import ServerInfo

# define the port
PORT = 5001

class ServerEntry:
    def __init__(self, host: str, port: int, qualifier: str):
        self.host = host
        self.port = port
        self.qualifier = qualifier

class ServiceEntry:
    def __init__(self, service_name: str):
        self.service_name = service_name
        self.servers: Dict[str, ServerEntry] = {}

    def add_server_entry(self, server_address: ServerEntry, qualifier: str):
        self.servers[qualifier] = server_address

class NameServer:
    def __init__(self):
        self.services: Dict[str, ServiceEntry] = {}

    def register(self, service_name: str, server_address: ServerEntry, qualifier: str):

        if service_name not in self.services:
            self.services[service_name] = ServiceEntry(service_name)
        service_entry = self.services[service_name]

        if qualifier in service_entry.servers:
            raise ValueError("Not possible to register the server")

        service_entry.add_server_entry(server_address, qualifier)

        return pb2.RegisterResponse()

    def lookup(self, service_name: str, qualifier: str = None) -> List[ServerInfo]:
        if service_name not in self.services:
            return []  # Service doesn't exist, return empty list
        service_entry = self.services[service_name]
        server_infos = []

        if qualifier is None:
            # Return all servers for the service
            for qualifier, server_entry in service_entry.servers.items():
                server_info = ServerInfo()
                server_info.address = server_entry.address
                server_info.qualifier = qualifier
                server_infos.append(server_info)
        else:
            # Return server associated with the qualifier
            if qualifier in service_entry.servers:
                server_entry = service_entry.servers[qualifier]
                server_info = pb2.ServerInfo()
                server_address = pb2.ServerAddress(host=server_entry.host, port=server_entry.port)
                server_info.address.CopyFrom(server_address)
                server_info.qualifier = qualifier
                server_infos.append(server_info)
        return server_infos

    def delete(self, service_name: str, server_address: str) -> str:
        if service_name not in self.services:
            raise ValueError("Not possible to remove the server")


        service_entry = self.services[service_name]

        for qualifier, server_entry in service_entry.servers.items():

            if server_entry.host == server_address.host and server_entry.port == server_address.port:
                del service_entry.servers[qualifier]
                return ""

        raise ValueError("Not possible to remove the server")



class NameServerServiceImpl(NameServerServiceServicer):
    def __init__(self, name_server):
        self.name_server = name_server

    def register(self, request, context):
        try:
            servicename = request.serviceName
            qualifier = request.qualifier
            address = request.address

            response = self.name_server.register(servicename, address, qualifier)
            return response
        except ValueError as e:
            context.set_details(str(e))
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            raise ValueError("Registration failed: " + str(e))

    def lookup(self, request, context):
        try:
            servicename = request.serviceName
            qualifier = request.qualifier
            response = self.name_server.lookup(servicename, qualifier)

            return pb2.LookupServerResponse(serverInfo=response)
        except Exception as e:
            context.set_details(str(e))
            context.set_code(grpc.StatusCode.INTERNAL)
            raise RuntimeError("Lookup failed: " + str(e))

    def delete(self, request, context):
        try:
            service_name = request.serviceName
            server_address = request.address

            self.name_server.delete(service_name, server_address)

            return pb2.DeleteServerResponse()
        except ValueError as e:
            context.set_details(str(e))
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            raise ValueError("Deletion failed: " + str(e))

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    name_server = NameServer()
    add_NameServerServiceServicer_to_server(NameServerServiceImpl(name_server), server)
    server.add_insecure_port('[::]:' + str(PORT))
    server.start()
    server.wait_for_termination()




if __name__ == '__main__':
    try:
        print("NameServer started")

        # print received arguments
        print("Received arguments:")
        for i in range(1, len(sys.argv)):
            print("  " + sys.argv[i])


        serve()

    except KeyboardInterrupt:
        print("HelloServer stopped")
        exit(0)
