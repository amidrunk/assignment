# Overview

This project is focused heavily on showcasing an architecture. It is not intended to implement full-featured authentication
and authorization and other non-functional features - what exists is only to demonstrate the architecture.

# Architecture

The architecture in this project is based on a saga pattern with event sourcing. Events are persisted in a message outbox
- in the same transaction as a business operation - and then published to a message broker via Debezium, which consumes
the database's change data capture (CDC) stream. This ensures consistency between the database state and the events published.

# Trade-offs

Time constraints have led to some trade-offs in the implementation:

* Websocket connections capture the host they are "pinned" to, allowing the websocket module to route messages to the
    correct host in a multi-instance deployment. However, this is not implemented in the current solution (but prepared).
* Authentication and authorization are only minimally implemented to demonstrate the architecture. In a production system,
    a JWT authorization scheme would have been appropriate, where the JWT also carries the user's organization id which subsequently
    can be used to resolve encryption keys for files and whatnot (to enforce complete isolation between organizations).