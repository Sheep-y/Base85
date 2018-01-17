package sheepy.util.text;

import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Base85 {
   // Constants used in encoding and decoding
   private static final long Power5 = 52200625; // 85^5
   private static final long Power4 = 614125;  // 85^4
   private static final long Power3 = 7225;   // 85^3

   /** This is a skeleton class for encoding data using the Base85 encoding scheme.
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
         if ( offset < 0 || length < 0 || offset + length > data.length )
            throw new IllegalArgumentException();
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
         if ( offset < 0 || length < 0 || out_offset < 0 || offset + length > data.length || out_offset + size > out.length )
            throw new IllegalArgumentException();
         return _encode( data, offset, length, out, out_offset );
      }

      protected abstract int _encode ( byte[] in, int ri, int rlen, byte[] out, int wi );

      /** Get the matching decoder.
       * @return a matching decoder
       */
      public abstract Decoder getDecoder();
      protected abstract byte[] getEncodeMap();
      public String getCharset() { return new String( getEncodeMap(), US_ASCII ); }
   }

   /** This is a skeleton class for decoding data in the Base85 encoding scheme.
     * Decoder instances can be safely shared by multiple threads.
     */
   public static abstract class Decoder {
      private final boolean[] validBytes;
      private final int minValid, maxValid;

      public Decoder() {
         validBytes = new boolean[256];
         int min = 255, max = 0;
         for ( byte b : getEncoder().getCharset().getBytes( US_ASCII ) ) {
            validBytes[ b ] = true;
            if ( b < min ) min = b;
            else if ( b > max ) max = b;
         }
         minValid = min;
         maxValid = max;
      }

      /** Test that given data can be decoded correctly.
        * @param encoded_data Encoded data in ascii charset
        * @return true if data is of correct length and composed of correct characters
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough
        */
      public boolean test ( final byte[] encoded_data ) {
         return test( encoded_data, 0, encoded_data.length );
      }

      /** Test that part of given data can be decoded correctly.
        * @param encoded_data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return true if data is of correct length and composed of correct characters
        * @throws IllegalArgumentException if offset or length is negative, or if data array is not big enough
        */
      public boolean test ( final byte[] encoded_data, final int offset, final int length ) {
         if ( offset < 0 || length < 0 || offset + length > encoded_data.length )
            throw new IllegalArgumentException();
         for ( int i = offset, len = offset + length ; i < len ; i++ ) {
            byte e = encoded_data[i];
            if ( e < minValid || e > maxValid || ! validBytes[ e ] )
               return false;
         }
         return true;
      }

      /** Calculate byte length of decoded data.
        * @param encoded_data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return number of byte of decoded data
        */
      public int calcDecodedLength ( final byte[] encoded_data, final int offset, final int length ) {
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
         if ( offset < 0 || length < 0 || offset + length > data.length )
            throw new IllegalArgumentException( "Incorrect offset/length" );
         byte[] result = new byte[ calcDecodedLength( data, offset, length ) ];
         try {
            _decode( data, offset, length, result, 0 );
         } catch ( ArrayIndexOutOfBoundsException ex ) {
            throw new IllegalArgumentException( "Malformed Base85/" + getName() + " data" );
         }
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
         if ( offset < 0 || length < 0 || out_offset < 0 || offset + length > data.length || out_offset + size > out.length )
            throw new IllegalArgumentException();
         try {
            _decode( data, offset, length, out, out_offset );
         } catch ( ArrayIndexOutOfBoundsException ex ) {
            throw new IllegalArgumentException( "Malformed Base85/" + getName() + " data" );
         }
         return size;
      }

      /** Get the matching encoder.
       * @return a matching encoder
       */
      public abstract Encoder getEncoder();
      protected abstract int _decode ( byte[] in, int ri, int rlen, byte[] out, int wi );
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
            out[wi  ] = encodeMap[ (char) ( sum / Power5 ) ]; sum %= Power5;
            out[wi+1] = encodeMap[ (char) ( sum / Power4 ) ]; sum %= Power4;
            out[wi+2] = encodeMap[ (char) ( sum / Power3 ) ]; sum %= Power3;
            out[wi+3] = encodeMap[ (char) ( sum / 85 ) ];
            out[wi+4] = encodeMap[ (char) ( sum % 85 ) ];
            wi += 5;
         }
         rlen %= 4;
         if ( rlen == 0 ) return loop * 5;
         sum = 0;
         for ( int i = 0 ; i < rlen ; i++ )
            sum = ( sum << 8 ) + ( in[ri+i] & 0xff );
         switch ( rlen ) {
            case 3: out[wi] = encodeMap[ (char) ( sum / Power4 ) ]; sum %= Power4; ++wi;
            case 2: out[wi] = encodeMap[ (char) ( sum / Power3 ) ]; sum %= Power3; ++wi;
         }
         out[wi  ] = encodeMap[ (char) ( sum / 85 ) ];
         out[wi+1] = encodeMap[ (char) ( sum % 85 ) ];
         return loop * 5 + rlen + 1;
      }
   }

   /** This class encodes data in the Base85 encoding scheme as described by IETF RFC 1924.
     * This scheme does not use quotes, comma, or slash, and can usually be used in sql, json, csv etc. without escaping.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Encoder extends SimpleEncoder {
      private static final byte[] EncodeMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return EncodeMap; }
      @Override public Decoder getDecoder() { return getRfc1942Decoder(); }
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
      @Override public Decoder getDecoder() { return getZ85Decoder(); }
   }

   private static abstract class SimpleDecoder extends Decoder {
      protected abstract byte[] getDecodeMap();

      @Override public boolean test( byte[] encoded_data, int offset, int length) {
         if ( ! super.test( encoded_data, offset, length ) ) return false;
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
            default: throw new IllegalArgumentException( "Malformed Base85/" + getName() + " data" );
         }
         switch ( rlen ) {
            case 3: out[wi] = (byte) ( sum >>> 16 ); ++wi;
            case 2: out[wi] = (byte) ( sum >>> 8  ); ++wi;
            case 1: out[wi] = (byte) sum;
         }
         return loop * 4 + rlen;
      }
   }

   /** This class decodes data in the Base85 encoding scheme as described by IETF RFC 1924.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Decoder extends SimpleDecoder {
      @Override public Encoder getEncoder() { return getRfc1942Encoder(); }
      private static final byte[] DecodeMap = new byte[127];
      static {
         for ( int i = 0 ; i < 85 ; ++i )
            DecodeMap[ Rfc1924Encoder.EncodeMap[i] ] = (byte) i;
      }
      @Override protected byte[] getDecodeMap() { return DecodeMap; }
      @Override protected String getName() { return "RFC1924"; }
   }

   /** This class decodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://rfc.zeromq.org/spec:32/Z85/
     */
   public static class Z85Decoder extends SimpleDecoder {
      @Override public Encoder getEncoder() { return getZ85Encoder(); }
      private static final byte[] DecodeMap = new byte[126];
      static {
         for ( int i = 0 ; i < 85 ; ++i )
            DecodeMap[ Z85Encoder.EncodeMap[i] ] = (byte) i;
      }
      @Override protected byte[] getDecodeMap() { return DecodeMap; }
      @Override protected String getName() { return "Z85"; }
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