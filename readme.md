# Overview

This project is focused heavily on showcasing an interesting architecture. There are numerous trade-offs that are of less
relevance to the architecture itself, These trade-offs are documented below.

It is not intended to implement full-featured application with hardened authentication and authorization or other 
non-functional features - what exists is only to demonstrate the architecture.

## Running 

```shell
docker compose up --build
```

Navigate to http://localhost:3000.

# Functionality

* Login using admin / changeme. There's only one user right now to concentrate on the architecture.
* You can create canvases and they'll be listed when you login.
* You can open a canvas and see the file list.
* You can upload files to the canvas.
* You will get notifications when files are uploaded to the canvas you have open (the frontend subscribes to the canvas it opens).

# Architecture

The architecture in this project is based on event sourcing. Events are persisted in a message outbox
(in the same transaction as a business operation) and then published to Kafka via Debezium, which consumes
the database's change data capture (CDC) stream. This ensures consistency between the database state and the events published.

Since this is a demo project, it does not implement micro-services but rather independent modules. Modules communicate
over gRPC to decouple and illustrate the independence of modules. It's a microcosm of a micro-service architecture of sorts.

## Generic, bounded functions

The functions that are implemented are generic with an eye towards bounded contexts, APIs and events. The functions
focus on a specific domain and do not mix concerns. This allows them to be reused and extended through the event
mechanism and the APIs.

## Files

Files are stored using a FileStorage - currently on file only. But the FileStorage can be changed for S3 or some other
file storage mechanism. The backend also stores a file description with the file. This contains eg the file name, type
and attributes, but could also contain encryption metadata or other relevant information.

When a file is being uploaded, a file descriptor will be created in `PENDING`, showing that it is about to be uploaded.
The id of the file descriptor is used to identify the file contents, but this could be more sophisticated in a more complete
implementation. The file contents are then uploaded to the FileStorage. When the upload is complete,
the file descriptor status is changed to `UPLOADED`. This allows peer services to track the upload progress of files.

## WebSockets

WebSockets are implemented as a generic module that serves only the purpose of accepting a websocket, maintaining the
websocket lifecycle, broadcasting received messages and exposing an API to send messages to connected clients. A trade-off
right now is that this can't be scaled - the websocket connections are pinned to the host they are created on and currently
a node receiving a request to send a message to a client will not be able to route the message to the correct host. This limitation
is due to the time constraints of the project, but the architecture is prepared for this.

When a user connects to a websocket, the backend will announce a `WebSocketConnectionChangedEvent`. The websocket metadata
is also saved in the database for routing purposes. When a user disconnects, another `WebSocketConnectionChangedEvent` is
published for the same Kafka key, showing that the websocket connection is deleted (disconnected). 

If the use sends a message of the websocket, the websocket module will not itself have any logic to handle the message.
It will instead broadcast the message as a `WebSocketMessageReceivedEvent` to allow other modules to handle the message
appropriately. They currently need to filter out messages not intended for them, but in a more complete implementation
there could be a routing mechanism to route messages to the correct module or other targeting criteria.

A module that picks up a websocket message can respond by sending a message to the client using the gRPC API exposed
by the websocket module. 

## Subscriptions

When the frontend opens a canvas, it subscribes to notifications for that canvas by sending a websocket message to the backend.
The notification module picks up the message as a `WebSocketMessageReceivedEvent` and unpacks a subscription request.
This will be stored in the database.

When a file is subsequently uploaded to the canvas, a `FileDescriptorChangedEvent` is published. This is currently tagged
with the canvasId - an overly simplistic solution that works for this demo project. The notification module picks up the event
and checks which users are subscribed to notifications for that canvas. It will then send a notification message to each subscribed user
using the websocket module's gRPC API.

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