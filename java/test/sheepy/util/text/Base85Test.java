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
   private final Base85.Encoder rfcE, z85E, a85E;
   private final Base85.Decoder rfcD, z85D, a85D;

   public Base85Test() {
      rfcE = Base85.getRfc1924Encoder();
      rfcD = Base85.getRfc1924Decoder();
      z85E = Base85.getZ85Encoder();
      z85D = Base85.getZ85Decoder();
      a85E = Base85.getAscii85Encoder();
      a85D = Base85.getAscii85Decoder();
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

   private void recurTestValidate ( byte[] ok, byte[] fail, byte[] buf, int offset, Decoder decoder ) {
      if ( offset >= buf.length ) return;
      int count = offset + 1, oklen = ok.length;
      if ( count % 5 == 1 )  {
         buf[ offset ] = ok[ rng.nextInt( oklen ) ];
         testValidateFail( buf, count, decoder );
         recurTestValidate( ok, fail, buf, count, decoder );
         return;
      }
      for ( int i = 0, len = fail.length ; i < len ; i++ ) {
         buf[ offset ] = fail[ i ]; // Wrong data should also fails
         testValidateFail( buf, offset + 1, decoder );
      }
      buf[ offset ] = ok[ rng.nextInt( oklen ) ];
      if ( ! decoder.test( buf, 0, count ) ) // Otherwise should pass
         fail( randData( buf, count ) + " should not return false" );
      recurTestValidate( ok, fail, buf, count, decoder );
   }

   private void testValidateFail ( byte[] buf, int len, Decoder decoder ) {
      if ( decoder.test( buf, 0, len ) )
         fail( "test( " + randData( buf, len ) + ") should not return true" );
   }
   private String randData ( byte[] buf, int len ) {
      return "atob(\"" + Base64.getEncoder().encodeToString( Arrays.copyOf( buf, len ) ) + "\")";
   }

   private void testException ( Runnable action, String testName ) { testException( action, Exception.class, testName ); }
   private void testException ( Runnable action, Class exceptionClass, String testName ) {
      try {
         action.run();
         fail( testName + " does not throw any exception" );
      } catch ( Exception yes ) {
         if ( ! exceptionClass.isInstance( yes ) )
            fail( testName + " throws " + yes.getClass() + ", expected " + exceptionClass );
      }
   }

   /////////// Generic Test Routines ///////////

   public void testStrEncode ( Base85.Encoder e, String[] map ) {
      for ( int i = 0 ; i < map.length ; i += 2 )
         assertEquals( "Encode " + map[i], map[i+1], e.encode( map[i] ) );
   }

   public void testStrDecode ( Base85.Decoder d, String[] map ) {
      for ( int i = 0 ; i < map.length ; i += 2 ) {
         assertEquals( "Decode " + map[i+1], map[i], d.decode( map[i+1] ) );
         assertArrayEquals( "Decode " + map[i+1] + " to bytes", map[i].getBytes( UTF_8 ), d.decode( map[i+1].getBytes( US_ASCII ) ) );
      }
   }

   public void testByteEncode ( Base85.Encoder e, String[] map ) {
      String origStr = map[ map.length - 2 ], codeStr = map[ map.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertEquals( "encodeToString", codeStr, e.encodeToString( orig ) );
      assertArrayEquals( "Byte to byte encode", code, e.encode( orig ) );
      byte[] buf = Arrays.copyOf( orig, orig.length * 2 );
      assertArrayEquals( "Byte to byte encode offset 0", code, e.encode( buf, 0, orig.length ) );
      System.arraycopy( buf, 0, buf, 2, orig.length );
      assertArrayEquals( "Byte to byte encode offset 2", code, e.encode( buf, 2, orig.length ) );
      byte[] output = new byte[ code.length + 2 ];
      e.encode( orig, 0, orig.length, output, 0 );
      assertArrayEquals( "Byte to byte direct encode offset 0", code, Arrays.copyOfRange( output, 0, code.length ) );
      e.encode( buf, 2, orig.length, output, 2 );
      assertArrayEquals( "Byte to byte direct encode offset 2", code, Arrays.copyOfRange( output, 2, code.length + 2 ) );
   }

   public void testByteDecode ( Base85.Decoder d, String[] map ) {
      String origStr = map[ map.length - 2 ], codeStr = map[ map.length - 1 ];
      byte[] orig = origStr.getBytes( UTF_8 ), code = codeStr.getBytes( US_ASCII );
      assertArrayEquals( "Byte to byte decode", orig, d.decode( code ) );
      byte[] buf = Arrays.copyOf( code, code.length * 2 );
      assertArrayEquals( "Byte to byte decode offset 0", orig, d.decode( buf, 0, code.length ) );
      System.arraycopy( buf, 0, buf, 2, code.length );
      assertArrayEquals( "Byte to byte decode offset 2", orig, d.decode( buf, 2, code.length ) );
      byte[] output = new byte[ orig.length + 2 ];
      d.decode( code, 0, code.length, output, 0 );
      assertArrayEquals( "Byte to byte direct decode offset 0", orig, Arrays.copyOfRange( output, 0, orig.length ) );
      d.decode( buf, 2, code.length, output, 2 );
      assertArrayEquals( "Byte to byte direct decode offset 2", orig, Arrays.copyOfRange( output, 2, orig.length + 2 ) );
   }

   public void testRoundTrip ( Base85.Encoder e, Base85.Decoder d ) {
      for ( int len = 1 ; len <= 12 ; len++ ) {
         byte[] from = new byte[ len ], enc, dec;
         for ( int v = Byte.MIN_VALUE ; v <= Byte.MAX_VALUE ; v++ ) {
            Arrays.fill( from, (byte) v );
            String test = "byte[" + len + "]{" + v + "} ";
            try {
               enc = e.encode( from );
               assertTrue( test + " encoded data test.", d.test( enc ) );
               assertEquals( test + " encoded length", enc.length, e.calcEncodedLength( from ) );
               dec = d.decode( enc );
               assertEquals( test + " decoded length", dec.length, d.calcDecodedLength( enc ) );
               assertArrayEquals( test + " round trip.", from, dec );
            } catch ( Exception ex ) {
               fail( test + " round trip throws " + ex );
            };
         }
      }
   }

   public void testInvalidLength ( Base85.Encoder e, Base85.Decoder d ) {
      byte[] buf = new byte[4];
      Arrays.fill( buf, e.getEncodeMap()[0] ); // Fill in valid values in case an decode actually tries to read it
      testException( () -> e.encode( buf, -1, 4 ), "Encode in offset -1" );
      testException( () -> e.encode( buf, 4, 1 ), "Encode in offset > size" );
      testException( () -> e.encode( buf, 0, -1 ), "Encode in length -1" );
      testException( () -> e.encode( buf, 0, 5 ), "Encode in length > size" );
      testException( () -> e.encode( buf, 2, 4 ), "Encode in index out of bounds" );
      testException( () -> e.encode( buf, 0, 1, buf, -1 ), "Encode out index -1" );
      testException( () -> e.encode( buf, 0, 1, buf, 4 ), "Encode out index > size" );
      testException( () -> e.encode( buf, 0, 1, buf, 3 ), "Encode out index out of bounds" );
      testException( () -> d.decode( buf, -1, 4 ), "Decode in offset -1" );
      testException( () -> d.decode( buf, 4, 1 ), "Decode in offset > size" );
      testException( () -> d.decode( buf, 0, -1 ), "Decode in length -1" );
      testException( () -> d.decode( buf, 0, 5 ), "Decode in length > size" );
      testException( () -> d.decode( buf, 2, 4 ), "Decode in index out of bounds" );
      testException( () -> d.decode( buf, 0, 1, buf, -1 ), "Decode out index -1" );
      testException( () -> d.decode( buf, 0, 1, buf, 4 ), "Decode out index > size" );
      testException( () -> d.decode( buf, 0, 1, buf, 3 ), "Decode out index out of bounds" );
   }

   public void testInvalidData ( Base85.Encoder e, Base85.Decoder d ) {
      testException( () -> d.decode( new byte[]{ 127, 127 } ), IllegalArgumentException.class, "Decode char(127)" );
      testException( () -> d.decode( new byte[]{ -1, -1 } ), IllegalArgumentException.class, "Decode char(-1)" );
      byte[] validCodes = e.getCharset().getBytes( US_ASCII );
      byte[] invalidCodes = reverseCharset( validCodes );
      recurTestValidate( validCodes, invalidCodes, new byte[11], 0, d );
   }

   /////////// RFC Tests ///////////

   private final String[] rfcTests = {
      "", "",
      "A", "K>",
      "AB", "K|%",
      "ABC", "K|(_",
      "ABCD", "K|(`B",
      "ABCDE", "K|(`BMF",
      "ABCDEF", "K|(`BMMe",
      "ABCDEFG", "K|(`BMMg&",
      "ABCDEFGH", "K|(`BMMg(R",
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~",
         "FflSSG&MFiI5|N=LqtVJM@UIZOH55pPf$@(Q&d$}S6EqEVPa!sWoBn+X=-b1ZEkOHadLBXb#`}nd3qruBqb&&DJm;1J3Ku;KR{kzV0(Oheg",
      "測試中", "=D4irsix$(tp",
      "اختبارات", "*r(X8*s9p5*r(XB*r(X4",
   };

   @Test public void testRfcSpec() throws UnknownHostException {
      byte[] addr = Inet6Address.getByName( "1080:0:0:0:8:800:200C:417A" ).getAddress();
      String encoded = "4)+k&C#VzJ4br>0wv%Yp";
      assertEquals( "Inet encode", encoded, new String( rfcE.encodeBlockReverse( addr ), US_ASCII ) );
      assertArrayEquals( "Inet decode", addr, rfcD.decodeBlockReverse( encoded.getBytes( US_ASCII ) ) );
   }

   @Test public void testRfcStrEncode() { testStrEncode( rfcE, rfcTests ); }
   @Test public void testRfcStrDecode() { testStrDecode( rfcD, rfcTests ); }
   @Test public void testRfcEncode() { testByteEncode( rfcE, rfcTests ); }
   @Test public void testRfcDecode() { testByteDecode( rfcD, rfcTests ); }
   @Test public void testRfcRoundTrip() { testRoundTrip( rfcE, rfcD ); }
   @Test public void testRfcWrongData() { testInvalidData( rfcE, rfcD ); }
   @Test public void testRfcWrongLength() { testInvalidLength( rfcE, rfcD ); }

   @Test(expected = IllegalArgumentException.class) public void testRfcDecodedLenErr6() {
      rfcD.calcDecodedLength( null, 0, 6 );
   }
   @Test(expected = IllegalArgumentException.class) public void testRfcDecodedLenErr1() {
      rfcD.calcDecodedLength( null, 0, 1 );
   }


   private final String[] z85Tests = {
      "", "",
      "A", "k(",
      "AB", "k%+",
      "ABC", "k%^{",
      "ABCD", "k%^}b",
      "ABCDE", "k%^}bmf",
      "ABCDEF", "k%^}bmmE",
      "ABCDEFG", "k%^}bmmG=",
      "ABCDEFGH", "k%^}bmmG^r",
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#",
         "fFLssg=mfIi5$zRv}od.xj#0]yIW<9z/xYpB98LFCx!yVk%^}bmmG^rnLhSHo?[FXqbQs(rArg6sZ0BfiX9v1aULrfcoKEudo*88ElY87bl",
      "測試中", ">d4IRSIX:^TP",
      "اختبارات", "/R^x8/S9P5/R^xb/R^x4",
   };

   @Test public void testZ85Spec() {
      byte[] helloWorld = new byte[]{ (byte)0x86, (byte)0x4F, (byte)0xD2, (byte)0x6F, (byte)0xB5, (byte)0x59, (byte)0xF7, (byte)0x5B };
      assertEquals( "HelloWorld encode", "HelloWorld", z85E.encodeToString( helloWorld ) );
      assertArrayEquals( "HelloWorld decode", helloWorld, z85D.decodeToBytes( "HelloWorld" ) );
   }

   @Test public void testZ85StrEncode() { testStrEncode( z85E, z85Tests ); }
   @Test public void testZ85StrDecode() { testStrDecode( z85D, z85Tests ); }
   @Test public void testZ85Encode() { testByteEncode( z85E, z85Tests ); }
   @Test public void testZ85Decode() { testByteDecode( z85D, z85Tests ); }
   @Test public void testZ85RoundTrip() { testRoundTrip( z85E, z85D ); }
   @Test public void testZ85WrongData() { testInvalidData( z85E, z85D ); }
   @Test public void testZ85WrongLength() { testInvalidLength( z85E, z85D ); }

   @Test(expected = IllegalArgumentException.class) public void testZ85DecodedLenErr6() {
      z85D.calcDecodedLength( null, 0, 6 );
   }
   @Test(expected = IllegalArgumentException.class) public void testZ85DecodedLenErr1() {
      z85D.calcDecodedLength( null, 0, 1 );
   }


   private final String[] A85Tests = {
      "", "",
      "A", "5l",
      "AB", "5sb",
      "ABC", "5sdp",
      "ABCD", "5sdq,",
      "ABCDE", "5sdq,70",
      "ABCDEF", "5sdq,77I",
      "ABCDEFG", "5sdq,77Kc",
      "ABCDEFGH", "5sdq,77Kd<",
      "\u0000\u0000\u0000\u0000", "z",
      "    ", "y",
      "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~",
         "+X/-V,pjuf.4Qi!/M8\\10etOA2)[BQ3BB5a4[)(q5sdq,77Kd<8P2WL9hnJ\\;,U=l<E<1'=^#$7?!^lG@:E_WAS,RgBkhF\"D/O92EH6,BF`qtRH$XgbI=;",
      "測試中", "k.%MVWM\\adXT",
      "اختبارات", "fVdB)fW*T&fVdB,fVdB%",
   };
   @Test public void testAscii85Spec() {
      String from = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
      String to = "9jqo^BlbD-BleB1DJ+*+F(f,q/0JhKF<GL>Cj@.4Gp$d7F!,L7@<6@)/0JDEF<G%<+EV:2F!,"+
                  "O<DJ+*.@<*K0@<6L(Df-\\0Ec5e;DffZ(EZee.Bl.9pF\"AGXBPCsi+DGm>@3BB/F*&OCAfu2/AKY"+
                  "i(DIb:@FD,*)+C]U=@3BN#EcYf8ATD3s@q?d$AftVqCh[NqF<G:8+EV:.+Cf>-FD5W8ARlolDIa" +
                  "l(DId<j@<?3r@:F%a+D58'ATD4$Bl@l3De:,-DJs`8ARoFb/0JMK@qB4^F!,R<AKZ&-DfTqBG%G" +
                  ">uD.RTpAKYo'+CT/5+Cei#DII?(E,9)oF*2M7/c";
      assertEquals( "Leviathan encode", to, a85E.encode( from ) );
      assertEquals( "Leviathan decode", from, a85D.decode( to ) );
   }
   @Test public void testA85StrEncode() { testStrEncode( a85E, A85Tests ); }
   @Test public void testA85StrDecode() { testStrDecode( a85D, A85Tests ); }
   @Test public void testA85Encode() { testByteEncode( a85E, A85Tests ); }
   @Test public void testA85Decode() { testByteDecode( a85D, A85Tests ); }
   @Test public void testA85RoundTrip() { testRoundTrip( a85E, a85D ); }
   @Test public void testA85WrongData() {
      testInvalidData( a85E, a85D );
      assertFalse( "Ascii85 test \"ya\" should fail", a85D.test( "ya" ) );
      assertFalse( "Ascii85 test \"zya\" should fail", a85D.test( "zya" ) );
   }
   @Test public void testA85WrongLength() { testInvalidLength( a85E, a85D ); }

}