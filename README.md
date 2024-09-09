# TupleSpaces

Distributed Systems Project 2024
**Group A48**

**Difficulty level: I am Death incarnate!**


### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members


| Number | Name              | User                             | Email                                       |
|--------|-------------------|----------------------------------|---------------------------------------------|
| 102078 |  João Costa       | <https://github.com/joaolscosta> | <mailto:joaolscosta@tecnico.ulisboa.pt>     |
| 102975 | Rafael Ribeiro    | <https://github.com/RafaR13>     | <mailto:rafael.m.ribeiro@tecnico.ulisboa.pt>|
| 103095 | Daniel Nunes      | <https://github.com/danimnunes>  | <mailto:daniel.m.nunes@tecnico.ulisboa.pt>  |

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

### Execution

```
source .venv/bin/activate
```
Inside Contract:
```
mvn exec:exec
```

Inside NameServer:
```
python3 server.py
```

Inside ServerR1 (example of a set of arguments):
```
mvn compile exec:java -Dexec.args="2001 A"
```

Inside Client:
```
mvn compile exec:java
```

Inside Sequencer:
```
mvn compile exec:java
```



## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.






## Phase 2.1 Implementation:

In this phase of development, we made changes to both the client and server to incorporate the use of requests and responses defined in the Xu Liskov replica file. Additionally, we implemented a mechanism called the "Response Collector" to store the responses received from the server in a list. We also created a `ClientObserver` that extends the `StreamObserver` class, allowing us to add the responses received from the server to the Response Collector.

In the specific implementation of the read and put operations in the client, we used the `waitUntilAllReceived` function to await the response from all servers in the case of the read operation and from only one server in the case of the put operation.


## Phase 2.2 Implementation:

In phase 2.2, alongside switching requests and responses to those from the Xu Liskov replica, we divided the implementation of the take operation into two phases, mirroring the algorithm.

**Client:**
- Phase 1: The client sends a request as described in the algorithm and receives responses from all servers. If all servers accept and the intersection of tuples satisfying the expression given by the client is non-empty, then it proceeds to phase 2. Otherwise, release requests are sent, and this procedure is repeated until the conditions above are met.
- Phase 2: The client sends the randomly chosen tuple from those selected in phase 1 and waits for responses from all servers.

**Server:**
- Phase 1: Each server receives the search pattern and searches its tuple space for tuples that satisfy it. If found and these tuples are not already blocked by another client, they are added to the server's response. If there are no tuples satisfying the search pattern, the server waits.
- Phase 2: The server receives the tuple to be taken, and it is removed from the tuple space.

To implement these locks, in addition to a list of tuples, we implemented a list of client IDs and a list of lock flags (with the same length as the tuple list). In the server's put function, entries are added to these three lists. In phase 1 of take, the values of the flag list and client IDs are altered to signal that the tuple is blocked, and in phase 2 of take, entries are removed from the lists.



## Phase 3.1 Implementation:

**Client:**
- The types of requests and responses were changed to use the Total Order replica. To send requests to the servers, we used the sequencer to obtain a sequence number that is part of the request.

**Server:**
- In the server part, we created a class Request, whose attributes are the sequence number, the tuple to be removed, and a boolean canTake, which indicates whether it is possible to remove that tuple from the tuple space. We also created a list of Requests, which stores the takeRequests waiting, and a nextRequest counter, which indicates the sequence number of the next request to be executed.

Both in the put and take operations, each thread waits for nextRequest to have the value of its Sequence Number before obtaining mutual exclusion and executing the request.

- Put:
  Once mutual exclusion is obtained, the tuple is inserted into the tuple space, and it is checked if there is any take waiting to remove that tuple. If none, then nextRequest is incremented, a notifyAll is performed, and the thread executing this request terminates. If there is any take waiting, the canTake attribute of the request with the oldest sequence number, among the requests whose tuple matches the inserted tuple, is set to true. In this case, nextRequest is not incremented, but notifyAll is still performed; thus, all requests wake up, but only the chosen take can obtain mutual exclusion, perform the take, and subsequently increment the nextRequest counter.

- Take:
  When nextRequest has the value of the Sequence Number of the request in this thread of execution, mutual exclusion is obtained, and there is an attempt to perform a take operation in the tuple space. In the case of success, nextRequest is incremented, notifyAll is performed, and the next request is executed. If it is not possible to remove the tuple, a Request with the sequence Number and the tuple of this request is created, and it is added to the list of takeRequests. Then, nextRequest is incremented.
