package io.mediachain.multiaddr

import java.io.IOException

import cats.data.Xor

sealed trait MultiAddressError
case class IOError(reason: IOException) extends MultiAddressError
case class StringDecodingError(reason: String) extends MultiAddressError
case object InvalidFormat extends MultiAddressError
case class UnknownProtocolCode(code: Int) extends MultiAddressError
case class UnknownProtocolString(string: String) extends MultiAddressError
case class UnimplementedProtocol(protocol: Protocol) extends MultiAddressError

object IOError {
  def catchIOException[T](f: => T): Xor[IOError, T] =
    Xor.catchOnly[IOException](f).leftMap(IOError(_))
}