# Overview

This project is focused heavily on showcasing an interesting architecture. There are numerous trade-offs that are of less
relevance to the architecture itself, These trade-offs are documented below.

It is not intended to implement full-featured application with hardened authentication and authorization or other 
non-functional features - what exists is only to demonstrate the architecture.

# Architecture

The architecture in this project is based on event sourcing. Events are persisted in a message outbox
- in the same transaction as a business operation - and then published to a message broker via Debezium, which consumes
the database's change data capture (CDC) stream. This ensures consistency between the database state and the events published.

Since this is a demo project, it does not implement micro-services but rather independent modules. Modules communicate
over gRPC to decouple and illustrate the independence of modules.

# Frontend

The frontend is implemented mostly for fun and is rather heavy on codex. The usual process to work with codex is to 
iterate the code to ensure quality and correctness. However, with the time constraints of this project, codex was given
a more free reign to implement larger parts of the frontend. This means that the quality and correctness of the
frontend code is not as high as it could be with more manual intervention.

# Trade-offs

Time constraints have led to some trade-offs in the implementation:

* Websocket connections capture the host they are "pinned" to, allowing the websocket module to route messages to the
    correct host in a multi-instance deployment. However, this is not implemented in the current solution (but prepared).
* Authentication and authorization are only minimally implemented to demonstrate the architecture. In a production system,
    a JWT authorization scheme would have been appropriate, where the JWT also carries the user's organization id which subsequently
    can be used to resolve encryption keys for files and whatnot (to enforce complete isolation between organizations).
* Files are loosly associated with canvases through file attributes. This is obviously not feasible for a production application,
    but it is sufficient to demonstrate the architecture.