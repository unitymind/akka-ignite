package com.cleawing.ignite.akka.remote

import java.net.{ServerSocket, InetAddress}

import scala.util.Try

object Utils {
  type HostPort = (String, Int)

  def getRandomPortFor(host: String) : Try[HostPort] = {
    Try {
      var serverSocket : ServerSocket = null
      try {
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName(host))
        serverSocket.setReuseAddress(true)
        (host, serverSocket.getLocalPort)
      } catch {
        case t: Throwable => throw t
      }
      finally {
        if (serverSocket != null) serverSocket.close()
      }
    }
  }
}
