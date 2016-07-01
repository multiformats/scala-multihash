package io.mediachain.multiaddr

import java.io.IOException
import java.nio.{BufferUnderflowException, BufferOverflowException}

import cats.data.Xor
import io.mediachain.multihash.MultiHashError

import scala.util.{Try, Success, Failure}

sealed trait MultiAddressError

object MultiAddressError {
  case class IOError(reason: Throwable) extends MultiAddressError
  case class StringDecodingError(reason: String) extends MultiAddressError
  case class InvalidFormat(reason: String) extends MultiAddressError
  case class UnknownProtocolCode(code: Int) extends MultiAddressError
  case class UnknownProtocolString(string: String) extends MultiAddressError
  case class UnknownHost(string: String) extends MultiAddressError
  case class InvalidMultihash(error: MultiHashError) extends MultiAddressError
  case class UnimplementedProtocol(protocol: Protocol) extends MultiAddressError

  object IOError {
    def catchIOException[T](f: => T): Xor[IOError, T] =
      Try(f) match {
        case Success(res) =>
          Xor.right(res)

        case Failure(err: Throwable) =>
          err match {
            case _: IOException | _: BufferUnderflowException | _: BufferOverflowException =>
              Xor.left(IOError(err))
            case _ =>
              throw err
          }
      }
  }
}


