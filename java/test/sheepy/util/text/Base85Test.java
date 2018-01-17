package sheepy.util.text;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;
import sheepy.util.text.Base85.Decoder;

public class Base85Test {
   private final Random rng = new Random();
   private final Base85.Encoder rfcE, z85E;
   private final Base85.Decoder rfcD, z85D;

   public Base85Test() {
      rfcE = Base85.getRfc1942Encoder();
      rfcD = Base85.getRfc1942Decoder();
      z85E = Base85.getZ85Encoder();
      z85D = Base85.getZ85Decoder();
   }

   /*
   @BeforeClass public static void setUpClass() {}
   @AfterClass public static void tearDownClass() {}
   @Before public void setUp() {}
   @After public void tearDown() {}
   */

   /////////// Common utils ///////////

   private byte[] reverseCharset( byte[] valids ) {
      List<Byte> invalidList = new ArrayList<>(256);
      for ( int i = Byte.MIN_VALUE ; i < Byte.MAX_VALUE ; i++ ) invalidList.add( (byte) i );
      for ( byte e : valids ) invalidList.remove( Byte.valueOf( e ) );
      final int len = invalidList.size();
      byte[] result = new byte[ len ];
      for ( int i = 0 ; i < len ; i++ ) result[i] = invalidList.get( i );
      return result;
   }

   private void recurTestInvalid ( byte[] ok, byte[] fail, byte[] buf, int offset, Decoder decoder ) {
      if ( offset >= buf.length ) return;
      if ( ( offset + 1 ) % 5 == 1 )  {
         buf[ offset ] = ok[ rng.nextInt( ok.length ) ];
         testDecodeFail( buf, offset, decoder ); // Wrong data length should fails
         recurTestInvalid( ok, fail, buf, offset + 1, decoder );
         return;
      }
      for ( int i = 0, len = fail.length ; i < len ; i++ ) {
         buf[ offset ] = fail[ i ]; // Wrong data should also fails
         testDecodeFail( buf, offset, decoder );
      }
      buf[ offset ] = ok[ rng.nextInt( ok.length ) ];
      decoder.decode( buf, 0, offset + 1 ); // Otherwise should pass
      recurTestInvalid( ok, fail, buf, offset + 1, decoder );
   }

   private void testDecodeFail ( byte[] buf, int len, Decoder decoder ) {
      len++;
      try {
         decoder.decode( buf, 0, len );
         fail( failMessage( buf, len, null ) );
      } catch ( IllegalArgumentException ignored ) {
      } catch ( Exception ex ) { fail( failMessage( buf, len, ex ) ); }
   }
   private String failMessage ( byte[] buf, int len, Exception ex ) {
      if ( ex != null ) ex.printStackTrace();
      return "atob(\"" + Base64.getEncoder().encodeToString( Arrays.copyOf( buf, len ) ) + "\") does not throw IllegalArgumentException on decode";
   }


   /////////// RFC Tests ///////////

   private final String[] rfcTests = {
      "", "",
      "A", "0%",
      "AB", "2Qk",
      "ABC", "6{`S",
      "ABCD", "K|(`B",
      "ABCDE", "K|(`B0*",
      "ABCDEF", "K|(`B2cs",
      "ABCDEFG", "K|(`B7XVe",
      "ABCDEFGH", "K|(`BMMg(R",
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~",
         "FflSSG&MFiI5|N=LqtVJM@UIZOH55pPf$@(Q&d$}S6EqEVPa!sWoBn+X=-b1ZEkOHadLBXb#`}nd3qruBqb&&DJm;1J3Ku;KR{kzV0(Oh1f",
      "測試中", "=D4irsix$(23",
      "اختبارات", "*r(X8*s9p5*r(XB*r(X4",
   };

   @Test public void testRfcSpec() throws UnknownHostException {
      byte[] addr = Inet6Address.getByName( "1080:0:0:0:8:800:200C:417A" ).getAddress();
      assertArrayEquals( "Inet round trip", addr, rfcD.decode( rfcE.encode( addr ) ) );
//      byte[] decoded = rfcD.decodeToBytes( "4)+k&C#VzJ4br>0wv%Yp" );
//      System.out.println( rfcE.encodeToString( addr ) );
//      for ( byte b : decoded )
//         System.out.print( Integer.toHexString( ( (int) b ) & 0xff ) );
   }

   @Test public void testRfcStrEncode() {
      for ( int i = 0 ; i < rfcTests.length ; i += 2 )
         assertEquals( "Encode " + rfcTests[i], rfcTests[i+1], rfcE.encode( rfcTests[i] ) );
   }
   @Test public void testRfcStrDecode() {
      for ( int i = 0 ; i < rfcTests.length ; i += 2 ) {
         assertEquals( "Decode " + rfcTests[i+1], rfcTests[i], rfcD.decode( rfcTests[i+1] ) );
         assertArrayEquals( "Decode " + rfcTests[i+1] + " to bytes", rfcTests[i].getBytes( UTF_8 ), rfcD.decode( rfcTests[i+1].getBytes( US_ASCII ) ) );
      }
   }
   @Test public void testRfcEncode() {
      String origStr = rfcTests[ rfcTests.length - 2 ], codeStr = rfcTests[ rfcTests.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertEquals( "encodeToString", codeStr, rfcE.encodeToString( orig ) );
      assertArrayEquals( "Byte to byte encode", code, rfcE.encode( orig ) );
      byte[] buf = Arrays.copyOf( orig, orig.length * 2 );
      assertArrayEquals( "Byte to byte encode offset 0", code, rfcE.encode( buf, 0, orig.length ) );
      System.arraycopy( buf, 0, buf, 2, orig.length );
      assertArrayEquals( "Byte to byte encode offset 2", code, rfcE.encode( buf, 2, orig.length ) );
      byte[] output = new byte[ code.length + 2 ];
      rfcE.encode( orig, 0, orig.length, output, 0 );
      assertArrayEquals( "Byte to byte direct encode offset 0", code, Arrays.copyOfRange( output, 0, code.length ) );
      rfcE.encode( buf, 2, orig.length, output, 2 );
      assertArrayEquals( "Byte to byte direct encode offset 2", code, Arrays.copyOfRange( output, 2, code.length + 2 ) );
   }
   @Test public void testRfcDecode() {
      String origStr = rfcTests[ rfcTests.length - 2 ], codeStr = rfcTests[ rfcTests.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertArrayEquals( "Byte to byte decode", orig, rfcD.decode( code ) );
      byte[] buf = Arrays.copyOf( code, code.length * 2 );
      assertArrayEquals( "Byte to byte decode offset 0", orig, rfcD.decode( buf, 0, code.length ) );
      System.arraycopy( buf, 0, buf, 2, code.length );
      assertArrayEquals( "Byte to byte decode offset 2", orig, rfcD.decode( buf, 2, code.length ) );
      byte[] output = new byte[ orig.length + 2 ];
      rfcD.decode( code, 0, code.length, output, 0 );
      assertArrayEquals( "Byte to byte direct decode offset 0", orig, Arrays.copyOfRange( output, 0, orig.length ) );
      rfcD.decode( buf, 2, code.length, output, 2 );
      assertArrayEquals( "Byte to byte direct decode offset 2", orig, Arrays.copyOfRange( output, 2, orig.length + 2 ) );
   }
   @Test public void testRfcRoundTrip() {
      for ( int len = 1 ; len <= 8 ; len++ ) {
         final byte[] from = new byte[ len ];
         for ( int v = Byte.MIN_VALUE ; v <= Byte.MAX_VALUE ; v++ ) {
            Arrays.fill( from, (byte) v );
            assertArrayEquals( "byte[" + len + "]{" + v + "} round trip.", from, rfcD.decode( rfcE.encode( from ) ) );
         }
      }
   }
   @Test public void testRfcDecodeErr() {
      byte[] validCodes = rfcE.getCharset().getBytes( US_ASCII );
      byte[] invalidCodes = reverseCharset( validCodes );
      recurTestInvalid( validCodes, invalidCodes, new byte[11], 0, rfcD );
   }

   @Test public void testRfcDecodedLen() {
      int[]  in = { 0, 2, 3, 4, 5, 7, 8, 9, 10, 200000, 200002 },
            out = { 0, 1, 2, 3, 4, 5, 6, 7,  8, 160000, 160001 };
      for ( int i = 0 ; i < in.length ; i++ )
         assertEquals( "Decoded length of " + in[i] + " chars.", out[i], rfcD.calcDecodedLength( null, 0, in[i] ) );
   }
   @Test(expected = IllegalArgumentException.class) public void testRfcDecodedLenErr6() {
      rfcD.calcDecodedLength( null, 0, 6 );
   }
   @Test(expected = IllegalArgumentException.class) public void testRfcDecodedLenErr1() {
      rfcD.calcDecodedLength( null, 0, 1 );
   }
   @Test public void testRfcEncodedLen() {
      int[]  in = { 0, 1, 2, 3, 4, 5, 6, 7,  8, 160000, 160001 },
            out = { 0, 2, 3, 4, 5, 7, 8, 9, 10, 200000, 200002 };
      for ( int i = 0 ; i < in.length ; i++ )
         assertEquals( "Encoded length of " + in[i] + " bytes.", out[i], rfcE.calcEncodedLength( null, 0, in[i] ) );
   }


   private final String[] z85Tests = {
      "", "",
      "A", "0+",
      "AB", "2qK",
      "ABC", "6@}s",
      "ABCD", "k%^}b",
      "ABCDE", "k%^}b0/",
      "ABCDEF", "k%^}b2CS",
      "ABCDEFG", "k%^}b7xvE",
      "ABCDEFGH", "k%^}bmmG^r",
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#",
         "fFLssg=mfIi5$zRv}od.xj#0]yIW<9z/xYpB98LFCx!yVk%^}bmmG^rnLhSHo?[FXqbQs(rArg6sZ0BfiX9v1aULrfcoKEudo*88ElY870z",
      "測試中", ">d4IRSIX:^23",
      "اختبارات", "/R^x8/S9P5/R^xb/R^x4",
   };

   @Test public void testZ85StrEncode() {
      for ( int i = 0 ; i < z85Tests.length ; i += 2 )
         assertEquals( "Encode " + z85Tests[i], z85Tests[i+1], z85E.encode( z85Tests[i] ) );
   }
   @Test public void testZ85StrDecode() {
      for ( int i = 0 ; i < z85Tests.length ; i += 2 ) {
         assertEquals( "Decode " + z85Tests[i+1], z85Tests[i], z85D.decode( z85Tests[i+1] ) );
         assertArrayEquals( "Decode " + z85Tests[i+1] + " to bytes", z85Tests[i].getBytes( UTF_8 ), z85D.decode( z85Tests[i+1].getBytes( US_ASCII ) ) );
      }
   }
   @Test public void testZ85Encode() {
      String origStr = z85Tests[ z85Tests.length - 2 ], codeStr = z85Tests[ rfcTests.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertEquals( "encodeToString", codeStr, z85E.encodeToString( orig ) );
      assertArrayEquals( "Byte to byte encode", code, z85E.encode( orig ) );
      byte[] buf = Arrays.copyOf( orig, orig.length * 2 );
      assertArrayEquals( "Byte to byte encode offset 0", code, z85E.encode( buf, 0, orig.length ) );
      System.arraycopy( buf, 0, buf, 2, orig.length );
      assertArrayEquals( "Byte to byte encode offset 2", code, z85E.encode( buf, 2, orig.length ) );
      byte[] output = new byte[ code.length + 2 ];
      z85E.encode( orig, 0, orig.length, output, 0 );
      assertArrayEquals( "Byte to byte direct encode offset 0", code, Arrays.copyOfRange( output, 0, code.length ) );
      z85E.encode( buf, 2, orig.length, output, 2 );
      assertArrayEquals( "Byte to byte direct encode offset 2", code, Arrays.copyOfRange( output, 2, code.length + 2 ) );
   }
   @Test public void testZ85Decode() {
      String origStr = z85Tests[ z85Tests.length - 2 ], codeStr = z85Tests[ z85Tests.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertArrayEquals( "Byte to byte decode", orig, z85D.decode( code ) );
      byte[] buf = Arrays.copyOf( code, code.length * 2 );
      assertArrayEquals( "Byte to byte decode offset 0", orig, z85D.decode( buf, 0, code.length ) );
      System.arraycopy( buf, 0, buf, 2, code.length );
      assertArrayEquals( "Byte to byte decode offset 2", orig, z85D.decode( buf, 2, code.length ) );
      byte[] output = new byte[ orig.length + 2 ];
      z85D.decode( code, 0, code.length, output, 0 );
      assertArrayEquals( "Byte to byte direct decode offset 0", orig, Arrays.copyOfRange( output, 0, orig.length ) );
      z85D.decode( buf, 2, code.length, output, 2 );
      assertArrayEquals( "Byte to byte direct decode offset 2", orig, Arrays.copyOfRange( output, 2, orig.length + 2 ) );
   }
   @Test public void testZ85RoundTrip() {
      for ( int len = 1 ; len <= 8 ; len++ ) {
         final byte[] from = new byte[ len ];
         for ( int v = Byte.MIN_VALUE ; v <= Byte.MAX_VALUE ; v++ ) {
            Arrays.fill( from, (byte) v );
            assertArrayEquals( "byte[" + len + "]{" + v + "} round trip.", from, z85D.decode( z85E.encode( from ) ) );
         }
      }
   }
   @Test public void testZ85DecodeErr() {
      byte[] validCodes = z85E.getCharset().getBytes( US_ASCII );
      byte[] invalidCodes = reverseCharset( validCodes );
      recurTestInvalid( validCodes, invalidCodes, new byte[11], 0, z85D );
   }

   @Test public void testZ85DecodedLen() {
      int[]  in = { 0, 2, 3, 4, 5, 7, 8, 9, 10, 200000, 200002 },
            out = { 0, 1, 2, 3, 4, 5, 6, 7,  8, 160000, 160001 };
      for ( int i = 0 ; i < in.length ; i++ )
         assertEquals( "Decoded length of " + in[i] + " chars.", out[i], z85D.calcDecodedLength( null, 0, in[i] ) );
   }
   @Test(expected = IllegalArgumentException.class) public void testZ85DecodedLenErr6() {
      z85D.calcDecodedLength( null, 0, 6 );
   }
   @Test(expected = IllegalArgumentException.class) public void testZ85DecodedLenErr1() {
      z85D.calcDecodedLength( null, 0, 1 );
   }
   @Test public void testZ85EncodedLen() {
      int[]  in = { 0, 1, 2, 3, 4, 5, 6, 7,  8, 160000, 160001 },
            out = { 0, 2, 3, 4, 5, 7, 8, 9, 10, 200000, 200002 };
      for ( int i = 0 ; i < in.length ; i++ )
         assertEquals( "Encoded length of " + in[i] + " bytes.", out[i], z85E.calcEncodedLength( null, 0, in[i] ) );
   }
   @Test public void testZ85Spec() {
      byte[] helloWorld = new byte[]{ (byte)0x86, (byte)0x4F, (byte)0xD2, (byte)0x6F, (byte)0xB5, (byte)0x59, (byte)0xF7, (byte)0x5B };
      assertArrayEquals( "HelloWorld decode", helloWorld, z85D.decodeToBytes( "HelloWorld" ) );
   }
}