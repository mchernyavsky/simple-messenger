package ru.spbau.mit.softwaredesign.messenger.server

import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import mu.KotlinLogging
import ru.spbau.mit.messenger.protocol.Message
import ru.spbau.mit.messenger.protocol.ChatGrpc
import java.util.LinkedHashSet

class ChatServer : ChatGrpc.ChatImplBase() {
    val logger = KotlinLogging.logger("Server")

    override fun routeChat(responseObserver: StreamObserver<Message>): StreamObserver<Message> {
        observers.add(responseObserver)
        return object : StreamObserver<Message> {
            override fun onNext(message: Message) {
                logger.info("${message.timestamp} ${message.username}: ${message.content}")
                observers.stream().forEach { it.onNext(message) }
            }

            override fun onError(t: Throwable) {
                logger.error("Server error: ${t.message}")
                observers.remove(responseObserver)
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
                observers.remove(responseObserver)
            }
        }
    }

    companion object {
        private val observers = LinkedHashSet<StreamObserver<Message>>()
    }
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: server <port>")
        return
    }

    val port = Integer.parseInt(args[0])
    val service = ChatServer()
    val server = ServerBuilder
            .forPort(port)
            .addService(service)
            .build()
    server.start()
    service.logger.info { "Server started" }
    server.awaitTermination()
}
