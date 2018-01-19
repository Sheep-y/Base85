package sheepy.util.text;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;

/**
 * Example: <br>
 * <code> String encodedString = Base85.getZ85Encoder().encodeToString( byteArray ); <br>
 * byte[] data = Base85.getZ85Decoder().decodeToBytes( encodedString );</code>
 */
public class Base85 {
   // Constants used in encoding and decoding
   private static final long Power5 = 52200625; // 85^5
   private static final long Power4 = 614125;  // 85^4
   private static final long Power3 = 7225;   // 85^3

   /** This is a skeleton class for encoding data using the Base85 encoding scheme,
     * in the same style as Base64 encoder.
     * Encoder instances can be safely shared by multiple threads.
     */
   public static abstract class Encoder {
      /** Calculate byte length of encoded data.
        *
        * @param data data to be encoded
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return maximum length of encoded data in byte
        */
      public int calcEncodedLength ( final byte[] data, final int offset, final int length ) {
         return (int) Math.ceil( length * 1.25 );
      }

      /** Encode string data into Base85 string.  The input data will be converted to byte using UTF-8 encoding.
        * @param data text to encode
        * @return encoded Base85 string
        */
      public final String encode ( final String data ) {
         return encodeToString( data.getBytes( UTF_8 ) );
      }

      /** Encode binary data into Base85 string.
        * @param data data to encode
        * @return encoded Base85 string
        */
      public final String encodeToString ( final byte[] data ) {
         return new String( encode( data ), US_ASCII );
      }

      /** Encode part of binary data into Base85 string.
        * @param data data to encode
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return encoded Base85 string
        */
      public final String encodeToString ( final byte[] data, final int offset, final int length ) {
         return new String( encode( data, offset, length ), US_ASCII );
      }

      /** Encode binary data into a new byte array.
        * @param data data to encode
        * @return encoded Base85 encoded data in ASCII charset
        */
      public final byte[] encode ( final byte[] data ) {
         return encode( data, 0, data.length );
      }

      /** Encode part of a binary data into a new byte array.
        * @param data array with data to encode
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @return encoded Base85 encoded data in ASCII charset
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough (data won't be written)
        */
      public final byte[] encode ( final byte[] data, final int offset, final int length ) {
         checkBounds( data, offset, length );
         byte[] out = new byte[ calcEncodedLength( data, offset, length ) ];
         _encode( data, offset, length, out, 0 );
         return out;
      }

      /** Encode part of a byte array and write the output into a byte array in ASCII charset.
        * @param data array with data to encode
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @param out array to write encoded data to
        * @param out_offset byte offset to start writing encoded data to
        * @return number of encoded bytes
        * @throws IllegalArgumentException if offset or length is negative, or if either array is not big enough (data may be written)
        */
      public final int encode ( final byte[] data, final int offset, final int length, final byte[] out, final int out_offset ) {
         int size = calcEncodedLength( data, offset, length );
         checkBounds( data, offset, length );
         checkBounds( out, out_offset, size );
         return _encode( data, offset, length, out, out_offset );
      }

      /** Encode the data as one block in reverse output order.
        * This is the strict algorithm specified by RFC 1924 for IP address encoding,
        * when the data is exactly 16 bytes (128 bits) long.
        * Because the whole input data is encoded as one big block,
        * this is much less efficient than the more common encodings.
        * @see https://tools.ietf.org/html/rfc1924
        * @param data Byte data to encode
        * @return Encoded data in ascii encoding
        */
      public byte[] encodeBlockReverse ( byte[] data ) {
         int size = Math.max( 0, (int) Math.ceil( data.length * 1.25 ) );
         byte[] result = new byte[ size ];
         encodeBlockReverse ( data, 0, data.length, result, 0 );
         return result;
      }

      /** Encode part of data as one block in reverse output order into output array.
        * This is the strict algorithm specified by RFC 1924 for IP address encoding,
        * when the data part is exactly 16 bytes (128 bits) long.
        * Because the whole input data part is encoded as one big block,
        * this is much less efficient than the more common encodings.
        * @see https://tools.ietf.org/html/rfc1924
        * @param data array to read data from
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @param out array to write encoded data to
        * @param out_offset byte offset to start writing encoded data to
        * @return number of encoded bytes
        */
      public int encodeBlockReverse ( byte[] data, int offset, int length, byte[] out, int out_offset ) {
         int size = (int) Math.ceil( length * 1.25 );
         checkBounds( data, offset, length );
         checkBounds( out, out_offset, size );
         if ( offset != 0 || length != data.length )
            data = Arrays.copyOfRange( data, offset, offset + length );
         BigInteger blockSum = new BigInteger( 1, data ), b85 = BigInteger.valueOf( 85 );
         byte[] map = getEncodeMap();
         for ( int i = size + out_offset - 1 ; i >= out_offset ; i-- ) {
            BigInteger[] mod = blockSum.divideAndRemainder( b85 );
            out[ i ] = map[ mod[1].intValue() ];
            blockSum = mod[0];
         }
         return size;
      }

      protected abstract int _encode ( byte[] in, int ri, int rlen, byte[] out, int wi );
      protected abstract byte[] getEncodeMap();
      public String getCharset() { return new String( getEncodeMap(), US_ASCII ); }
   }

   /** This is a skeleton class for decoding data in the Base85 encoding scheme.
     * in the same style as Base64 encoder.
     * Decoder instances can be safely shared by multiple threads.
     */
   public static abstract class Decoder {
      /** Calculate byte length of decoded data.
        * @param data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return number of byte of decoded data
        */
      public int calcDecodedLength ( final byte[] data, final int offset, final int length ) {
         return (int) ( length * 0.8 );
      }

      /** Decode Base85 string into a UTF-8 string.
        * @param data text to decode
        * @return decoded UTF-8 string
        */
      public final String decode ( final String data ) {
         return new String( decode( data.getBytes( US_ASCII ) ), UTF_8 );
      }

      /** Decode ASCII Base85 data into a new byte array.
        * @param data data to decode
        * @return decoded binary data
        */
      public final byte[] decode ( final byte[] data ) {
         return decode( data, 0, data.length );
      }

      /** Decode Base85 string into a new byte array.
        * @param data data to decode
        * @return decoded binary data
        */
      public final byte[] decodeToBytes ( final String data ) {
         return decode( data.getBytes( US_ASCII ) );
      }

      /** Decode ASCII Base85 data into a new byte array.
        * @param data array with data to decode
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @return decoded binary data
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough (data won't be written)
        */
      public final byte[] decode ( final byte[] data, final int offset, final int length ) {
         checkBounds( data, offset, length );
         byte[] result = new byte[ calcDecodedLength( data, offset, length ) ];
         try {
            _decode( data, offset, length, result, 0 );
         } catch ( ArrayIndexOutOfBoundsException ex ) { throwMalformed( ex ); }
         return result;
      }

      /** Decode part of a byte array and write the output into a byte array in ASCII charset.
        * @param data array with data to encode
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @param out array to write decoded data to
        * @param out_offset byte offset to start writing decoded data to
        * @return number of decoded bytes
        * @throws IllegalArgumentException if offset or length is negative, or if either array is not big enough (data won't be written)
        */
      public final int decode ( final byte[] data, final int offset, final int length, final byte[] out, final int out_offset ) {
         int size = calcDecodedLength( data, offset, length );
         checkBounds( data, offset, length );
         checkBounds( out, out_offset, size );
         try {
            _decode( data, offset, length, out, out_offset );
         } catch ( ArrayIndexOutOfBoundsException ex ) { throwMalformed( ex ); }
         return size;
      }

      /** Decode the data as one block in reverse input order.
        * This is the strict algorithm specified by RFC 1924 for IP address decoding,
        * when the data is exactly 16 bytes (128 bits) long.
        * @see https://tools.ietf.org/html/rfc1924
        * @param data Byte data to encode
        * @return Encoded data in ascii encoding
        */
      public byte[] decodeBlockReverse ( byte[] data ) {
         int size = Math.max( 0, (int) Math.ceil( data.length * 0.8 ) );
         byte[] result = new byte[ size ];
         decodeBlockReverse ( data, 0, data.length, result, 0 );
         return result;
      }

      /** Decode part of data as one block in reverse input order into output array.
        * This is the strict algorithm specified by RFC 1924 for IP address decoding,
        * when the data part is exactly 16 bytes (128 bits) long.
        * @see https://tools.ietf.org/html/rfc1924
        * @param data array to read data from
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @param out array to write decoded data to
        * @param out_offset byte offset to start writing decoded data to
        * @return number of decoded bytes
        */
      public int decodeBlockReverse ( byte[] data, int offset, int length, byte[] out, int out_offset ) {
         int size = (int) Math.ceil( length * 0.8 );
         checkBounds( data, offset, length );
         checkBounds( out, out_offset, size );
         BigInteger blockSum = BigInteger.ZERO, b85 = BigInteger.valueOf( 85 );
         byte[] map = getDecodeMap();
         try {
            for ( int i = offset, len = offset + length ; i < len ; i++ )
               blockSum = blockSum.multiply( b85 ).add( BigInteger.valueOf( map[ data[ i ] ] ) );
         } catch ( ArrayIndexOutOfBoundsException ex ) { throwMalformed( ex ); }
         System.arraycopy( blockSum.toByteArray(), 0, out, out_offset, size );
         return size;
      }

      /** Test that given data can be decoded correctly.
        * @param data Encoded data in ascii charset
        * @return true if data is of correct length and composed of correct characters
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough
        */
      public boolean test ( final byte[] data ) {
         return test( data, 0, data.length );
      }

      /** Test that part of given data can be decoded correctly.
        * @param data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return true if data is of correct length and composed of correct characters
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough
        */
      public boolean test ( final byte[] data, final int offset, final int length ) { throw new UnsupportedOperationException( "Not implemented" ); }

      protected boolean _test( final byte[] data, final int offset, final int length, final boolean[] valids ) {
         checkBounds( data, offset, length );
         for ( int i = offset, len = offset + length ; i < len ; i++ ) {
            byte e = data[i];
            if ( e < 0 || ! valids[ e ] )
               return false;
         }
         return true;
      }

      protected RuntimeException throwMalformed ( Exception ex ) {
         throw new IllegalArgumentException( "Malformed Base85/" + getName() + " data", ex );
      }

      protected abstract int _decode ( byte[] in, int ri, int rlen, byte[] out, int wi );
      protected abstract byte[] getDecodeMap();
      protected abstract String getName();
   }

   private static abstract class SimpleEncoder extends Encoder {
      @Override protected int _encode( byte[] in, int ri, int rlen, byte[] out, int wi ) {
         long sum;
         final int loop = rlen / 4;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array(), encodeMap = getEncodeMap();
         for ( int i = loop ; i > 0 ; i-- ) {
            System.arraycopy( in, ri, buf, 0, 4 );
            ri += 4;
            sum = buffer.getInt( 0 ) & 0x00000000ffffffffL;
            out[wi  ] = encodeMap[ (int) ( sum / Power5 ) ]; sum %= Power5;
            out[wi+1] = encodeMap[ (int) ( sum / Power4 ) ]; sum %= Power4;
            out[wi+2] = encodeMap[ (int) ( sum / Power3 ) ]; sum %= Power3;
            out[wi+3] = encodeMap[ (int) ( sum / 85 ) ];
            out[wi+4] = encodeMap[ (int) ( sum % 85 ) ];
            wi += 5;
         }
         rlen %= 4;
         if ( rlen == 0 ) return loop * 5;
         sum = 0;
         for ( int i = 0 ; i < rlen ; i++ )
            sum = ( sum << 8 ) + ( in[ri+i] & 0xff );
         switch ( rlen ) {
            case 3: out[wi++] = encodeMap[ (int) ( sum / Power4 ) ]; sum %= Power4;
            case 2: out[wi++] = encodeMap[ (int) ( sum / Power3 ) ]; sum %= Power3;
         }
         out[wi  ] = encodeMap[ (int) ( sum / 85 ) ];
         out[wi+1] = encodeMap[ (int) ( sum % 85 ) ];
         return loop * 5 + rlen + 1;
      }
   }

   /** This class encodes data in the Base85 encoding scheme using the character set described by IETF RFC 1924,
     * but in the efficient algorithm of Ascii85 and Z85.
     * This scheme does not use quotes, comma, or slash, and can usually be used in sql, json, csv etc. without escaping.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Encoder extends SimpleEncoder {
      private static final byte[] EncodeMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return EncodeMap; }
   }

   /** This class encodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
     * This scheme does not use quotes or comma, and can usually be used in sql, json, csv etc. without escaping.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://rfc.zeromq.org/spec:32/Z85/
     */
   public static class Z85Encoder extends SimpleEncoder {
      private static final byte[] EncodeMap = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return EncodeMap; }
   }

   private static abstract class SimpleDecoder extends Decoder {
      @Override protected boolean _test( byte[] encoded_data, int offset, int length, boolean[] valids ) {
         if ( ! super._test( encoded_data, offset, length, valids ) ) return false;
         calcDecodedLength( encoded_data, offset, length ); // Throw IllegalArgumentException if length is incorrect.
         return true;
      }

      @Override public int calcDecodedLength ( byte[] encoded_data, int offset, int length ) {
         if ( length % 5 == 1 ) throw new IllegalArgumentException( length + " is not a valid Base85/" + getName() + " data length." );
         return super.calcDecodedLength( encoded_data, offset, length );
      }

      @Override protected int _decode ( byte[] in, int ri, int rlen, final byte[] out, int wi ) {
         final int loop = rlen / 5;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array(), decodeMap = getDecodeMap();
         for ( int i = loop ; i > 0 ; i-- ) {
            buffer.putInt( 0, (int) ( decodeMap[ in[ri  ] ] * Power5 +
                                      decodeMap[ in[ri+1] ] * Power4 +
                                      decodeMap[ in[ri+2] ] * Power3 +
                                      decodeMap[ in[ri+3] ] * 85 +
                                      decodeMap[ in[ri+4] ] ) );
            ri += 5;
            out[wi  ] = buf[0];
            out[wi+1] = buf[1];
            out[wi+2] = buf[2];
            out[wi+3] = buf[3];
            wi += 4;
         }
         rlen %= 5;
         if ( rlen == 0 ) return loop * 4;
         final byte[] data = new byte[rlen];
         --rlen;
         for ( int i = rlen ; i >= 0 ; i-- )
            data[i] = decodeMap[ in[ri+i] ];
         long sum;
         switch ( rlen ) {
            case 3: sum = data[0]*Power4 + data[1]*Power3 + data[2]*85 + data[3]; break;
            case 2: sum = data[0]*Power3 + data[1]*85 + data[2]; break;
            case 1: sum = data[0]*85 + data[1]; break;
            default: throw throwMalformed( null );
         }
         switch ( rlen ) {
            case 3: out[wi++] = (byte)( sum >>> 16 );
            case 2: out[wi++] = (byte)( sum >>> 8  );
            case 1: out[wi  ] = (byte)  sum;
         }
         return loop * 4 + rlen;
      }
   }

   /** This class decodes data in the Base85 encoding using the character set described by IETF RFC 1924,
     * in the efficient algorithm of Ascii85 and Z85.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Decoder extends SimpleDecoder {
      private static final byte[] DecodeMap = new byte[127];
      private static final boolean[] ValidBytes = new boolean[255];
      static {
         buildDecodeMap( Rfc1924Encoder.EncodeMap, DecodeMap, ValidBytes );
      }
      @Override protected String getName() { return "RFC1924"; }
      @Override protected byte[] getDecodeMap() { return DecodeMap; }
      @Override public boolean test ( byte[] encoded_data, int offset, int length ) { return _test( encoded_data, offset, length, ValidBytes ); }
   }

   /** This class decodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://rfc.zeromq.org/spec:32/Z85/
     */
   public static class Z85Decoder extends SimpleDecoder {
      private static final byte[] DecodeMap = new byte[126];
      private static final boolean[] ValidBytes = new boolean[255];
      static {
         buildDecodeMap( Z85Encoder.EncodeMap, DecodeMap, ValidBytes );
      }
      @Override protected String getName() { return "Z85"; }
      @Override protected byte[] getDecodeMap() { return DecodeMap; }
      @Override public boolean test ( byte[] encoded_data, int offset, int length ) { return _test( encoded_data, offset, length, ValidBytes ); }
   }

   private static void checkBounds ( byte[] data, int offset, int length ) {
      if ( offset < 0 || length < 0 || offset + length > data.length )
         throw new IllegalArgumentException();
   }

   private static void buildDecodeMap ( byte[] encodeMap, byte[] decodeMap, boolean[] validMap ) {
      for ( byte i = 0, len = (byte) encodeMap.length ; i < len ; i++ ) {
         byte b = encodeMap[ i ];
         decodeMap[ b ] = i;
         validMap [ b ] = true;
      }
   }

   private static Encoder RFC1924ENCODER;
   private static Decoder RFC1924DECODER;
   private static Encoder Z85ENCODER;
   private static Decoder Z85DECODER;

   public static Encoder getRfc1942Encoder() {
      if ( RFC1924ENCODER == null ) RFC1924ENCODER = new Rfc1924Encoder();
      return RFC1924ENCODER; // No worry if multiple encoder is created in multiple threads. Same for all.
   }
   public static Decoder getRfc1942Decoder() {
      if ( RFC1924DECODER == null ) RFC1924DECODER = new Rfc1924Decoder();
      return RFC1924DECODER;
   }
   public static Encoder getZ85Encoder() {
      if ( Z85ENCODER == null ) Z85ENCODER = new Z85Encoder();
      return Z85ENCODER;
   }
   public static Decoder getZ85Decoder() {
      if ( Z85DECODER == null ) Z85DECODER = new Z85Decoder();
      return Z85DECODER;
   }
}