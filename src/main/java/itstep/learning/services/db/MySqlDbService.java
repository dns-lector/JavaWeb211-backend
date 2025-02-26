package itstep.learning.services.db;

import com.google.inject.Singleton;
import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Singleton
public class MySqlDbService implements DbService {
    private Connection connection ;

    @Override
    public Connection getConnection() throws SQLException {
        if( connection == null ) {
            String connectionString = "jdbc:mysql://localhost:3308/java221"
                    + "?useUnicode=true&characterEncoding=UTF-8";
            // DriverManager.registerDriver(
            //         new com.mysql.cj.jdbc.Driver()
            // );
            // connection = DriverManager.getConnection(
            //         connectionString,
            //         "user221",
            //         "pass221"
            // );
            MysqlDataSource mds = new MysqlDataSource();
            mds.setURL( connectionString );
            connection = mds.getConnection( "user221", "pass221" );
            connection.setAutoCommit( false );
        }
        return connection ;
    } 
    
}
/*
Д.З. Підключити сервіс конфігурації до MySqlDbService.
Реалізувати деталі підключення у відповідності до даних конфігурації.
Забезпечити відображення (логування) винятків.
** Виключити appsettings.json з репозиторію, створити файл-зразок
   з ***** паролями.
*/