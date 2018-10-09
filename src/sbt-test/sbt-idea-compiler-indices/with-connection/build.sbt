import java.io.{DataInputStream, DataOutputStream}
import java.net.ServerSocket

import complete.DefaultParsers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

InputKey[Unit]("startConnection") := {
  val port = (Space ~> NatBasic).parsed
  val server = new ServerSocket(port)

  Future {
    val client = server.accept()
    val in     = new DataInputStream(client.getInputStream())
    val (_, _) = (in.readUTF(), in.readUTF())
    CompilationState.result += 0
    val out = new DataOutputStream(client.getOutputStream())
    Thread.sleep(500)
    out.writeUTF("ack")
    val (_, _) = (in.readBoolean(), in.readUTF())
    out.writeUTF("ack")
    CompilationState.result += 3
  }
  Thread.sleep(500)
}

Compile / manipulateBytecode := {
  val old = (Compile / manipulateBytecode).value
  CompilationState.result += 2
  Thread.sleep(500)
  old
}

lazy val updateState = TaskKey[Unit]("updateState")

updateState                  := { CompilationState.result += 1 }
Compile / manipulateBytecode := (Compile / manipulateBytecode).dependsOn(updateState).value

TaskKey[Unit]("checkResults") := {
  val result = CompilationState.result.toList
  if (result != List(0, 1, 2, 3)) sys.error("Wrong results order.")
}