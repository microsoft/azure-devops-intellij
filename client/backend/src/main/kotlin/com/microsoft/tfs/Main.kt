package com.microsoft.tfs

import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials
import java.nio.file.Paths

fun main(args: Array<String>) {
    val login = args[0]
    val password = args[1]
    val path = Paths.get(System.getProperty("user.dir"))

    val client = TfsClient(path, UsernamePasswordCredentials(login, password))
    val pending = client.status(path)
    pending.forEach {
        println(it.pendingChanges.size)
        println(it.candidatePendingChanges.size)
        for (candidatePendingChange in it.candidatePendingChanges) {
            println(candidatePendingChange)
        }
    }
}