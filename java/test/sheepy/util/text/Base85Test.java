package sheepy.util.text;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

public class Base85Test {
   private final Base85.Encoder rfcE;
   private final Base85.Decoder rfcD;

   public Base85Test() {
      rfcE = Base85.getRfc1942Encoder();
      rfcD = Base85.getRfc1942Decoder();
   }

   /*
   @BeforeClass public static void setUpClass() {}
   @AfterClass public static void tearDownClass() {}
   @Before public void setUp() {}
   @After public void tearDown() {}
   */

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

   @Test public void testRfcStrEncode() {
      for ( int i = 0 ; i < rfcTests.length ; i += 2 ) try {
         assertEquals( "Encode " + rfcTests[i], rfcTests[i+1], rfcE.encode( rfcTests[i] ) );
      } catch ( Exception ex ) {
         ex.printStackTrace();
         fail( "Error when encoding " + rfcTests[i] + ": " + ex.getMessage() );
      }
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

   @Test(expected = IllegalArgumentException.class) public void testRfcDecodeErr() {
      rfcD.decode( "\uffff" );
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
}