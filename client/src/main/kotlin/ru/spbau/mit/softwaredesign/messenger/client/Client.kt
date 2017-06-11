package ru.spbau.mit.softwaredesign.messenger.client

import io.grpc.stub.StreamObserver
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging
import ru.spbau.mit.messenger.protocol.ChatGrpc
import ru.spbau.mit.messenger.protocol.Message
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ChatClient(
        private val username: String,
        private val host: String,
        private val port: Int
) {
    private val logger = KotlinLogging.logger("Client")
    private var connection: StreamObserver<Message>? = null

    fun start() {
        val channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext(true)
                .build()
        val service = ChatGrpc.newStub(channel)
        this.connection = service.routeChat(object : StreamObserver<Message> {
            override fun onNext(message: Message) {
                logger.info("${message.timestamp} ${message.username}: ${message.content}")
            }

            override fun onError(t: Throwable) {
                logger.error("Error: ${t.message}")
                System.exit(1)
            }

            override fun onCompleted() {
                logger.info("Completed")
                System.exit(0)
            }
        })

        logger.info { "Client started" }
        Scanner(System.`in`).let {
            while (true) {
                val message = it.nextLine()
                send(message)
                logger.info("$timestamp $username: $message")
            }
        }
    }

    private fun send(message: String) = connection?.onNext(
            Message.newBuilder()
                    .setUsername(username)
                    .setTimestamp(timestamp)
                    .setContent(message)
                    .build()
    )

    companion object {
        val FORMAT_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

        val timestamp: String
            get() = FORMAT_TIME.format(LocalDateTime.now())
    }
}

fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: client <username> <host> <port>")
        return
    }

    val client = ChatClient(args[0], args[1], Integer.parseInt(args[2]))
    client.start()
}
