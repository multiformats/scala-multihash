package io.mediachain.multiaddr

/*
This code was shamelessly lifted from https://github.com/larroy/varint-scala
with many thanks!

 The MIT License (MIT)

Copyright (c) 2014 Pedro Larroy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE

  */

import java.nio.ByteBuffer
import scala.collection.mutable

/**
  * Encoding of integers with variable-length encoding
  */
object Varint {
  def writeSignedLong[T <: mutable.Buffer[Byte]](x: Long, dest: T): Unit = {
    // sign to even/odd mapping: http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
    writeUnsignedLong((x << 1) ^ (x >> 63), dest)
  }

  def writeUnsignedLong[T <: mutable.Buffer[Byte]](v: Long, dest: T): Unit = {
    var x = v
    while((x & 0xFFFFFFFFFFFFFF80L) != 0L) {
      dest += (((x & 0x7F) | 0x80).toByte)
      x >>>= 7
    }
    dest += ((x & 0x7F).toByte)
  }

  def writeSignedInt[T <: mutable.Buffer[Byte]](x: Int, dest: T): Unit = {
    writeUnsignedInt((x << 1) ^ (x >> 31), dest)
  }

  def writeUnsignedInt[T <: mutable.Buffer[Byte]](v: Int, dest: T): Unit = {
    var x = v
    while((x & 0xFFFFF80) != 0L) {
      dest += (((x & 0x7F) | 0x80).toByte)
      x >>>= 7
    }
    dest += ((x & 0x7F).toByte)
  }

  def readSignedInt(src: ByteBuffer): Int = {
    val unsigned = readUnsignedInt(src)
    // undo even odd mapping
    val tmp = (((unsigned << 31) >> 31) ^ unsigned) >> 1
    // restore sign
    tmp ^ (unsigned & (1 << 31))
  }

  def readUnsignedInt(src: ByteBuffer): Int = {
    var i = 0
    var v = 0
    var read = 0
    do {
      read = src.get
      v |= (read & 0x7F) << i
      i += 7
      require(i <= 35)
    } while((read & 0x80) != 0)
    v
  }


  def readSignedLong(src: ByteBuffer): Long = {
    val unsigned = readUnsignedLong(src)
    // undo even odd mapping
    val tmp = (((unsigned << 63) >> 63) ^ unsigned) >> 1
    // restore sign
    tmp ^ (unsigned & (1L << 63))
  }

  def readUnsignedLong(src: ByteBuffer): Long = {
    var i = 0
    var v = 0L
    var read = 0L
    do {
      read = src.get
      v |= (read & 0x7F) << i
      i += 7
      require(i <= 70)
    } while((read & 0x80L) != 0)
    v
  }
}
