console.log( "Initialising Base85" );

// Constants used in encoding and decoding
const Power4 = 52200625; // 85^4
const Power3 = 614125;  // 85^3
const Power2 = 7225;   // 85^2

const TypedArrayType = Object.getPrototypeOf( Uint8Array );

// Convert all compatible input to DataView
function MakeInput ( data ) {
   if ( data instanceof DataView )
      return data;
   if ( typeof( data ) === 'string' )
      data = new TextEncoder().encode( data ); // If string, encode as utf-8 and later convert to DataView below
   if ( data instanceof TypedArrayType )
      return new DataView( data.buffer );
   if ( data instanceof ArrayBuffer )
      return new DataView( data );
   throw new TypeError( 'Unsupported Base85 input: must be string, TypedArray, DataView, or ArrayBuffer.' );
}

// Convert all compatible output to Uint8Array
function MakeOutput ( data, size ) {
   if ( ! data ) data = new Uint8Array( size() );
   if ( data instanceof Uint8Array )
      return data;
   if ( data instanceof TypedArrayType )
      return new Uint8Array( data.buffer );
   if ( data instanceof DataView )
      return new Uint8Array( data.buffer, data.byteOffset, data.byteLength );
   if ( data instanceof ArrayBuffer || data instanceof SharedArrayBufferType )
      return new Uint8Array( data );
   throw new TypeError( 'Unsupported Base85 output: must be TypedArray, DataView, or ArrayBuffer' );
}

function BufToText ( data ) {
   // data must be Uint8Array
   return new TextDecoder( 'utf-8' ).decode( data );
}

function BufToCode ( data ) {
   // data must be string or Uint8Array
   if ( typeof( data ) === 'string' ) return data;
   return new TextDecoder( 'ascii' ).decode( data );
}

function StringToMap ( data ) {
   // data must be string, return Uint8Array map
   return new TextEncoder().encode( data );
}

function CreateClass( baseObject, constructor ) {
   if ( constructor == undefined ) constructor = function(){};
   constructor.prototype = baseObject;
   Object.assign( constructor, baseObject );
   Object.setPrototypeOf( constructor, baseObject.__proto__ );
   return constructor;
}

/** This is a skeleton class for encoding data using the Base85 encoding scheme,
  * in the same style as Base64 encoder.
  * Encoder instances can be safely shared by multiple threads.
  */
export const Base85Encoder = {

   /** Calculate the max encoded byte length of a given input data. */
   calcEncodedLength ( data ) {
      data = MakeInput( data );
      return ~~Math.ceil( data.byteLength * 1.25 );
   },

   /** Encode data.
    * 1. If a String is supplied, a Base85 string of its utf-8 data will be returned.
    * 2. If a TypedArray is supplied, a TypedArray of the Base85 data will be returned.
    * 3. A TypedArray output buffer can also be supplied, in which case the number of Base85 data will be returned regardless of input type.
    */
   encode ( data, out ) {
      const inputIsString = typeof( data ) === 'string', outputIsProveded = out ? true : false;
      data = MakeInput( data );
      out = MakeOutput( out, () => this.calcEncodedLength( data ) );
      const len = this._encode( data, out );
      if ( outputIsProveded ) return len;
      if ( len !== out.length ) out = out.slice( 0, len );
      if ( inputIsString ) return BufToCode( out );
      return out;
   },

   /** Encode data into Base85 string. */
   encodeToString ( data ) {
      return BufToCode( this.encode( data ) );
   },

   /** Encode part of data as one block in reverse output order into output array.
     * This is the strict algorithm specified by RFC 1924 for IP address encoding,
     * when the data part is exactly 16 bytes (128 bits) long.
     * Because the whole input data part is encoded as one big block,
     * this is much less efficient than the more common encodings.
     * Output is [ Uint8Array result, int encoded_byte_count ]
     */
   encodeBlockReverse ( data, out ) {
      data = MakeInput( data );
      const size = this.calcEncodedLength( data ), map = this.getEncodeMap();
      out = MakeOutput( out, () => size );
      let blockSum = BigInt( 0 ), b85 = BigInt( 85 ), b8 = BigInt( 8 );
      for ( let i = data.byteOffset, len = data.byteLength ; i < len ; i++ )
         blockSum = ( blockSum << b8 ) + BigInt( data.getUint8( i ) );
      for ( let i = size - 1 ; i >= 0 ; i-- ) {
         const mod = blockSum % b85;
         blockSum = ( blockSum - mod ) / b85;
         out[ i ] = map[ mod ];
      }
      return [ out, size ];
   },

   _encode () { throw new TypeError( '_encode() is not implemented by Base85Encoder' ); },
   getEncodeMap () { return this.ENCODE_MAP; },
   getCharset() { return BufToCode( this.getEncodeMap() ); },
};



/* An encoder that does not support compression */
export const Base85SimpleEncoder = { __proto__ : Base85Encoder,

   _encodeDangling ( encodeMap, out, wi, buf, leftover ) {
      let sum = new DataView( buf.buffer ).getUint32();
      out[wi  ] = encodeMap[ parseInt( sum / Power4 ) ]; sum %= Power4;
      out[wi+1] = encodeMap[ parseInt( sum / Power3 ) ]; sum %= Power3;
      if ( leftover >= 2 ) {
         out[wi+2] = encodeMap[ parseInt( sum / Power2 ) ]; sum %= Power2;
         if ( leftover >= 3 )
            out[wi+3] = encodeMap[ parseInt( sum / 85 ) ];
      }
      return leftover + 1;
   },

   _encode ( data, out ) {
      const rlen = data.byteLength;
      if ( rlen <= 0 ) return 0;
      const encodeMap = this.getEncodeMap(), leftover = rlen % 4;
      let ri = data.byteOffset, wi = 0;
      for ( let loop = parseInt( rlen / 4 ) ; loop > 0 ; loop--, ri += 4  )
         wi = this._writeData( data.getUint32( ri ), encodeMap, out, wi );
      if ( leftover == 0 ) return wi;
      const buf = new Uint8Array( 4 );
      buf.set( new Uint8Array( data.buffer.slice( ri ) ) );
      return wi + this._encodeDangling( encodeMap, out, wi, buf, leftover );
   },

   _writeData ( sum, map, out, wi ) {
      out[wi  ] = map[ parseInt( sum / Power4 ) ]; sum %= Power4;
      out[wi+1] = map[ parseInt( sum / Power3 ) ]; sum %= Power3;
      out[wi+2] = map[ parseInt( sum / Power2 ) ]; sum %= Power2;
      out[wi+3] = map[ parseInt( sum / 85 ) ];
      out[wi+4] = map[ sum % 85 ];
      return wi + 5;
   },
};


/** This class encodes data in the Base85 encoding scheme using the character set described by IETF RFC 1924,
  * but in the efficient algorithm of Ascii85 and Z85.
  * This scheme does not use quotes, comma, or slash, and can usually be used in sql, json, csv etc. without escaping.
  *
  * @see https://tools.ietf.org/html/rfc1924
  */
export const Rfc1924Encoder = CreateClass( { __proto__ : Base85SimpleEncoder,
   ENCODE_MAP : StringToMap( "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~" ),
} );

/** This class encodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
  * This scheme does not use quotes or comma, and can usually be used in sql, json, csv etc. without escaping.
  *
  * @see https://rfc.zeromq.org/spec:32/Z85/
  */
export const Z85Encoder = CreateClass( { __proto__ : Base85SimpleEncoder,
   ENCODE_MAP : StringToMap( "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#" ),
} );

/** This class encodes data in the Ascii85 encoding (Adobe variant without &lt;~ and ~&gt;).
  * Supports "z" and "y" compression, which can be disabled individually.
  * Line break is not supported.
  *
  * Encoder instances can be safely shared by multiple threads.
  * @see https://en.wikipedia.org/wiki/Ascii85
  */
export const Ascii85Encoder = CreateClass( { __proto__ : Base85SimpleEncoder,
   ENCODE_MAP : StringToMap( "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstu" ),

   calcEncodedLength ( data ) {
      data = MakeInput( data );
      let result = super.calcEncodedLength( data );
      const { useZ, useY } = this;
      if ( useZ || useY ) {
         const buffer = new Uint32Array( data.buffer.slice( data.byteOffset ), 0, parseInt( data.byteLength / 4 ) );
         for ( let i = 0, len = buffer.length ; i <= len ; i++ )
            if ( ( useZ && buffer[ i ] == 0 ) || ( useY && buffer[ i ] == 0x20202020 ) )
               result -= 4;
      }
      return result;
   },

   useZ : true,
   useY : true,

   set zeroCompression ( compress ) { this.useZ = compress; },
   get zeroCompression () { return this.useZ; },

   set spaceCompression ( compress ) { this.useY = compress; },
   get spaceCompression () { return this.useY; },

   _writeData ( sum, map, out, wi ) {
      if ( this.useZ && sum == 0 )
         out[wi++] = 'z';
      else if ( this.useY && sum == 0x20202020 )
         out[wi++] = 'y';
      else
         return super._writeData( sum, map, out, wi );
      return wi;
   },
} );

export default {
   getRfc1942Encoder() { return Rfc1924Encoder; },
   getZ85Encoder() { return Z85Encoder; },
   getAscii85Encoder() { return Ascii85Encoder; },
};