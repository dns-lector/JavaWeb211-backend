package itstep.learning.dal.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dto.Product;
import itstep.learning.services.db.DbService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ProductDao { 
    private final DbService dbService;
    private final Logger logger;    

    @Inject
    public ProductDao( DbService dbService, Logger logger ) throws SQLException {
        this.dbService = dbService;
        this.logger    = logger;
    }
    
    public Product addNewProduct( Product product ) {
        product.setProductId( UUID.randomUUID() );
        String sql = "INSERT INTO products(product_id, category_id, product_title,"
                + "product_description, product_slug, product_image_id,"
                + "product_delete_moment, product_price, product_stock) "
                + "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ? )";
        try( PreparedStatement prep = dbService.getConnection().prepareStatement(sql) ) {
            prep.setString( 1, product.getProductId().toString() );
            prep.setString( 2, product.getCategoryId().toString() );
            prep.setString( 3, product.getProductTitle() );
            prep.setString( 4, product.getProductDescription());
            prep.setString( 5, product.getProductSlug());
            prep.setString( 6, product.getProductImageId());
            prep.setTimestamp( 7, product.getDeleteMoment() == null ? null :
                    new Timestamp( product.getDeleteMoment().getTime() ) );
            prep.setDouble( 8, product.getPrice() );
            prep.setInt( 9, product.getStock() );
            prep.executeUpdate() ;
            dbService.getConnection().commit();
            return product ;
        }        
        catch( SQLException ex ) {
            logger.log( 
                    Level.WARNING, 
                    "ProductDao::addNewProduct {0} sql: '{1}'",
                    new Object[] { ex.getMessage(), sql } 
            );
            return null;
        }
    }
    
    public boolean installTables() {
        String sql = "CREATE TABLE IF NOT EXISTS products ("
                + "product_id            CHAR(36)      PRIMARY KEY DEFAULT( UUID() ),"
                + "category_id           CHAR(36)      NOT NULL,"
                + "product_title         VARCHAR(64)   NOT NULL,"
                + "product_description   VARCHAR(256)  NOT NULL,"
                + "product_slug          VARCHAR(64)       NULL,"
                + "product_image_id      VARCHAR(64)       NULL,"
                + "product_delete_moment DATETIME          NULL,"
                + "product_price         FLOAT         NOT NULL,"
                + "product_stock         INT           NOT NULL,"
                + "UNIQUE(product_slug)"
                + ") Engine = InnoDB, DEFAULT CHARSET = utf8mb4";
        
        try( Statement statement = dbService.getConnection().createStatement() ) {
            statement.executeUpdate( sql ) ;
            dbService.getConnection().commit() ;
            logger.info( "ProductDao::installTables OK" );
            return true;
        }
        catch( SQLException ex ) {
            logger.log( 
                    Level.WARNING, 
                    "ProductDao::installTables {0} sql: '{1}'",
                    new Object[] { ex.getMessage(), sql } 
            );
        }
        return false;
    }
}
