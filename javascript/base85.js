// Constants used in encoding and decoding
const Power4 = 52200625; // 85^4
const Power3 = 614125;  // 85^3
const Power2 = 7225;   // 85^2

const TypedArrayType = Object.getPrototypeOf( Uint8Array );

// Convert all compatible data to DataView
function MakeDataView ( data, size ) {
   if ( data == null || data == undefined )
      return new DataView( new Uint8Array( size() ).buffer );
   if ( data instanceof DataView )
      return data;
   if ( typeof( data ) === 'string' )
      data = new TextEncoder().encode( data );
   if ( data instanceof TypedArrayType )
      return new DataView( data.buffer );
   if ( data instanceof ArrayBuffer )
      return new DataView( data );
   throw new TypeError( 'Unsupported Base85 input/output: must be string, TypedArray, DataView, or ArrayBuffer.' );
}

// Convert all compatible output to Uint8Array
function MakeUint8 ( data, size ) {
   if ( data == null || data == undefined )
      return new Uint8Array( size() );
   if ( data instanceof Uint8Array )
      return data;
   if ( typeof( data ) === 'string' )
      return new TextEncoder().encode( data );
   if ( data instanceof TypedArrayType )
      return new Uint8Array( data.buffer );
   if ( data instanceof DataView )
      return new Uint8Array( data.buffer, data.byteOffset, data.byteLength );
   if ( data instanceof ArrayBuffer )
      return new Uint8Array( data );
   throw new TypeError( 'Unsupported Base85 input/output: must be string, TypedArray, DataView, or ArrayBuffer' );
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

function CreateClass ( baseObject, map ) {
   const constructor = function(){};
   if ( map ) {
      const decodeMap = baseObject.DECODE_MAP, validMap = baseObject.VALID_BYTES;
      for ( let i = 0, len = map.length ; i < len ; i++ ) {
         const b = map[ i ];
         decodeMap[ b ] = i;
         validMap [ b ] = true;
      }
   }
   constructor.prototype = baseObject;
   Object.assign( constructor, baseObject );
   Object.setPrototypeOf( constructor, baseObject.__proto__ );
   return constructor;
}


/* A basic encoder.  Just inherit and override ENCODE_MAP to get a functional encoder. */
export const Base85Encoder = {

   /** Calculate the max encoded byte length of a given input data. */
   calcEncodedLength ( data ) {
      return ~~Math.ceil( MakeDataView( data ).byteLength * 1.25 );
   },

   /** Encode data.
    * 1. If a String is supplied, a Base85 string of its utf-8 data will be returned.
    * 2. If a TypedArray is supplied, a TypedArray of the Base85 data will be returned.
    * 3. A TypedArray output buffer can also be supplied, in which case the number of Base85 data will be returned regardless of input type.
    */
   encode ( data, out ) {
      const inputIsString = typeof( data ) === 'string', outputIsProveded = out ? true : false;
      data = MakeDataView( data );
      out = MakeUint8( out, () => this.calcEncodedLength( data ) );
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
      data = MakeDataView( data );
      const size = this.calcEncodedLength( data ), map = this.getEncodeMap();
      out = MakeUint8( out, () => size );
      let sum = BigInt( 0 ), b85 = BigInt( 85 ), b8 = BigInt( 8 );
      for ( let i = data.byteOffset, len = data.byteLength ; i < len ; i++ )
         sum = ( sum << b8 ) + BigInt( data.getUint8( i ) );
      for ( let i = size - 1 ; i >= 0 ; i-- ) {
         const mod = sum % b85;
         sum = ( sum - mod ) / b85;
         out[ i ] = map[ mod ];
      }
      return [ out, size ];
   },

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
      for ( let loop = parseInt( rlen / 4 ) ; loop > 0 ; loop--, ri += 4 )
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

   getEncodeMap () { return this.ENCODE_MAP; },
   getCharset() { return BufToCode( this.getEncodeMap() ); },
};
if ( typeof( BigInt ) == 'undefined' ) delete Base85Encoder.encodeBlockReverse;

/** This class encodes data in the Base85 encoding scheme using the character set described by IETF RFC 1924,
  * but in the efficient algorithm of Ascii85 and Z85.
  * This scheme does not use quotes, comma, or slash, and can usually be used in sql, json, csv etc. without escaping.
  *
  * @see https://tools.ietf.org/html/rfc1924
  */
export const Rfc1924Encoder = CreateClass( { __proto__ : Base85Encoder,
   ENCODE_MAP : StringToMap( "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~" ),
} );

/** This class encodes data in the Base85 encoding scheme Z85 as described by ZeroMQ.
  * This scheme does not use quotes or comma, and can usually be used in sql, json, csv etc. without escaping.
  *
  * @see https://rfc.zeromq.org/spec:32/Z85/
  */
export const Z85Encoder = CreateClass( { __proto__ : Base85Encoder,
   ENCODE_MAP : StringToMap( "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#" ),
} );

/** This class encodes data in the Ascii85 encoding (Adobe variant without &lt;~ and ~&gt;).
  * Supports "z" and "y" compression, which can be disabled individually.
  * Line break is not supported.
  *
  * Encoder instances can be safely shared by multiple threads.
  * @see https://en.wikipedia.org/wiki/Ascii85
  */
export const Ascii85Encoder = CreateClass( { __proto__ : Base85Encoder,
   ENCODE_MAP : StringToMap( "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstu" ),

   calcEncodedLength ( data ) {
      data = MakeDataView( data );
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
         out[wi++] = 122; // 'z'
      else if ( this.useY && sum == 0x20202020 )
         out[wi++] = 121; // 'y'
      else
         return super._writeData( sum, map, out, wi );
      return wi;
   },
} );



/* A basic decoder.  Just inherit and override Name, ENCODE_MAP, and VALID_BYTES to get a functional decoder. */
export const Base85Decoder = {
   /** Calculate byte length of decoded data.
     * Assumes data is correct; use test method to validate data.
     */
   calcDecodedLength ( data ) {
      data = MakeUint8( data );
      const length = data.byteLength;
      if ( length % 5 == 1 ) throw new Error( length + " is not a valid Base85/" + this.Name + " data length." );
      return parseInt( length * 0.8 );
   },

   decode ( data, out, forceByteOutput ) {
      const inputIsString = typeof( data ) === 'string', outputIsProveded = out ? true : false;
      data = MakeUint8( data );
      out = MakeUint8( out, () => this.calcDecodedLength( data ) );
      const len = this._decode( data, out );
      if ( ! forceByteOutput && outputIsProveded ) return len;
      if ( len !== out.byteLength ) out = out.slice( 0, len );
      if ( ! forceByteOutput && inputIsString ) return BufToText( out );
      return out;
   },

   /** Decode Base85 data into a new DataView. */
   decodeToBytes ( data, out ) {
      return this.decode( data, out, true );
   },

   /** Decode the data as one block in reverse input order.
     * This is the strict algorithm specified by RFC 1924 for IP address decoding,
     * when the data is exactly 16 bytes (128 bits) long.
     * Output is Uint8Array.
     */
   decodeBlockReverse ( data, out ) {
      data = MakeUint8( data );
      const size = Math.max( 0, Math.ceil( data.length * 0.8 ) ), map = this.getDecodeMap().map( e => BigInt( e ) );
      out = MakeUint8( out, () => size );
      let sum = BigInt( 0 ), b255 = BigInt( 255 ), b85 = BigInt( 85 ), b8 = BigInt( 8 );
      for ( let i = data.byteOffset, len = data.byteLength ; i < len ; i++ )
         sum = sum * b85 + map[ data[ i ] ];
      throw 'Not Implemented';
      //System.arraycopy( sum.toByteArray(), 0, out, out_offset, size );
      return [ out, size ];
   },

   /** Test that given data can be decoded correctly. */
   test ( data ) {
      data = MakeUint8( data );
      const valids = this.getValidBytes();
      for ( let i = 0, len = data.length ; i < len ; i++ )
         if ( ! valids[ data[ i ] ] ) return false;
      try {
         this.calcDecodedLength( data );
      } catch ( ex ) { return false; }
      return true;
   },

   _decodeDangling ( decodeMap, data, ri, buffer, leftover ) {
      if ( leftover == 1 ) throw new Error( "Malformed Base85/" + this.Name + " data" );
      let sum = decodeMap[ data[ri  ] ] * Power4 +
                decodeMap[ data[ri+1] ] * Power3 + 85;
      if ( leftover >= 3 ) {
         sum += decodeMap[ data[ri+2] ] * Power2;
         if ( leftover >= 4 )
            sum += decodeMap[ data[ri+3] ] * 85;
         else
            sum += Power2;
      } else
         sum += Power3 + Power2;
      buffer.setUint32( 0, sum );
      return leftover-1;
   },

   _decode ( data, out ) {
      let ri = 0, wi = 0, rlen = data.byteLength, leftover = rlen % 5;
      const decodeMap = this.getDecodeMap(), buf = new Uint8Array( 4 ), buffer = new DataView( buf.buffer );
      for ( let loop = parseInt( rlen / 5 ) ; loop > 0 ; loop--, ri += 5, wi += 4 ) {
         this._putData( buffer, decodeMap, data, ri );
         out.set( buf, wi );
      }
      if ( leftover == 0 ) return wi;
      leftover = this._decodeDangling( decodeMap, data, ri, buffer, leftover );
      out.set( new Uint8Array( buf.buffer, 0, leftover ), wi );
      return wi * 4 + leftover;
   },

   _putData ( buffer, map, data, ri ) {
      buffer.setUint32( 0, map[ data[ri  ] ] * Power4 +
                           map[ data[ri+1] ] * Power3 +
                           map[ data[ri+2] ] * Power2 +
                           map[ data[ri+3] ] * 85 +
                           map[ data[ri+4] ] );
   },

   getDecodeMap() { return this.DECODE_MAP; },
   getValidBytes() { return this.VALID_BYTES; },
};
if ( typeof( BigInt ) == 'undefined' ) delete Base85Decoder.decodeBlockReverse;

/** This class decodes data in the Base85 encoding using the character set described by IETF RFC 1924,
  * in the efficient algorithm of Ascii85 and Z85.
  * Malformed data may or may not throws IllegalArgumentException on decode; call test(byte[]) to check data if necessary.
  * Decoder instances can be safely shared by multiple threads.
  * @see https://tools.ietf.org/html/rfc1924
  */
export const Rfc1924Decoder = CreateClass( { __proto__ : Base85Decoder,
   DECODE_MAP : new Uint8Array( 127 ),
   VALID_BYTES : new Array( 127 ),
   Name : "RFC1924",
}, Rfc1924Encoder.ENCODE_MAP );


export default {
   getRfc1942Encoder() { return Rfc1924Encoder; },
   getRfc1942Decoder() { return Rfc1924Decoder; },
   getZ85Encoder() { return Z85Encoder; },
   getAscii85Encoder() { return Ascii85Encoder; },
};