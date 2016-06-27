package io.mediachain.multiaddr
import java.nio.ByteBuffer

import scala.collection.mutable

sealed abstract class Protocol(val name: String, val code: Int, val size: Int) {

  val codeBytes: Array[Byte] = {
    val buffer = new mutable.ArrayBuffer[Byte]()
    Varint.writeUnsignedInt(code, buffer)
    buffer.toArray
  }

  def sizeForAddress(addressBytes: ByteBuffer): Int = {
    if (size > 0) {
      size / 8
    } else if (size == 0) {
      size
    } else {
      Varint.readUnsignedInt(addressBytes)
    }
  }


}

object Protocol {
  val LENGTH_PREFIXED_VARIABLE_SIZE = -1

  case object ip4 extends Protocol("ip4", 4, 32)
  case object tcp extends Protocol("tcp", 6, 16)
  case object udp extends Protocol("udp", 17, 16)
  case object dccp extends Protocol("dccp", 33, 16)
  case object ip6 extends Protocol("ip6", 41, 128)
  case object sctp extends Protocol("sctp", 132, 16)
  case object utp extends Protocol("utp", 301, 0)
  case object udt extends Protocol("udt", 302, 0)
  case object ipfs extends Protocol("ipfs", 421, LENGTH_PREFIXED_VARIABLE_SIZE)
  case object https extends Protocol("https", 443, 0)
  case object http extends Protocol("http", 480, 0)
  case object onion extends Protocol("onion", 444, 80)

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
