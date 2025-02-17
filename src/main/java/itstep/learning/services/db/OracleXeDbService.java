package itstep.learning.services.db;

import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;

@Singleton
public class OracleXeDbService implements DbService {
    private Connection connection ;

    @Override
    public Connection getConnection() throws SQLException {
        if( connection == null ) {
            // OracleDataSource ods = new OracleDataSource();
            // ods.setURL( "jdbc:oracle:thin:@localhost/XE" );
            // connection = ods.getConnection( "SYSTEM", "root" );
        }
        return connection ;
    }
    
}
