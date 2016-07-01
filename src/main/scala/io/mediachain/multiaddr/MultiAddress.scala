package io.mediachain.multiaddr

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import cats.data.Xor


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
  import io.mediachain.multiaddr.MultiAddressError._

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
      _ <- if (proto.hasAddress && addressOpt.isEmpty) {
        Xor.left(StringDecodingError(
          "Protocol requires address, but none provided"))
      } else {
        Xor.right({})
      }

    } yield {
      val bout = new ByteArrayOutputStream
      bout.write(proto.codeBytes)
      addressOpt.foreach { address =>
        val addressBytes = proto.addressBytes(address)
        // TODO: catch errors in addressBytes
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
      val addrBytes = new Array[Byte](buf.remaining)
      buf.get(addrBytes)
      for {
        proto <- Xor.fromOption(Protocol.get(code),
          UnknownProtocolCode(code))
        addr <- proto.addressString(addrBytes)
      } yield {
        val out = new StringBuilder
        out.append(s"/${proto.name}")
        if (proto.hasAddress) {
          out.append(s"/$addr")
        }
        out.toString
      }
    }

    resXor.flatMap { res => res}
  }
}
