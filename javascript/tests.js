import Base85 from './base85.js';

window.Base85 = Base85; // For browser console debug

const rfcE = Base85.getRfc1942Encoder();
//const rfcD = Base85.getRfc1942Decoder();
const z85E = Base85.getZ85Encoder();
//const z85D = Base85.getZ85Decoder();
//const a85E = Base85.getAscii85Encoder();
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

/*
QUnit.test( "RfcSpec", function ( assert ) {
   byte[] addr = Inet6Address.getByName( "1080:0:0:0:8:800:200C:417A" ).getAddress();
   String encoded = "4)+k&C#VzJ4br>0wv%Yp";
   assert.equal( encoded, new String( rfcE.encodeBlockReverse( addr ), US_ASCII ), "Inet encode" );
   assert.propEqual( addr, rfcD.decodeBlockReverse( encoded.getBytes( US_ASCII ), "Inet encode" ) );
} );
*/

QUnit.test( "RfcStrEncode", function( assert ) { testStrEncode( assert, rfcE, rfcTests ); } );
//QUnit.test( "RfcStrDecode", function( assert ) { testStrDecode( assert, rfcD, rfcTests ); } );
QUnit.test( "RfcEncode", function( assert ) { testByteEncode( assert, rfcE, rfcTests ); } );
//QUnit.test( "RfcDecode", function( assert ) { testByteDecode( assert, rfcD, rfcTests ); } );
//QUnit.test( "RfcRoundTrip", function( assert ) { testRoundTrip( assert, rfcE, rfcD ); } );
//QUnit.test( "RfcWrongData", function( assert ) { testInvalidData( assert, rfcE, rfcD ); } );
//QUnit.test( "RfcWrongLength", function( assert ) { testInvalidLength( assert, rfcE, rfcD ); } );
