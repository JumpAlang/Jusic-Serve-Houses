package com.scoder.jusic.util;

/**
 * @author JumpAlang
 * @create 2020-06-10 11:31
 */
public class CustomEncrypt{

    public static void main( String[] args ) {
        /*
         * c#给的正确测试用例: id=>mid
         * 991135225=>001B4Lk5
         * 981411932=>001A5Vj6
         * 988914785=>001ABp8R
         * 97879005=>0009x3GJ
         *
         * 981612013=>001A6Lmd
         *
         * 97949621=>0009xm3H
         *
         * 97919383=>0009xebZ
         *
         * 984113004=>001AhfYM
         *
         * 986013809=>001ApesV
         *
         * 97416233=>0009v7iF
         *
         * 9090103=>C8Kz
         *
         * 9194277359=>00ePhWJF
         * 96521442=>0009rmwy
         */

        String ids = "9734249,981411932,988914785,97879005,981612013,97949621,97919383,984113004,986013809,97416233,9090103,9194277359,96521442";
        for(String id : ids.split( "," )){
            String mid = idToMid(id);
            System.out.println("mid:"+mid);
            System.out.println("id:"+midToId(mid));
        }
        String sessionId = "5b1f11d0-ad92-4855-ae44-b2052ecd76d8";

    }

    // / <summary>
    // / The str62keys
    // / </summary>
    private static String str62keys = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // / <summary>
    // / 将Mid转换为Id
    // / </summary>
    // / <param name="str62">The STR62.</param>
    // / <returns>System.String.</returns>
    public static String midToId( String str62 ) {
        String id = "";
        // 从最后往前以4字节为一组读取字符
        for ( int i = str62.length( ) - 4; i > -4; i = i - 4 ) {
            int offset = i < 0 ? 0 : i;
            int len = i < 0 ? str62.length( ) % 4 : 4;
            long encode = encode62ToInt( left( str62, offset, len ) );
            String str = String.valueOf( encode );
            if ( offset > 0 )
                str = leftPad( str, 7, '0' ); // 若不是第一组，则不足7位补0
            id = str + id;
        }
        return id;
    }

    // / <summary>
    // / 将Id转制为Mid
    // / </summary>
    // / <param name="mid">The mid.</param>
    // / <returns>System.String.</returns>
    public static String idToMid( String mid ) {

        // long int_mid = long.Parse(mid);
        String result = "";
        for ( int i = mid.length( ) - 7; i > -7; i -= 7 ) {
            int offset1 = ( i < 0 ) ? 0 : i;
            int offset2 = i + 7;
            String num = intToEnode62( left( mid, offset1, offset2 - offset1 ) );
            result = num + result;
        }
        return result;
    }

    // / <summary>
    // / Ints to enode62.
    // / </summary>
    // / <param name="mid">The mid.</param>
    // / <returns>System.String.</returns>
    private static String intToEnode62( String mid ) {
        long int_mid = Long.parseLong( mid );
        String result = "";
        do {
            long a = int_mid % 62;
            result = str62keys.charAt( ( int ) a ) + result;
            int_mid = ( int_mid - a ) / 62;
        } while ( int_mid > 0 );

        return leftPad( result, 4, '0' );
    }

    // / <summary>
    // / Encode62s to int.
    // / </summary>
    // / <param name="str62">The STR62.</param>
    // / <returns>System.Int64.</returns>
    private static long encode62ToInt( String str62 ) {
        long i10 = 0;

        for ( int i = 0; i < str62.length( ); i++ ) {
            double n = str62.length( ) - i - 1;
            i10 += str62keys.indexOf( str62.charAt( i ) ) * Math.pow( 62, n );
        }
        String temp = leftPad( String.valueOf( i10 ), 7, '0' );
        // Long.TryParse(temp, out i10);
        try {
            i10 = Long.parseLong( temp );
        } catch ( Exception e ) {
            // TODO: handle exception
        } finally {
            return i10;
        }
    }

    // 左边补
    public static String leftPad( String s, int size, char padChar ) {
        int length = s.length( );
        if ( length == 0 ) {
            return s;
        }
        int pads = size - length;
        if ( pads <= 0 ) {
            return s;
        }
        return padding( pads, padChar ).concat( s );
    }

    // 填充
    private static String padding( int repeat, char padChar ) {
        if ( repeat < 0 ) {
            throw new IndexOutOfBoundsException( "Cannot pad a negative amount: " + repeat );
        }
        char[] buf = new char[ repeat ];
        for ( int i = 0; i < buf.length; i++ ) {
            buf[ i ] = padChar;
        }
        return new String( buf );
    }

    /**
     * 左起截取字符串
     *
     * @param s
     * @param len
     * @return
     */
    public static String left( String s, int begin, int len ) {
        int length = length( s );
        if ( length <= len ) {
            return s;
        }
        return s.substring( begin, begin > 0 ? begin + len : len );
    }

    /**
     * 长度
     *
     * @param s
     * @return
     */
    public static int length( String s ) {
        return s != null ? s.length( ) : 0;
    }

}