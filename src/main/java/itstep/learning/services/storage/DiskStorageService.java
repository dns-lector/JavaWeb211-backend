package itstep.learning.services.storage;

import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Singleton
public class DiskStorageService implements StorageService {
    private final String storagePath = "C:/storage/Java211/";
    /* Д.З. Перенести дані про шлях сховища до конфігурації. Забезпечити її інжекцію  */
    
    @Override
    public String put( InputStream inputStream, String ext ) throws IOException {
        String itemId = UUID.randomUUID() + ext;
        File file = new File( storagePath + itemId ) ;
        try( FileOutputStream writer = new FileOutputStream(file) ) {
            byte[] buf = new byte[131072];
            int len;
            while( ( len = inputStream.read(buf) ) > 0 ) {
                writer.write( buf, 0, len );
            }
        }
        return itemId;
    }

    @Override
    public InputStream get( String itemId ) throws IOException {
        return new FileInputStream( storagePath + itemId ) ;
    }
    
}
