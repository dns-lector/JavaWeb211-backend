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
        }
        return connection ;
    } 
    
}
/*
Розширити UserDao::update для врахування додаткових полів, релізованих
у попередніх ДЗ

// Розширити таблицю користувача, додати відомості з різними типами даних
// Реалізувати User.fromResultSet з урахуванням усіх полів.
*/