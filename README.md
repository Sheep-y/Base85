# Base85 Encoder/Decoder #

High speed, standalone, thread-safe [Base85](https://en.wikipedia.org/wiki/Ascii85) encoder/decoder implemented in Java and JavaScript.

Supports [RFC 1924](https://tools.ietf.org/html/rfc1924), [Z85](https://rfc.zeromq.org/spec:32/Z85/), and [Ascii85](https://en.wikipedia.org/wiki/Ascii85) (with optional null and space compression).
Supports writing to a provided output buffer (`byte[]` for Java and any `TypedArray` for JavaScript).

Java implementation requires Java 8 or above.
JavaScript implementation requires TypedArray, and strict RFC 1924 (rarely used) requires BigInt.

Both implementations process the bulk of input data in unit of four or five bytes, using byte buffer to map bytes from/to 32 bits integer block, thus the requirements and the high performance.
Leftover data is processed separately, so the code is a bit more complicated than run of the mill Base 85 implementations.
But for practical purpose, each is an independent file that you can just drop in your project.

License: [Apache v2](https://github.com/Sheep-y/Base85/blob/master/LICENSE)