package io.mediachain.multiaddr

import java.nio.ByteBuffer

import cats.data.Xor
import io.mediachain.multihash.MultiHash


case class MultiAddress private(bytes: Array[Byte]) {

}

object MultiAddress {
  def protocolFor(bytes: Array[Byte]): Xor[MultiAddressError, Protocol] = {
    val buf = ByteBuffer.wrap(bytes)
    for {
      code <- IOError.catchIOException(Varint.readUnsignedInt(buf))
      result <- Xor.fromOption(Protocol.get(code), UnknownProtocol(code))
    } yield result
  }

  def fromBytes(bytes: Array[Byte]): Xor[MultiAddressError, MultiAddress] = {
    for {
      protocol <- protocolFor(bytes)

    }

    ???
  }

  def fromString(string: String): Xor[MultiAddressError, MultiAddress] = {
    val trimmed = string.stripSuffix("/")
    val parts = trimmed.split('/')
    for {

    }
  }


  def readAddress(protocol: Protocol, addressBytes: Array[Byte]):
  Xor[MultiAddressError, String] = {
    import java.net.InetAddress


    val readBuffer = ByteBuffer.wrap(addressBytes)
    val size = protocol.sizeForAddress(readBuffer)
    protocol match {
      case Protocol.ip4 | Protocol.ip6 =>
        val buf = new Array[Byte](size)
        readBuffer.get(buf)
        Xor.right(InetAddress.getByAddress(buf).toString.substring(1))

      case Protocol.tcp | Protocol.udp | Protocol.dccp | Protocol.sctp =>
        Xor.right(Integer.toString(readBuffer.getShort()))

      case Protocol.ipfs =>
        val buf = new Array[Byte](size)
        readBuffer.get(buf)
        MultiHash.fromBytes(buf).map(_.base58).leftMap(_ => InvalidFormat)

      case Protocol.onion =>
        val host = new Array[Byte](10)
        readBuffer.get(host)
        val port = Integer.toString(readBuffer.getShort)
        val str = s"${Base32.encode(host)}:$port"
        Xor.right(str)

      case _ => Xor.left(UnimplementedProtocol(protocol))
    }
  }
}
