package io.mediachain.multiaddr

import java.io.{ByteArrayOutputStream, IOException}
import java.nio.ByteBuffer

import cats.data.Xor
import io.mediachain.multihash.MultiHash


case class MultiAddress private(bytes: Array[Byte]) {

}

object MultiAddress {
  import java.net.InetAddress

  def protocolFor(bytes: Array[Byte]): Xor[MultiAddressError, Protocol] = {
    val buf = ByteBuffer.wrap(bytes)
    for {
      code <- IOError.catchIOException(Varint.readUnsignedInt(buf))
      result <- Xor.fromOption(Protocol.get(code), UnknownProtocolCode(code))
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
    val parts = trimmed.split('/').toList

    val bytesXor: Xor[MultiAddressError, Array[Byte]] = for {
      _ <- Xor.fromOption(parts.headOption,
        StringDecodingError("MultiAddress must start with /"))
      protoString <- Xor.fromOption(parts.lift(1),
        StringDecodingError("MultiAddress is missing protocol specifier"))

      proto <- Xor.fromOption(Protocol.get(protoString),
        UnknownProtocolString(protoString))

      addressOpt = parts.lift(2)
      _ <- if (proto.size != 0 && addressOpt.isEmpty) {
        Xor.left(StringDecodingError(
          "Protocol requires address, but none provided"))
      } else {
        Xor.right({})
      }

    } yield {
      val bout = new ByteArrayOutputStream
      bout.write(proto.codeBytes)
      addressOpt.foreach { address =>
        val addressBytes = addressToBytes(proto, address)
        // TODO: catch errors in addressToBytes
        addressBytes.foreach(bout.write)
      }
      bout.toByteArray
    }

    bytesXor.map(MultiAddress.apply)
  }


  def readAddress(protocol: Protocol, addressBytes: Array[Byte]):
  Xor[MultiAddressError, Option[String]] = {
    val readBuffer = ByteBuffer.wrap(addressBytes)
    val size = protocol.sizeForAddress(readBuffer)
    if (size == 0) {
      Xor.right(None)
    } else {
      protocol match {
        case ip4 | ip6 =>
          val buf = new Array[Byte](size)
          readBuffer.get(buf)
          Xor.right(Some(InetAddress.getByAddress(buf).toString.substring(1)))

        case tcp | udp | dccp | sctp =>
          Xor.right(Some(Integer.toString(readBuffer.getShort())))

        case ipfs =>
          val buf = new Array[Byte](size)
          readBuffer.get(buf)
          MultiHash.fromBytes(buf)
            .map(hash => Some(hash.base58))
            .leftMap(_ => InvalidFormat)

        case onion =>
          val host = new Array[Byte](10)
          readBuffer.get(host)
          val port = Integer.toString(readBuffer.getShort)
          val str = s"${Base32.encode(host)}:$port"
          Xor.right(Some(str))

        case _ => Xor.left(UnimplementedProtocol(protocol))
      }
    }
  }

  def addressToBytes(protocol: Protocol, address: String):
  Xor[MultiAddressError, Array[Byte]] = {
    protocol match {
      case ip4 | ip6 =>
        Xor.right(InetAddress.getByName(address).getAddress)

      case tcp | udp | dccp | sctp =>
        val x = Integer.parseInt(address)
        if (x > 65535) {
          Xor.left(StringDecodingError(
            s"Failed to parse ${protocol.name} address $address: " +
            "value > 65535"))
        } else {
          Xor.right(Array((x >> 8).toByte, x.toByte))
        }

      case ipfs =>
        MultiHash.fromBase58(address)
          .map { hash =>
            val buf = collection.mutable.ArrayBuffer.empty[Byte]
            val hashBytes = hash.bytes
            Varint.writeUnsignedInt(hashBytes.length, buf)
            buf ++= hashBytes
            buf.toArray
          }.leftMap(_ => InvalidFormat)

      case onion =>
        // TODO
        Xor.left(UnimplementedProtocol(protocol))

      case _ =>
        Xor.left(UnimplementedProtocol(protocol))
    }
  }
}
