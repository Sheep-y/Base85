# Base85 Encoder/Decoder #

High speed, standalone [Base85](https://en.wikipedia.org/wiki/Ascii85) encoder/decoder for Java 8+.
Decode twice as fast as [libxjava](https://github.com/mcpat/java-microedition-libraries/blob/master/libxjava/libxjava-cldc/src/shared/java/com/github/libxjava/io/Base85.java) and encode four times as fast.

Supports [RFC 1924](https://tools.ietf.org/html/rfc1924), [Z85](https://rfc.zeromq.org/spec:32/Z85/), and [Ascii85](https://en.wikipedia.org/wiki/Ascii85) (support null and space compression).


JavaScript (EcmaScript) encoder has also been implemented.  Requires modern browser/node with ES6 module and TypedArray support.
Single block encoding / decoding (strict RFC 1924) requires BigInt support.


License: [Apache v2](https://github.com/Sheep-y/Base85/blob/master/LICENSE)