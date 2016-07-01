package io.mediachain.multiaddr


import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.nio.ByteBuffer

import cats.data.Xor
import io.mediachain.multihash.MultiHash

sealed trait MultiAddressCodec {
  def stringToBytes(string: String): Xor[MultiAddressError, Array[Byte]]
  def bytesToString(bytes: Array[Byte]): Xor[MultiAddressError, String]
}

object Codecs {
  import MultiAddressError._

  object Empty extends MultiAddressCodec {
    override def stringToBytes(string: String)
    : Xor[MultiAddressError, Array[Byte]] = Xor.right(Array.empty[Byte])

    override def bytesToString(bytes: Array[Byte])
    : Xor[MultiAddressError, String] = Xor.right("")
  }

  object IpAddress extends MultiAddressCodec {
    import java.net.{InetAddress, UnknownHostException}

    override def stringToBytes(string: String)
    : Xor[MultiAddressError, Array[Byte]] = {
      Xor.catchOnly[UnknownHostException] {
        InetAddress.getByName(string).getAddress
      }.leftMap(_ => UnknownHost(string))
    }


    override def bytesToString(bytes: Array[Byte])
    : Xor[MultiAddressError, String] = {
      Xor.catchOnly[UnknownHostException] {
        InetAddress.getByAddress(bytes).toString.substring(1)
      }.leftMap(_ => UnknownHost("Unable to decode host for ip address"))
    }
  }

  object Int16 extends MultiAddressCodec {
    override def stringToBytes(string: String)
    : Xor[MultiAddressError, Array[Byte]] =
      Xor.catchOnly[NumberFormatException](Integer.parseInt(string))
        .leftMap(err => InvalidFormat(err.toString))
        .flatMap { x =>
          if (x > 65535) {
            Xor.left(StringDecodingError(
              s"Failed to parse address $string: value > 65535"))
          } else {
            Xor.right(Array((x >> 8).toByte, x.toByte))
          }
        }

    override def bytesToString(bytes: Array[Byte])
    : Xor[MultiAddressError, String] = {
      val buf = ByteBuffer.wrap(bytes)
      IOError.catchIOException(
        Integer.toString(buf.getShort)
      )
    }
  }

  object Multihash extends MultiAddressCodec {
    override def stringToBytes(string: String)
    : Xor[MultiAddressError, Array[Byte]] =
      MultiHash.fromBase58(string)
        .map(_.bytes)
        .leftMap(InvalidMultihash)

    override def bytesToString(bytes: Array[Byte]): Xor[MultiAddressError, String] =
      MultiHash.fromBytes(bytes)
        .map(hash => hash.base58)
        .leftMap(InvalidMultihash)
  }

  object Onion extends MultiAddressCodec {
    override def stringToBytes(string: String)
    : Xor[MultiAddressError, Array[Byte]] = {
      val parts = string.split(':')
      val portMissing = StringDecodingError("Onion address must have port")
      for {
        hostString <- Xor.fromOption(parts.headOption, portMissing)
        portString <- Xor.fromOption(parts.lift(1), portMissing)
        _ <- if (hostString.length != 16) {
          Xor.left(StringDecodingError(
            "Invalid length for base32-encoded onion address: " +
            hostString.length))
        }  else {
          Xor.right({})
        }
        hostBytes <- Xor.catchNonFatal(Base32.decode(hostString.toUpperCase))
          .leftMap(_ => StringDecodingError("Unable to decode onion address"))
        port <- Xor.catchOnly[NumberFormatException](Integer.parseInt(portString))
          .leftMap(_ => StringDecodingError("Failed to decode onion port"))
        _ <- if (port < 1 || port > 65535) {
          Xor.left(StringDecodingError(
            s"Onion port must be between 1 and 65535, found: $port"))
        } else {
          Xor.right({})
        }
      } yield {
        val b = new ByteArrayOutputStream
        val out = new DataOutputStream(b)
        out.write(hostBytes)
        out.writeShort(port)
        out.flush()
        b.toByteArray
      }
    }

    override def bytesToString(bytes: Array[Byte])
    : Xor[MultiAddressError, String] =
      IOError.catchIOException {
        val buf = ByteBuffer.wrap(bytes)
        val host = new Array[Byte](10)
        buf.get(host)
        val port = Integer.toString(buf.getShort)
        s"${Base32.encode(host)}:$port"
      }

  }
}
