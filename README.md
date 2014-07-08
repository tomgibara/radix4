Radix 4
=======

A binary-to-text encoding which is similar to Base64 encoding but with the following differences:

 * It maps binary to a subset of ASCII which is well suited to inclusion in URLs and filenames [-_A-Za-z0-9]
 * Termination characters (to indicate the EOS) are optional and consist of '.'s.
 * Many ASCII characters are preserved by the encoding.

