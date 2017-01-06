scala-multihash
===============

[![](https://img.shields.io/badge/project-multiformats-blue.svg?style=flat-square)](https://github.com/multiformats/multiformats)
[![](https://img.shields.io/badge/freenode-%23ipfs-blue.svg?style=flat-square)](https://webchat.freenode.net/?channels=%23ipfs)
[![](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

> Scala [multihash](//github.com/multiformats/multihash) implementation

## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Maintainers](#maintainers)
- [Contribute](#contribute)
- [License](#license)

## Install

scala-multihash can either be installed from Maven, or by directly referencing the github project
using `sbt`. 

To install from maven:

```scala
// in Build.scala
resolvers ++= Seq(
    Resolver.sonatypeRepo("public")
)

libraryDependencies ++= Seq(
  "io.mediachain" %% "multihash" % "0.1.0"
)
```

This may be out of date, so if you want to keep up with the latest development,
you can include a project reference, pinned to a specific commit hash:

```scala
// in Build.scala
val scalaMultihashCommit = "56e9b6b559463ef85e3eb41e054e62a47eff33eb" // or w/e
lazy val scalaMultihash = RootProject(uri(
s"git://github.com/multiformats/scala-multihash.git#$scalaMultihashCommit"

lazy val MyProject = Project(...).dependsOn(scalaMultihash)
```

## Usage

```scala
import io.mediachain.multihash.MultiHash

// create a SHA-256 multihash digest of a byte array:
val message = "hello world"
val mh = MultiHash.hashWithSHA256(message.getBytes("utf-8"))
println(mh.base58) // => QmaozNR7DZHQK1ZcU9p7QdrshMvXqWK6gpu5rmrkPdT3L4
println(mh.hex) // => 1220b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
val mhBytes: Array[Byte] = mh.bytes

// decode a base58-encoded multihash string:
//
// Operations that might fail return an Xor, which is like an Either from the scala standard lib
// An Xor will either contain Xor.Left(errorValue) or Xor.Right(successValue)
// In contrast to Either, Xor can be used in for comprehensions, and will
// "short circuit" if an error occurs:

import cats.data.Xor
import io.mediachain.multihash.MultiHashError
val decodedXor: Xor[MultiHashError, MultiHash] = 
  MultiHash.fromBase58("QmaozNR7DZHQK1ZcU9p7QdrshMvXqWK6gpu5rmrkPdT3L4")

val networkThing = for {
  mh <- MultiHash.fromBase58("nope, sorry")
  value <- NetworkStuff.fetchByMultihash(mh)  // assume fetchByMultihash also returns an Xor
} yield value

// In the example above, NetworkStuff.fetchByMultihash will never execute,
// because the for comprehension will abort after Multihash.fromBase58 fails.
// Note that `networkThing` will also be an Xor.

// You can also use map, flatMap, etc on Xor values directly, and you can pattern
// match against them:
 
val hexStringXor: Xor[MultiHashError, String] = 
  MultiHash.fromBase58("QmaozNR7DZHQK1ZcU9p7QdrshMvXqWK6gpu5rmrkPdT3L4")
    .map(_.hex)

hexStringXor match {
  case Xor.Right(hexString) => println(hexString)
  case Xor.Left(err) => println("error: $err")
}

// prints "1220b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

// if you're certain that an operation won't error, you can use Xor.getOrElse
val hexString: String = 
  MultiHash.fromBase58("QmaozNR7DZHQK1ZcU9p7QdrshMvXqWK6gpu5rmrkPdT3L4")
    .map(_.hex)
    .getOrElse {
        throw new Exception("something went horribly wrong")
    }
```

## Limitations

Currently, SHA-3, Blake-2b and Blake-2s are unsupported.

There's only one "friendly" digest method, `MultiHash.hashWithSHA256`, but you can
encode a multihash with other digests using `MultiHash.fromHash`:

```scala
import java.security.MessageDigest
import io.mediachain.multihash.{MultiHash, MultiHashError}
import cats.data.Xor

val digest = MessageDigest.getInstance("SHA-1").digest("hello sha-1".getBytes)
val mh: Xor[MultiHashError, MultiHash] = MultiHash.fromHash(MultiHash.sha1, digest)
```

## Maintainers

Captain: [@parkan](https://github.com/parkan).

## Contribute

Contributions welcome. Please check out [the issues](https://github.com/multiformats/scala-multihash/issues).

Check out our [contributing document](https://github.com/multiformats/multiformats/blob/master/contributing.md) for more information on how we work, and about contributing in general. Please be aware that all interactions related to multiformats are subject to the IPFS [Code of Conduct](https://github.com/ipfs/community/blob/master/code-of-conduct.md).

Small note: If editing the README, please conform to the [standard-readme](https://github.com/RichardLitt/standard-readme) specification.

## License

[MIT](LICENSE) © 2016 Ratio Club, Inc.
