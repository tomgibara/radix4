Radix 4
=======

A binary-to-text encoding which is similar to Base64 encoding but with benefits
for some use-cases. Using the default encoding:

 * It maps binary to a subset of ASCII which is well suited to inclusion in URLs
   and filenames (specifically [-_A-Za-z0-9])
 * Termination characters (used to delimit data) are optional and the default is
   '.' which is again well suited to inclusion in URLs and filenames.
 * The ASCII characters used in the encoding are unchanged during encoding,
   meaning that in appropriate cases, *the encoding is idempotent*.

Encoding Examples
-----------------

Radix4 has a variety of encoding modes to suite different needs. Here are some
simple examples which encode ASCII character data:

* Using block encoding `This is encoded using Radix4` becomes
  `This-is-encoded-using-Radix4_33__F_F__`
* Using stream encoding, the same input `This is encoded using Radix4` becomes
  `_Thi3s-i3s-e_nco_dedF-us_ingF-Ra_dix_4`
* With optimistic block encoding `Radix4_is_idempotent_on_this_input` remains as
  `Radix4_is_idempotent_on_this_input`

Overview
--------

Radix4 encoding is similar to Base64 encoding. In both encodings, a sequence of
8 bit bytes is converted to a sequence of 6 bit words which is then converted to
ASCII by mapping each word to a designated ASCII character.

In Base64 encoding, each group of three bytes is encoded as four 6 bit words so:

    +--------+--------+--------+--------+--------+--------+
    +AAAAAAAA|BBBBBBBB|CCCCCCCC|AAAAAAAA|BBBBBBBB|CCCCCCCC|
    +--------+--------+--------+--------+--------+--------+

becomes encoded as

    +------+------+------+------+------+------+------+------+
    +AAAAAA|AABBBB|BBBBCC|CCCCCC|AAAAAA|AABBBB|BBBBCC|CCCCCC|
    +------+------+------+------+------+------+------+------+

Radix4 encoding operates similarly, except that the arrangement of the encoded
bits is different. The highest two bits (the radix) of each byte in the
group are collected into a single 6 bit word (referred to as the radix word):

    +------+------+------+------+------+------+------+------+
    +AAAAAA|BBBBBB|CCCCCC|AABBCC|AAAAAA|BBBBBB|CCCCCC|AABBCC|
    +------+------+------+------+------+------+------+------+

This has the advantage that byte values consistently produce the same 6 bit
words, with the exception of every fourth word, and these radix words can then
be optionally reordered:

    +------+------+------+------+------+------+     +------+------+
    +AAAAAA|BBBBBB|CCCCCC|AAAAAA|BBBBBB|CCCCCC| ... |AABBCC|AABBCC|
    +------+------+------+------+------+------+     +------+------+

A second difference between Radix4 encoding and Base64 encoding is that Radix4
encoding includes an additional pre-encoding transformation in which the bytes
to be encoded are substituted using a reversible lookup table.

The lookup table is constructed so as to map the ASCII byte values of the 64
designated characters to the value they represent in the encoding. This has the
effect of making the encoding *nearly* idempotent for these characters, since
the characters are guaranteed to be their own encoding. The encoding is only
nearly (and not fully) idempotent because the radix, which is guaranteed to be
zero, must still be encoded.

However, it is possible skip outputting any radix words until the first
non-zero radix word is encountered through the use of an additional character
referred to as the termination character.

Terminology
-------

* If the radix words are grouped at the end of the binary encoding, this is
  referred to as **block** mode, otherwise the mode is referred to as
  **stream**.

* If the encoding skips outputting the radix words until the first non-zero
  value, this is referred to as **optimistic**. The alternative (where every
  radix word is always output) is referred to as **pessimistic**.

* If the encoder is instructed to always include a termination character, this
  is referred to as a **terminated** encoding. Non-terminated encodings require
  an external end-of-stream condition to be correctly decoded.

Usage
-----

The library API encourages a fluent style of usage. The entry point for the
library is the `Radix4` class which is a pre-configured immutable and
serializable class which provides methods for Radix4 coding through its
`coding()` method which will return a thread-safe `Radix4Coding` instance that
provides a number of convenient coding methods:

* `OutputStream outputToStream(OutputStream out)`
* `OutputStream outputToWriter(Writer writer)`
* `OutputStream outputToBuilder(StringBuilder builder)`
* `InputStream inputFromStream(InputStream in)`
* `InputStream inputFromReader(Reader reader)`
* `InputStream inputFromChars(CharSequence chars)`
* `String encodeToString(byte[] bytes)`
* `byte[] encodeToBytes(byte[] bytes)`
* `byte[] decodeFromString(CharSequence chars)`
* `byte[] decodeFromBytes(byte[] bytes)`

Standard `Radix4` instances for block and stream based coding are obtained via
the static `Radix4.block()` and `Radix4.stream()` respectively. Configuring the
coding is achieved in three steps. First the `configure()` method is called on
an existing `Radix4` instance to get a new `Radix4Config` instance containing a
mutable copy of its configuration. Configuration changes are then applied by
chaining setters on the configuration (see Javadocs for details). Finally the
`use()` method is called on the `Radix4Config` instance to produce a new
`Radix4` instance reflecting the applied configuration changes.

Usage Examples
--------------

    byte[] bytes = { .... };
    String output = Radix4.block().coding().encodeToString(bytes);
    
    OutputStream underlying = ...;
    OutputStream out = Radix4.stream().coding().outputToStream(underlying);
    /* then write data to be encoded to output stream */
    
    Radix4 radix4 = Radix4.stream()
        .configure()
        .setOptimistic(false)
        .setLineLength(80)
        .setLineBreak("\r\n")
        .use();
    /* then use customized radix4 via coding() */

Note that all coding methods are available for all configurations.