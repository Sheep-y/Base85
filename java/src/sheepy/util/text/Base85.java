package sheepy.util.text;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Example: <br>
 * <code> String encodedString = Base85.getZ85Encoder().encodeToString( byteArray ); <br>
 * byte[] data = Base85.getZ85Decoder().decodeToBytes( encodedString );</code>
 */
public class Base85 {
   // Constants used in encoding and decoding
   private static final long Power4 = 52200625; // 85^4
   private static final long Power3 = 614125;  // 85^3
   private static final long Power2 = 7225;   // 85^2

   /** This is a base class for encoding data using the Base85 encoding scheme,
     * in the same style as Base64 encoder.
     * Just override {@link #getEncodeMap()} to create a fully functional encoder.
     * Encoder instances can be safely shared by multiple threads.
     */
   public static abstract class Encoder {
      /** Calculate byte length of encoded string.
        *
        * @param data string to be encoded in UTF-8 bytes
        * @return length of encoded data in byte
        */
      public int calcEncodedLength ( final String data ) {
         return calcEncodedLength( data.getBytes( UTF_8 ) );
      }

      /** Calculate byte length of encoded data.
        *
        * @param data data to be encoded
        * @return length of encoded data in byte
        */
      public int calcEncodedLength ( final byte[] data ) {
         return calcEncodedLength( data, 0, data.length );
      }

      /** Calculate byte length of encoded data.
        *
        * @param data data to be encoded
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return length of encoded data in byte
        */
      public int calcEncodedLength ( final byte[] data, final int offset, final int length ) {
         if ( offset < 0 || length < 0 ) throw new IllegalArgumentException( "Offset and length must not be negative" );
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
        */
      public final byte[] encode ( final byte[] data, final int offset, final int length ) {
         byte[] out = new byte[ calcEncodedLength( data, offset, length ) ];
         int len = _encode( data, offset, length, out, 0 );
         if ( out.length == len ) return out;
         return Arrays.copyOf( out, len );
      }

      /** Encode part of a byte array and write the output into a byte array in ASCII charset.
        * @param data array with data to encode
        * @param offset byte offset to start reading data
        * @param length number of byte to read
        * @param out array to write encoded data to
        * @param out_offset byte offset to start writing encoded data to
        * @return number of encoded bytes
        */
      public final int encode ( final byte[] data, final int offset, final int length, final byte[] out, final int out_offset ) {
         int size = calcEncodedLength( data, offset, length );
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

      protected int _encodeDangling ( final byte[] encodeMap, final byte[] out, final int wi, final ByteBuffer buffer, int leftover ) {
         long sum = buffer.getInt( 0 ) & 0x00000000ffffffffL;
         out[wi  ] = encodeMap[ (int) ( sum / Power4 ) ]; sum %= Power4;
         out[wi+1] = encodeMap[ (int) ( sum / Power3 ) ]; sum %= Power3;
         if ( leftover >= 2 ) {
            out[wi+2] = encodeMap[ (int) ( sum / Power2 ) ]; sum %= Power2;
            if ( leftover >= 3 )
               out[wi+3] = encodeMap[ (int) ( sum / 85 ) ];
         }
         return leftover + 1;
      }

      protected int _encode ( byte[] in, int ri, int rlen, byte[] out, int wi ) {
         final int wo = wi;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array(), encodeMap = getEncodeMap();
         for ( int loop = rlen / 4 ; loop > 0 ; loop--, ri += 4 ) {
            System.arraycopy( in, ri, buf, 0, 4 );
            wi = _writeData( buffer.getInt( 0 ) & 0x00000000ffffffffL, encodeMap, out, wi );
         }
         int leftover = rlen % 4;
         if ( leftover == 0 ) return wi - wo;
         buffer.putInt( 0, 0 );
         System.arraycopy( in, ri, buf, 0, leftover );
         return wi - wo + _encodeDangling( encodeMap, out, wi, buffer, leftover );
      }

      protected int _writeData ( long sum, byte[] map, byte[] out, int wi ) {
         out[wi  ] = map[ (int) ( sum / Power4 ) ]; sum %= Power4;
         out[wi+1] = map[ (int) ( sum / Power3 ) ]; sum %= Power3;
         out[wi+2] = map[ (int) ( sum / Power2 ) ]; sum %= Power2;
         out[wi+3] = map[ (int) ( sum / 85 ) ];
         out[wi+4] = map[ (int) ( sum % 85 ) ];
         return wi+5;
      }

      protected abstract byte[] getEncodeMap();
      public String getCharset() { return new String( getEncodeMap(), US_ASCII ); }
   }

   /** This class encodes data in the Base85 encoding scheme using the character set described by IETF RFC 1924,
     * but in the efficient algorithm of Ascii85 and Z85.
     * This scheme does not use quotes, comma, or slash, and can usually be used in sql, json, csv etc. without escaping.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Encoder extends Encoder {
      private static final byte[] ENCODE_MAP = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return ENCODE_MAP; }
   }

   /** This class encodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
     * This scheme does not use quotes or comma, and can usually be used in sql, json, csv etc. without escaping.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://rfc.zeromq.org/spec:32/Z85/
     */
   public static class Z85Encoder extends Encoder {
      private static final byte[] ENCODE_MAP = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return ENCODE_MAP; }
   }

   /** This class encodes data in the Ascii85 encoding (Adobe variant without &lt;~ and ~&gt;).
     * Supports "z" and "y" compression, which can be disabled individually.
     * Line break is not supported.
     *
     * Encoder instances can be safely shared by multiple threads.
     * @see https://en.wikipedia.org/wiki/Ascii85
     */
   public static class Ascii85Encoder extends Encoder {
      private static final byte[] ENCODE_MAP = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstu".getBytes( US_ASCII );
      @Override protected byte[] getEncodeMap() { return ENCODE_MAP; }

      @Override public int calcEncodedLength ( byte[] data, int offset, int length ) {
         int result = super.calcEncodedLength( data, offset, length );
         if ( useZ || useY ) {
            final ByteBuffer buffer = ByteBuffer.wrap( data );
            for ( int i = offset, len = offset + length - 4 ; i <= len ; i += 4 )
               if ( useZ && data[i] == 0 ) {
                  if ( buffer.getInt( i ) == 0 ) result -= 4;
               } else if ( useY && data[i] == 0x20 )
                  if ( buffer.getInt( i ) == 0x20202020 ) result -= 4;
         }
         return result;
      }

      private boolean useZ = true;
      private boolean useY = true;
      private final ReadWriteLock lock = new ReentrantReadWriteLock( true );

      /** Set whether to enable encoding of four zeros into "z".
       *  @param compress true to enable, false to disable
       */
      public void setZeroCompression( boolean compress ) { lock.writeLock().lock(); try { useZ = compress; } finally { lock.writeLock().unlock(); } }
      /** Get zero compression status.
       * @return true if enabled, false if disabled
       */
      public boolean getZeroCompression() { lock.readLock().lock(); try { return useZ; } finally { lock.readLock().unlock(); } }
      /** Set whether to enable encoding of four spaces into "y".*
       *  @param compress true to enable, false to disable
       */
      public void setSpaceCompression( boolean compress ) { lock.writeLock().lock(); try { useY = compress; } finally { lock.writeLock().unlock(); } }
      /** Get space compression status.
       * @return true if enabled, false if disabled
       */
      public boolean getSpaceCompression() { lock.readLock().lock(); try { return useY; } finally { lock.readLock().unlock(); } }

      @Override protected int _encode( byte[] in, int ri, int rlen, byte[] out, int wi ) {
         lock.readLock().lock();
         try {
            return super._encode( in, ri, rlen, out, wi );
         } finally {
            lock.readLock().unlock();
         }
      }

      @Override protected int _writeData( long sum, byte[] map, byte[] out, int wi ) {
         if ( useZ && sum == 0 )
            out[wi++] = 'z';

         else if ( useY && sum == 0x20202020 )
            out[wi++] = 'y';

         else
            return super._writeData( sum, map, out, wi );

         return wi;
      }
   }

   /** This is a skeleton class for decoding data in the Base85 encoding scheme.
     * in the same style as Base64 decoder.
     * Just override {@link #getEncodeMap()} and {@link #getName()} to create a fully functional decoder.
     * Decoder instances can be safely shared by multiple threads.
     */
   public static abstract class Decoder {
      /** Calculate byte length of decoded data.
        * Assumes data is correct; use test method to validate data.
        *
        * @param data Encoded data in ascii charset
        * @return number of byte of decoded data
        */
      public int calcDecodedLength ( String data ) {
         return calcDecodedLength( data.getBytes( US_ASCII ) );
      }

      /** Calculate byte length of decoded data.
        * Assumes data is correct; use test method to validate data.
        *
        * @param data Encoded data in ascii charset
        * @return number of byte of decoded data
        */
      public int calcDecodedLength ( final byte[] data ) {
         return calcDecodedLength( data, 0, data.length );
      }

      /** Calculate byte length of decoded data.
        * Assumes data is correct; use test method to validate data.
        *
        * @param data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return number of byte of decoded data
        */
      public int calcDecodedLength ( final byte[] data, final int offset, final int length ) {
         if ( length % 5 == 1 ) throw new IllegalArgumentException( length + " is not a valid Base85/" + getName() + " data length." );
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
         byte[] result = new byte[ calcDecodedLength( data, offset, length ) ];
         try {
            int len = _decode( data, offset, length, result, 0 );
            // Should not happen, but fitting the size makes sure tests will fail when it does happen.
            if ( result.length != len ) return Arrays.copyOf( result, len );
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
        */
      public boolean test ( final String data ) {
         return test( data.getBytes( US_ASCII ) );
      }

      /** Test that given data can be decoded correctly.
        * @param data Encoded data in ascii charset
        * @return true if data is of correct length and composed of correct characters
        */
      public boolean test ( final byte[] data ) {
         return test( data, 0, data.length );
      }

      /** Test that part of given data can be decoded correctly.
        * @param data Encoded data in ascii charset
        * @param offset byte offset that data starts
        * @param length number of data bytes
        * @return true if data is of correct length and composed of correct characters
        */
      public boolean test ( byte[] encoded_data, int offset, int length ) { return _test( encoded_data, offset, length ); }

      protected boolean _test( final byte[] data, final int offset, final int length ) {
         boolean[] valids = getValidBytes();
         for ( int i = offset, len = offset + length ; i < len ; i++ ) {
            byte e = data[i];
            if ( e < 0 || ! valids[ e ] )
               return false;
         }
         try {
            calcDecodedLength( data, offset, length );
         } catch ( IllegalArgumentException ex ) {
            return false;
         }
         return true;
      }

      protected RuntimeException throwMalformed ( Exception ex ) {
         throw new IllegalArgumentException( "Malformed Base85/" + getName() + " data", ex );
      }

      protected int _decodeDangling ( final byte[] decodeMap, final byte[] in, final int ri, final ByteBuffer buffer, int leftover ) {
         if ( leftover == 1 ) throwMalformed( null );
         long sum = decodeMap[ in[ri  ] ] * Power4 +
                    decodeMap[ in[ri+1] ] * Power3 + 85;
         if ( leftover >= 3 ) {
            sum   += decodeMap[ in[ri+2] ] * Power2;
            if ( leftover >= 4 )
               sum += decodeMap[ in[ri+3] ] * 85;
            else
               sum += Power2;
         } else
            sum += Power3 + Power2;
         buffer.putInt( 0, (int) sum );
         return leftover-1;
      }

      protected int _decode ( byte[] in, int ri, int rlen, final byte[] out, int wi ) {
         final int loop = rlen / 5, wo = wi;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array(), decodeMap = getDecodeMap();
         for ( int i = loop ; i > 0 ; i--, wi += 4 ) {
            _putData( buffer, decodeMap, in, ri );
            ri += 5;
            System.arraycopy( buf, 0, out, wi, 4 );
         }
         int leftover = rlen % 5;
         if ( leftover == 0 ) return wi - wo;
         leftover = _decodeDangling( decodeMap, in, ri, buffer, leftover );
         System.arraycopy( buf, 0, out, wi, leftover );
         return wi - wo + leftover;
      }

      protected void _putData ( ByteBuffer buffer, byte[] map, byte[] in, int ri ) {
         buffer.putInt( 0, (int) ( map[ in[ri  ] ] * Power4 +
                                   map[ in[ri+1] ] * Power3 +
                                   map[ in[ri+2] ] * Power2 +
                                   map[ in[ri+3] ] * 85 +
                                   map[ in[ri+4] ] ) );
      }

      protected abstract byte[] getDecodeMap();
      protected abstract boolean[] getValidBytes();
      protected abstract String getName();
   }

   /** This class decodes data in the Base85 encoding using the character set described by IETF RFC 1924,
     * in the efficient algorithm of Ascii85 and Z85.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://tools.ietf.org/html/rfc1924
     */
   public static class Rfc1924Decoder extends Decoder {
      private static final byte[] DECODE_MAP = new byte[127];
      private static final boolean[] VALID_BYTES = new boolean[255];
      static {
         buildDecodeMap( Rfc1924Encoder.ENCODE_MAP, DECODE_MAP, VALID_BYTES );
      }
      @Override protected String getName() { return "RFC1924"; }
      @Override protected byte[] getDecodeMap() { return DECODE_MAP; }
      @Override protected boolean[] getValidBytes() { return VALID_BYTES; }
   }

   /** This class decodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://rfc.zeromq.org/spec:32/Z85/
     */
   public static class Z85Decoder extends Decoder {
      private static final byte[] DECODE_MAP = new byte[126];
      private static final boolean[] VALID_BYTES = new boolean[255];
      static {
         buildDecodeMap( Z85Encoder.ENCODE_MAP, DECODE_MAP, VALID_BYTES );
      }
      @Override protected String getName() { return "Z85"; }
      @Override protected byte[] getDecodeMap() { return DECODE_MAP; }
      @Override protected boolean[] getValidBytes() { return VALID_BYTES; }
   }

   /** This class decodes Ascii85 encoded data (Adobe variant without &lt;~ and ~&gt;).
    *  'y' and 'z' are always processed.  This keep the decoder simple.
     * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
     * Decoder instances can be safely shared by multiple threads.
     * @see https://en.wikipedia.org/wiki/Ascii85
     */
   private static class Ascii85Decoder extends Decoder {
      @Override public int calcDecodedLength ( byte[] encoded_data, int offset, int length ) {
         int deflated = length, len = offset + length, i;
         for ( i = offset ; i < len ; i += 5 )
            if ( encoded_data[i] == 'z' || encoded_data[i] == 'y' ) {
               deflated += 4;
               i -= 4;
            }
         if ( i != len ) {
            i -= 5;
            while ( i < len && ( encoded_data[i] == 'z' || encoded_data[i] == 'y' ) ) {
               deflated += 4;
               i -= 4;
            }
         }
         return super.calcDecodedLength( null, 0, deflated );
      }

      @Override public boolean test ( byte[] encoded_data, int offset, int length ) {
         try {
            int deviation = 0;
            for ( int i = offset, len = offset + length ; i < len ; i++ ) {
               byte e = encoded_data[i];
               if ( e < 0 || ! VALID_BYTES[ e ] )
                  if ( ( deviation + i - offset ) % 5 != 0 || ( e != 'z' && e != 'y' ) )
                     return false;
                  else
                     deviation += 4;
            }
            super.calcDecodedLength( null, 0, length + deviation ); // Validate length
         } catch ( IllegalArgumentException ignored ) { return false; }
         return true;
      }

      private static final byte[] DECODE_MAP = new byte[126];
      private static final boolean[] VALID_BYTES = new boolean[255];
      static {
         buildDecodeMap( Ascii85Encoder.ENCODE_MAP, DECODE_MAP, VALID_BYTES );
      }
      @Override protected String getName() { return "Ascii85"; }
      @Override protected byte[] getDecodeMap() { return DECODE_MAP; }
      @Override protected boolean[] getValidBytes() { return VALID_BYTES; }

      @Override protected int _decode ( byte[] in, int ri, int rlen, final byte[] out, int wi ) {
         final int re = ri + rlen, wo = wi;
         final ByteBuffer buffer = ByteBuffer.allocate( 4 );
         final byte[] buf = buffer.array(), decodeMap = getDecodeMap();
         for ( int max = ri + rlen, max2 = max - 4 ; ri < max ; wi += 4 ) {
            while ( ri < max &&( in[ri] == 'z' || in[ri] == 'y' ) ) {
               switch ( in[ri++] ) {
               case 'z': buffer.putInt( 0, 0 );
                         break;
               case 'y': buffer.putInt( 0, 0x20202020 );
                         break;
               }
               System.arraycopy( buf, 0, out, wi, 4 );
               wi += 4;
            }
            if ( ri < max2 ) {
               _putData( buffer, decodeMap, in, ri );;
               ri += 5;
               System.arraycopy( buf, 0, out, wi, 4 );
            } else
               break;
         }
         if ( re == ri ) return wi - wo;
         int leftover = _decodeDangling( decodeMap, in, ri, buffer, re - ri );
         System.arraycopy( buf, 0, out, wi, leftover );
         return wi - wo + leftover;
      }
   }

   private static void buildDecodeMap ( byte[] encodeMap, byte[] decodeMap, boolean[] validMap ) {
      for ( byte i = 0, len = (byte) encodeMap.length ; i < len ; i++ ) {
         byte b = encodeMap[ i ];
         decodeMap[ b ] = i;
         validMap [ b ] = true;
      }
   }

   private static void checkCharacterMap ( byte[] map ) {
      if ( map.length != 85 ) throw new IllegalArgumentException( "Base85 character map requires exactly 85 characters. Got " + map.length );
      for ( int i = 0 ; i < 83 ; i++ )
         for ( int j = i+1 ; j < 84 ; j++ )
            if ( map[i] == map[j] )
               throw new IllegalArgumentException( "Base85 character map must not contain duplicates");
   }

   private static Encoder RFC1924ENCODER, Z85ENCODER, ASCII85ENCODER;
   private static Decoder RFC1924DECODER, Z85DECODER, ASCII85DECODER;

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
   public static Encoder getAscii85Encoder() {
      if ( ASCII85ENCODER == null ) ASCII85ENCODER = new Ascii85Encoder();
      return ASCII85ENCODER;
   }
   public static Decoder getAscii85Decoder() {
      if ( ASCII85DECODER == null ) ASCII85DECODER = new Ascii85Decoder();
      return ASCII85DECODER;
   }

   public static void main( String[] args ) {
      checkCharacterMap( Ascii85Encoder.ENCODE_MAP );
      Encoder e = getAscii85Encoder();
      Decoder d = getAscii85Decoder();
      byte[] test = new byte[4];
      Arrays.fill( test, (byte)0 );
      System.out.println( e.encodeToString( test ) );
      System.out.println( d.decodeToBytes( "z" )[0] );
      /*
      System.out.println( e.encode( "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstu" ) );
      System.out.println( e.encode( "測試中" ) );
      System.out.println( e.encode( "اختبارات" ) );
      System.out.println( e.encode( "A" ) );
      System.out.println( e.encode( "AB" ) );
      System.out.println( e.encode( "ABC" ) );
      System.out.println( e.encode( "ABCD" ) );
      System.out.println( e.encode( "ABCDE" ) );
      System.out.println( e.encode( "ABCDEF" ) );
      System.out.println( e.encode( "ABCDEFG" ) );
      System.out.println( e.encode( "ABCDEFGH" ) );
      */
   }
}