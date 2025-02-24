package itstep.learning.dal.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dto.User;
import itstep.learning.dal.dto.UserAccess;
import itstep.learning.models.UserSignupFormModel;
import itstep.learning.services.db.DbService;
import itstep.learning.services.kdf.KdfService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class UserDao {
    private final Connection connection;
    private final Logger logger;
    private final KdfService kdfService;
    private final DbService dbService;

    @Inject
    public UserDao( DbService dbService, java.util.logging.Logger logger, itstep.learning.services.kdf.KdfService kdfService) throws SQLException {
        this.dbService  = dbService;
        this.connection = dbService.getConnection();
        this.logger     = logger;
        this.kdfService = kdfService;
    }
    
    public boolean update( User user ) {
        Map<String, Object> data = new HashMap<>();
        if( user.getName() != null ) {
            data.put( "name", user.getName() );
        }
        if( user.getPhone() != null ) {
            data.put( "phone", user.getPhone() );
        }
        if( data.isEmpty() ) return true;  // ?
        
        // TODO: convert to StringBuilder
        String sql = "UPDATE users SET ";
        boolean isFirst = true;
        for( Map.Entry<String, Object> entry : data.entrySet() ) {
            if( isFirst ) isFirst = false ;
            else sql += ", ";
            sql += entry.getKey() + " = ? "; 
        }
        sql += " WHERE user_id = ?";
        
        try( PreparedStatement prep = dbService.getConnection().prepareStatement(sql) ) {
            int param = 1;
            for( Map.Entry<String, Object> entry : data.entrySet() ) {
                prep.setObject( param, entry.getValue() );
                param += 1;
            }
            prep.setString( param, user.getUserId().toString() );
            prep.execute();
            dbService.getConnection().commit();
            return true;
        }
        catch( SQLException ex ) {
            logger.log( 
                    Level.WARNING, 
                    "UserDao::getUserById {0}, {1}",
                    new Object[] { ex.getMessage(), sql } );
        }
        return false;
    }
    
    public User getUserById( String id ) {
        UUID uuid;
        try {
            uuid = UUID.fromString( id );
        }
        catch( Exception ex ) {
            logger.log( 
                    Level.WARNING, 
                    "UserDao::getUserById Parse error: {0}", 
                    id );
            return null;
        }
        return getUserById( uuid ) ;
    }
    
    public User getUserById( UUID uuid ) {
        String sql = String.format(
            "SELECT u.* FROM users u WHERE u.user_id = '%s'",
            uuid.toString()
        );
        try( Statement stmt = dbService.getConnection().createStatement() ) {
            ResultSet rs = stmt.executeQuery( sql );
            if( rs.next() ) {
                return User.fromResultSet( rs );
            }
        }               
        catch( SQLException ex ) {
            logger.log( 
                    Level.WARNING, 
                    "UserDao::getUserById {0}, {1}",
                    new Object[] { ex.getMessage(), sql } );
        }
        return null;
    }
    
    public User addUser( UserSignupFormModel userModel ) {
        User user = new User() ;        
        user.setUserId( UUID.randomUUID() );
        user.setName( userModel.getName() );
        user.setEmail( userModel.getEmail() );
        user.setPhone( userModel.getPhone() );
        
        String sql = "INSERT INTO users ( user_id, name, email, phone ) "
                + " VALUES( ?, ?, ?, ? ) ";  
        try( PreparedStatement prep = this.connection.prepareStatement( sql ) ) {
            prep.setString( 1, user.getUserId().toString() );
            prep.setString( 2, user.getName()  );
            prep.setString( 3, user.getEmail() );
            prep.setString( 4, user.getPhone() );
            prep.executeUpdate() ;
        }
        catch( SQLException ex ) {
            logger.warning( "UserDao::addUser " + ex.getMessage() );
            try { this.connection.rollback(); }
            catch( SQLException ignore ) {}
            return null;
        }
        
        sql = "INSERT INTO users_access ( user_access_id, user_id, role_id, login, salt, dk ) "
                + " VALUES( UUID(), ?, 'guest', ?, ?, ? ) ";        
        try( PreparedStatement prep = dbService.getConnection().prepareStatement( sql ) ) {
            prep.setString( 1, user.getUserId().toString() );
            prep.setString( 2, user.getEmail() );
            String salt = UUID.randomUUID().toString().substring(0, 16);            
            prep.setString( 3, salt );
            prep.setString( 4, kdfService.dk( userModel.getPassword(), salt ) );
            prep.executeUpdate() ;
            dbService.getConnection().commit();
        }
        catch( SQLException ex ) {
            logger.warning( "UserDao::addUser " + ex.getMessage() );
            try { dbService.getConnection().rollback(); }
            catch( SQLException ignore ) {}
            return null;
        }
        return user;
    }
    
    public UserAccess authorize( String login, String password ) {
        String sql = 
                "SELECT * FROM users_access ua " +
               // "JOIN users u ON ua.user_id = u.user_id " +
                "WHERE ua.login = ?";
        try( PreparedStatement prep = 
                dbService.getConnection().prepareStatement( sql ) ) {
            prep.setString( 1, login );
            ResultSet rs = prep.executeQuery() ;
            if( rs.next() ) {
                String dk = kdfService.dk( password, rs.getString( "salt" ) ) ;
                if( Objects.equals( dk, rs.getString( "dk" ) ) ) {
                    return UserAccess.fromResultSet( rs ) ;
                }
            }
        }        
        catch( SQLException ex ) {
            logger.log( Level.WARNING, "UserDao::authorize {0}", ex.getMessage() );
        }
        return null;
    }
    
    public CompletableFuture deleteAsync( User user ) {
        String sql = String.format(
                "UPDATE users SET delete_moment = CURRENT_TIMESTAMP,"
                + " name = '', email = NULL, phone = NULL WHERE user_id = '%s'",
                user.getUserId().toString() ) ;
        
        String sql2 = String.format(
                "UPDATE users_access SET ua_delete_dt = CURRENT_TIMESTAMP,"
                + " login = UUID() WHERE user_id = '%s'",
                user.getUserId().toString() ) ;
        
        CompletableFuture task1 = CompletableFuture.runAsync( () -> {
            try( Statement stmt = dbService.getConnection().createStatement() ) {
                stmt.executeUpdate( sql );
            }        
            catch( SQLException ex ) {
                logger.log( Level.WARNING, "UserDao::delete1 {0}", ex.getMessage() );
                try { dbService.getConnection().rollback();}
                catch( SQLException ignore ) { }
            }
        } );
        
        CompletableFuture task2 = CompletableFuture.runAsync( () -> {
            try( Statement stmt = dbService.getConnection().createStatement() ) {
                stmt.executeUpdate( sql2 );
            }        
            catch( SQLException ex ) {
                logger.log( Level.WARNING, "UserDao::delete2 {0}", ex.getMessage() );
                try { dbService.getConnection().rollback();}
                catch( SQLException ignore ) { }
            }
        } );
        return CompletableFuture
                .allOf( task1, task2 )
                .thenRun( () -> {
                    try { dbService.getConnection().commit() ; }
                    catch( SQLException ignore ) { }
                });
    }
    
    public boolean installTables() {
        Future<Boolean> task1 = CompletableFuture
                .supplyAsync( this::installUsersAccess ) ;
        
        Future<Boolean> task2 = CompletableFuture
                .supplyAsync( this::intallUsers ) ;
        try {
            boolean res1 = task1.get() ;   // await task1
            boolean res2 = task2.get() ;   // await task2
            try { dbService.getConnection().commit() ; } catch( SQLException ignore ) { }
            return res1 && res2;
            // XX return await supplyAsync(1) && await supplyAsync(2)
        }
        catch( ExecutionException | InterruptedException ignore ) {
            return false;
        }
    }
    
    private boolean installUsersAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access("
                + "user_access_id  CHAR(36)     PRIMARY KEY DEFAULT( UUID() ),"
                + "user_id         CHAR(36)     NOT NULL,"
                + "role_id         VARCHAR(16)  NOT NULL,"
                + "login           VARCHAR(128) NOT NULL,"
                + "salt            CHAR(16)     NOT NULL,"
                + "dk              CHAR(20)     NOT NULL,"
                + "ua_delete_dt    DATETIME         NULL,"
                + "UNIQUE(login)"
                + ") Engine = InnoDB, DEFAULT CHARSET = utf8mb4";
        try( Statement statement = connection.createStatement() ) {
            statement.executeUpdate( sql ) ;
            logger.info( "UserDao::installUsersAccess OK" );
            return true;
        }
        catch( SQLException ex ) {
            logger.warning("UserDao::installUsersAccess " + 
                    ex.getMessage() );
        }
        return false;    
    }
    
    private boolean intallUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users("
                + "user_id       CHAR(36)    PRIMARY KEY DEFAULT( UUID() ),"
                + "name          VARCHAR(128) NOT NULL,"
                + "email         VARCHAR(256)     NULL,"
                + "phone         VARCHAR(32)      NULL,"
                + "delete_moment DATETIME         NULL"
                + ") Engine = InnoDB, DEFAULT CHARSET = utf8mb4";
        try( Statement statement = connection.createStatement() ) {
            statement.executeUpdate( sql ) ;
            logger.info( "UserDao::intallUsers OK" );
            return true;
        }
        catch( SQLException ex ) {
            logger.warning( "UserDao::intallUsers " + 
                    ex.getMessage() );
        }
        return false;    
    }
}
/*
Д.З. На базі сервісу RandomService створити endpoint (API)
який буде повертати згенерований рядок запитаного типу та розміру
Наприклад:
GET /random?type=salt&length=10    
-->
{
  "status": 200,
  "resourceUrl": "GET /random",
  "meta": {
    "dataType": "string",
    "read": "GET /random",
    "type": "salt",
    "length": 10
  },
  "cacheTime": 0,
  "data": "8s+3/.v`0)"
}
*/