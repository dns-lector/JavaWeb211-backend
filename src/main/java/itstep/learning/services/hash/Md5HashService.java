package itstep.learning.services.hash;

import com.google.inject.Singleton;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Singleton
public class Md5HashService implements HashService {

    @Override
    public String digest( String input ) {
        try {
            char[] chars = new char[32];
            int i = 0;
            for( byte b : MessageDigest.getInstance( "MD5" ).digest( input.getBytes() ) ) {
                int bi = b & 0xFF ;
                String str = Integer.toHexString( bi ) ;  // b = 50 -> str = "32"
                if( bi < 16 ) {                  // b  = 10  -> str = 'A'
                    chars[i] = '0';              // 0
                    chars[i+1] = str.charAt(0);  // A
                }
                else {
                    chars[i] = str.charAt(0);
                    chars[i+1] = str.charAt(1);
                }
                i += 2;
            } // -1   1111 1111     0000 0000 1111 1111 == 255   1111 1111 1111 1111 == -1
            return new String( chars );
        }
        catch( NoSuchAlgorithmException ex ) {
            throw new RuntimeException( ex ) ;
        }
    }
    
}
