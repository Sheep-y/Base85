import Base85 from './base85.js';

window.Base85 = Base85; // For browser console debug

const rfcE = Base85.getRfc1942Encoder();
//const rfcD = Base85.getRfc1942Decoder();
const z85E = Base85.getZ85Encoder();
//const z85D = Base85.getZ85Decoder();
const a85E = Base85.getAscii85Encoder();
//const a85D = Base85.getAscii85Decoder();

/////////// Generic Test Routines ///////////

function StringToByte ( data ) { return new TextEncoder().encode( data ); }

function testStrEncode ( assert, e, map ) {
   for ( let i = 0 ; i < map.length ; i += 2 )
      assert.equal( map[i+1], e.encode( map[i] ), "Encode " + map[i] );
}

function testStrDecode ( assert, d, map ) {
   for ( let i = 0 ; i < map.length ; i += 2 ) {
      assert.equal( map[i], d.decode( map[i+1] ), "Decode " + map[i+1] );
      assert.propEqual( map[i].getBytes( UTF_8 ), d.decode( map[i+1].getBytes( US_ASCII ) ), "Decode " + map[i+1] + " to bytes" );
   }
}

function testByteEncode ( assert, e, map ) {
   const origStr = map[ map.length - 2 ], codeStr = map[ map.length - 1 ];
   let orig = StringToByte( origStr ), code = StringToByte( codeStr );
   assert.equal( codeStr, e.encodeToString( orig ), "encodeToString" );
   assert.propEqual( code, e.encode( orig ), "Byte to byte encode" );
   let buf = new Uint8Array( orig.length * 2 );
   buf.set( orig );
   assert.propEqual( code, e.encode( buf.slice( 0, orig.length ) ), "Byte to byte encode offset 0" );
   buf.copyWithin( 2, 0, orig.length );
   assert.propEqual( code, e.encode( buf.slice( 2, orig.length + 2 ) ), "Byte to byte encode offset 2" );
   let output = new Uint8Array( code.length + 2 );
   e.encode( orig, output );
   assert.propEqual( code, output.slice( 0, -2 ), "Byte to byte direct encode offset 0" );
   e.encode( buf.slice( 2, orig.length + 2 ), new DataView( output.buffer, 2 ) );
   assert.propEqual( code, output.slice( 2 ), "Byte to byte direct encode offset 2" );
}

/////////// RFC Tests ///////////

const rfcTests = [
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
];

QUnit.module( "RFC 1942" );
QUnit.test( "RfcSpec", function ( assert ) {
   if ( ! window.BigInt ) return;
   const addr = Uint8Array.from( [16, 128, 0, 0, 0, 0, 0, 0, 0, 8, 8, 0, 32, 12, 65, 122] );
   const encoded = "4)+k&C#VzJ4br>0wv%Yp";
   assert.equal( encoded, new TextDecoder( 'ascii' ).decode( rfcE.encodeBlockReverse( addr )[0] ), "Inet encode" );
   //assert.propEqual( addr, rfcD.decodeBlockReverse( encoded.getBytes( US_ASCII ), "Inet encode" ) );
} );

QUnit.test( "RfcStrEncode", function( assert ) { testStrEncode( assert, rfcE, rfcTests ); } );
//QUnit.test( "RfcStrDecode", function( assert ) { testStrDecode( assert, rfcD, rfcTests ); } );
QUnit.test( "RfcEncode", function( assert ) { testByteEncode( assert, rfcE, rfcTests ); } );
//QUnit.test( "RfcDecode", function( assert ) { testByteDecode( assert, rfcD, rfcTests ); } );
//QUnit.test( "RfcRoundTrip", function( assert ) { testRoundTrip( assert, rfcE, rfcD ); } );
//QUnit.test( "RfcWrongData", function( assert ) { testInvalidData( assert, rfcE, rfcD ); } );
//QUnit.test( "RfcWrongLength", function( assert ) { testInvalidLength( assert, rfcE, rfcD ); } );


/////////// Z85 Tests ///////////

const z85Tests = [
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
];

QUnit.module( "Z85" );
QUnit.test( "Z85Spec", function ( assert ) {
   const helloWorld = Uint8Array.of( 0x86, 0x4F, 0xD2, 0x6F, 0xB5, 0x59, 0xF7, 0x5B );
   assert.equal( "HelloWorld", z85E.encodeToString( helloWorld ), "HelloWorld encode" );
//   assert.propEqual( helloWorld, z85D.decodeToBytes( "HelloWorld" ), "HelloWorld decode" );
} );


QUnit.test( "Z85StrEncode", function( assert ) { testStrEncode( assert, z85E, z85Tests ); } );
//QUnit.test( "Z85StrDecode", function( assert ) { testStrDecode( assert, z85D, z85Tests ); } );
QUnit.test( "Z85Encode", function( assert ) { testByteEncode( assert, z85E, z85Tests ); } );
//QUnit.test( "Z85Decode", function( assert ) { testByteDecode( assert, z85D, z85Tests ); } );
//QUnit.test( "Z85RoundTrip", function( assert ) { testRoundTrip( assert, z85E, z85D ); } );
//QUnit.test( "Z85WrongData", function( assert ) { testInvalidData( assert, z85E, z85D ); } );
//QUnit.test( "Z85WrongLength", function( assert ) { testInvalidLength( assert, z85E, z85D ); } );



/////////// Ascii85 Tests ///////////

const a85Tests = [
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
];

QUnit.module( "Ascii85" );
QUnit.test( "A85Spec", function( assert ) {
   const from = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
   const to = "9jqo^BlbD-BleB1DJ+*+F(f,q/0JhKF<GL>Cj@.4Gp$d7F!,L7@<6@)/0JDEF<G%<+EV:2F!,"+
              "O<DJ+*.@<*K0@<6L(Df-\\0Ec5e;DffZ(EZee.Bl.9pF\"AGXBPCsi+DGm>@3BB/F*&OCAfu2/AKY"+
              "i(DIb:@FD,*)+C]U=@3BN#EcYf8ATD3s@q?d$AftVqCh[NqF<G:8+EV:.+Cf>-FD5W8ARlolDIa" +
              "l(DId<j@<?3r@:F%a+D58'ATD4$Bl@l3De:,-DJs`8ARoFb/0JMK@qB4^F!,R<AKZ&-DfTqBG%G" +
              ">uD.RTpAKYo'+CT/5+Cei#DII?(E,9)oF*2M7/c";
   assert.propEqual( to, a85E.encode( from ), "Leviathan encode" );
} );

QUnit.test( "A85StrEncode", function( assert ) { testStrEncode( assert, a85E, a85Tests ); } );
//QUnit.test( "A85StrDecode", function( assert ) { testStrDecode( assert, a85D, a85Tests ); } );
QUnit.test( "A85Encode", function( assert ) { testByteEncode( assert, a85E, a85Tests ); } );
//QUnit.test( "A85Decode", function( assert ) { testByteDecode( assert, a85D, a85Tests ); } );
//QUnit.test( "A85RoundTrip", function( assert ) { testRoundTrip( assert, a85E, a85D ); } );
//QUnit.test( "A85WrongData", function( assert ) {
//   testInvalidData( assert, a85E, a85D );
//   assert.notOk( a85D.test( "ya" ), "Ascii85 test \"ya\" should fail" );
//   assert.notOk( a85D.test( "zya" ), "Ascii85 test \"ya\" should fail" );
//} );
//QUnit.test( "A85WrongLength", function( assert ) { testInvalidLength( assert, a85E, a85D ); } );
