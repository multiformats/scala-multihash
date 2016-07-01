package io.mediachain.multiaddr

import java.io.ByteArrayOutputStream
import java.net.UnknownHostException
import java.nio.ByteBuffer

import cats.data.Xor
import io.mediachain.multihash.MultiHash


case class MultiAddress private(bytes: Array[Byte]) {

  def asString: String =
    MultiAddress.encodeToString(bytes)
    .getOrElse(throw new IllegalStateException(
      "Unable to encode multiaddr to string"))

  override def toString: String = {
    MultiAddress.encodeToString(bytes)
      .map (str => s"[MultiAddress]: $str")
      .getOrElse("[MultiAddress]: Invalid")
  }

}

object MultiAddress {
  import java.net.InetAddress
  import io.mediachain.multiaddr.MultiAddressError._

  def protocolFor(bytes: Array[Byte]): Xor[MultiAddressError, Protocol] = {
    val buf = ByteBuffer.wrap(bytes)
    for {
      code <- IOError.catchIOException(Varint.readUnsignedInt(buf))
      result <- Xor.fromOption(Protocol.get(code), UnknownProtocolCode(code))
    } yield result
  }

  def fromBytes(bytes: Array[Byte]): Xor[MultiAddressError, MultiAddress] = {
    // encode to string to check validity
    encodeToString(bytes)
      .map(_ => MultiAddress(bytes))
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

    for {
      bytes <- bytesXor
      _ <- encodeToString(bytes)
    } yield {
      MultiAddress(bytes)
    }
  }

  private def encodeToString(raw: Array[Byte]):
  Xor[MultiAddressError, String] = {
    val buf = ByteBuffer.wrap(raw)
    val resXor = IOError.catchIOException {
      val code = Varint.readUnsignedInt(buf)
      for {
        proto <- Xor.fromOption(Protocol.get(code),
          UnknownProtocolCode(code))
        addrOpt <- readAddress(proto, buf)
        _ <- if (proto.size != 0 && addrOpt.isEmpty) {
          Xor.left(InvalidFormat("Failed to encode address"))
        } else {
          Xor.right({})
        }
      } yield {
        val out = new StringBuilder
        out.append(s"/${proto.name}")
        addrOpt.foreach { addr =>
          out.append(s"/$addr")
        }
        out.toString
      }
    }

    resXor.flatMap { res => res}
  }

  def readAddress(protocol: Protocol, addressBuffer: ByteBuffer):
  Xor[MultiAddressError, Option[String]] = {
    val size = protocol.sizeForAddress(addressBuffer)
    if (size == 0) {
      Xor.right(None)
    } else {
      protocol match {
        case Protocol.ip4 | Protocol.ip6 =>
          val buf = new Array[Byte](size)
          addressBuffer.get(buf)
          Xor.right(Some(InetAddress.getByAddress(buf).toString.substring(1)))

        case Protocol.tcp | Protocol.udp | Protocol.dccp | Protocol.sctp =>
          Xor.right(Some(Integer.toString(addressBuffer.getShort())))

        case Protocol.ipfs =>
          val buf = new Array[Byte](size)
          addressBuffer.get(buf)
          MultiHash.fromBytes(buf)
            .map(hash => Some(hash.base58))
            .leftMap(InvalidMultihash)

        case Protocol.onion =>
          val host = new Array[Byte](10)
          addressBuffer.get(host)
          val port = Integer.toString(addressBuffer.getShort)
          val str = s"${Base32.encode(host)}:$port"
          Xor.right(Some(str))

        case _ => Xor.left(UnimplementedProtocol(protocol))
      }
    }
  }

  def addressToBytes(protocol: Protocol, address: String):
  Xor[MultiAddressError, Array[Byte]] = {
    protocol match {
      case Protocol.ip4 | Protocol.ip6 =>
        Xor.catchOnly[UnknownHostException](
          InetAddress.getByName(address).getAddress
        ).leftMap(_ => UnknownHost(address))

      case Protocol.tcp | Protocol.udp | Protocol.dccp | Protocol.sctp =>
        Xor.catchOnly[NumberFormatException](Integer.parseInt(address))
          .leftMap(err => InvalidFormat(err.toString))
          .flatMap { x =>
            if (x > 65535) {
              Xor.left(StringDecodingError(
                s"Failed to parse ${protocol.name} address $address: " +
                  "value > 65535"))
            } else {
              Xor.right(Array((x >> 8).toByte, x.toByte))
            }
          }

      case Protocol.ipfs =>
        MultiHash.fromBase58(address)
          .map { hash =>
            val buf = collection.mutable.ArrayBuffer.empty[Byte]
            val hashBytes = hash.bytes
            Varint.writeUnsignedInt(hashBytes.length, buf)
            buf ++= hashBytes
            buf.toArray
          }.leftMap(InvalidMultihash)

      case Protocol.onion =>
        // TODO
        Xor.left(UnimplementedProtocol(protocol))

      case _ =>
        Xor.left(UnimplementedProtocol(protocol))
    }
  }
}
