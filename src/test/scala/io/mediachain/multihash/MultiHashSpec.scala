package io.mediachain.multihash


import io.mediachain.BaseSpec


object MultiHashSpec extends BaseSpec {
  def is =
  s2"""
    Calculates sha256-based multihash $sha256
    Is POJO serializable: $serializableAsPojo
  """

  val str = "3A344E92C8F2EE8F54B8734F90328A946B0A4273DF8DEBA96A33D9C1EADFEFF82E7451663AF284DA025E2A12EFBAE0971D076C2D5AC30BD6A657601BD1FBC7AE"
  val bytes = str.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

  val expectedHashBytes: Array[Byte] =
    Array(18, 32, 72, -12, 3, 45, -103, -95, 83, -41, -45, -57, 45, 24, 31,
      48, -9, 61, 107, 78, -117, 108, 109, 109, -119, 35, 2, 98, 113, 66, 27, -50, -100, 116)

  def sha256 = {
    val multiHash = MultiHash.hashWithSHA256(bytes)

    multiHash.hashType must beEqualTo(MultiHash.sha256)
    multiHash.bytes must beEqualTo(expectedHashBytes)
  }

  def serializableAsPojo = {
    import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
    import scala.util.Try


    val multiHash = MultiHash.hashWithSHA256(bytes)
    val byteStream = new ByteArrayOutputStream
    val out = new ObjectOutputStream(byteStream)
    out.writeObject(multiHash)
    out.close()
    val in = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray))
    val deserializedTry = Try(in.readObject().asInstanceOf[MultiHash])

    deserializedTry must beSuccessfulTry { deserialized: MultiHash =>
      deserialized == multiHash
    }
  }
}
