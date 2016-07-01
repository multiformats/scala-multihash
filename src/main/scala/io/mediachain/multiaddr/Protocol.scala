package io.mediachain.multiaddr
import java.nio.ByteBuffer

import cats.data.Xor
import io.mediachain.multiaddr.MultiAddressError.IOError

import scala.collection.mutable

sealed abstract class Protocol(val name: String, val code: Int, val size: Int, val codec: MultiAddressCodec) {

  val codeBytes: Array[Byte] = {
    val buffer = new mutable.ArrayBuffer[Byte]()
    Varint.writeUnsignedInt(code, buffer)
    buffer.toArray
  }

  def hasAddress: Boolean = size != 0

  private def unprefixedAddressBytes(addressBytes: Array[Byte]):
  Xor[MultiAddressError, Array[Byte]] = {
    if (size == Protocol.LENGTH_PREFIXED_VARIABLE_SIZE) {
      IOError.catchIOException {
        val readBuf = ByteBuffer.wrap(addressBytes)
        val len = Varint.readUnsignedInt(readBuf)
        val unprefixed = new Array[Byte](len)
        readBuf.get(unprefixed)
        unprefixed
      }
    } else {
      Xor.right(addressBytes)
    }
  }

  def addressString(addressBytes: Array[Byte]): Xor[MultiAddressError, String] =
    unprefixedAddressBytes(addressBytes)
      .flatMap(codec.bytesToString)

  def addressBytes(addressString: String): Xor[MultiAddressError, Array[Byte]] =
  {
    codec.stringToBytes(addressString)
      .map { bytes =>
        if (size == Protocol.LENGTH_PREFIXED_VARIABLE_SIZE) {
          val buf = collection.mutable.ArrayBuffer.empty[Byte]
          Varint.writeUnsignedInt(bytes.length, buf)
          buf ++= bytes
          buf.toArray
        } else {
          bytes
        }
      }
  }

}

object Protocol {
  val LENGTH_PREFIXED_VARIABLE_SIZE = -1

  case object ip4 extends Protocol("ip4", 4, 32, Codecs.IpAddress)
  case object tcp extends Protocol("tcp", 6, 16, Codecs.Int16)
  case object udp extends Protocol("udp", 17, 16, Codecs.Int16)
  case object dccp extends Protocol("dccp", 33, 16, Codecs.Int16)
  case object ip6 extends Protocol("ip6", 41, 128, Codecs.IpAddress)
  case object sctp extends Protocol("sctp", 132, 16, Codecs.Int16)
  case object utp extends Protocol("utp", 301, 0, Codecs.Empty)
  case object udt extends Protocol("udt", 302, 0, Codecs.Empty)
  case object ipfs extends Protocol("ipfs", 421, LENGTH_PREFIXED_VARIABLE_SIZE, Codecs.Multihash)
  case object https extends Protocol("https", 443, 0, Codecs.Empty)
  case object http extends Protocol("http", 480, 0, Codecs.Empty)
  case object onion extends Protocol("onion", 444, 80, Codecs.Onion)

  private val byName: Map[String, Protocol] = Map(
    ip4.name -> ip4,
    tcp.name -> tcp,
    udp.name -> udp,
    dccp.name -> dccp,
    ip6.name -> ip6,
    sctp.name -> sctp,
    utp.name -> utp,
    udt.name -> udt,
    ipfs.name -> ipfs,
    https.name -> https,
    http.name -> http,
    onion.name -> onion
  )

  private val byCode: Map[Int, Protocol] = Map(
    ip4.code -> ip4,
    tcp.code -> tcp,
    udp.code -> udp,
    dccp.code -> dccp,
    ip6.code -> ip6,
    sctp.code -> sctp,
    utp.code -> utp,
    udt.code -> udt,
    ipfs.code -> ipfs,
    https.code -> https,
    http.code -> http,
    onion.code -> onion
  )

  def get(name: String): Option[Protocol] = byName.get(name)
  def get(code: Int): Option[Protocol] = byCode.get(code)

}
