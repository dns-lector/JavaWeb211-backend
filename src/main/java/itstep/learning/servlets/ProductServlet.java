package itstep.learning.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.dal.dto.Category;
import itstep.learning.dal.dto.Product;
import itstep.learning.models.UserAuthJwtModel;
import itstep.learning.rest.RestResponse;
import itstep.learning.rest.RestService;
import itstep.learning.services.form_parse.FormParseResult;
import itstep.learning.services.form_parse.FormParseService;
import itstep.learning.services.storage.StorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.fileupload2.core.FileItem;

@Singleton
public class ProductServlet extends HttpServlet {
    private final FormParseService formParseService;
    private final StorageService storageService;
    private final RestService restService;
    private final DataContext dataContext;
    
    @Inject
    public ProductServlet( FormParseService formParseService, StorageService storageService, itstep.learning.rest.RestService restService, itstep.learning.dal.dao.DataContext dataContext) {
        this.formParseService = formParseService;
        this.storageService = storageService;
        this.restService = restService;
        this.dataContext = dataContext;
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FormParseResult formParseResult = formParseService.parseRequest( req ) ;
        RestResponse restResponse = new RestResponse()
                .setResourceUrl( "POST /product" )
                .setMeta( Map.of(
                        "dataType", "object",
                        "read", "GET /product",
                        "update", "PUT /product",
                        "delete", "DELETE /product"
                ) );
        Product product = new Product();
        String str;
        
        str = formParseResult.getFields().get( "product-title" );
        if( str == null || str.isBlank() ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 400 )
                    .setData( "Missing or empty 'product-title'" ) );
            return;
        }
        product.setProductTitle( str );        
        
        str = formParseResult.getFields().get( "product-description" );
        if( str == null || str.isBlank() ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 400 )
                    .setData( "Missing or empty 'product-description'" ) );
            return;
        }
        product.setProductDescription( str );
        
        str = formParseResult.getFields().get( "product-code" );
        product.setProductSlug( str );        
        
        str = formParseResult.getFields().get( "product-price" );
        try { product.setPrice( Double.parseDouble( str ) ); }
        catch( NumberFormatException | NullPointerException ex ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 400 )
                    .setData( "Data parse error 'product-price' " + ex.getMessage() ) );
            return;
        }
        
        str = formParseResult.getFields().get( "product-stock" );
        try { product.setStock( Integer.parseInt( str ) ); }
        catch( NumberFormatException | NullPointerException ex ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 400 )
                    .setData( "Data parse error 'product-stock' " + ex.getMessage() ) );
            return;
        }
        
        str = formParseResult.getFields().get( "category-id" );
        try { product.setCategoryId( UUID.fromString( str ) ); }
        catch( IllegalArgumentException | NullPointerException ex ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 400 )
                    .setData( "Data parse error 'category-id' " + ex.getMessage() ) );
            return;
        }
        
        FileItem image = formParseResult.getFiles().get( "product-image" );
        if( image.getSize() > 0 ) {
            int dotPosition = image.getName().lastIndexOf( '.' );
            String ext = image.getName().substring( dotPosition ) ;
            str = storageService.put( image.getInputStream(), ext ) ;
        }
        else {
            str = null;
        }
        product.setProductImageId( str );
        
        product = dataContext.getProductDao().addNewProduct( product );
        if( product == null ) {
            // додавання у БД не відбулось - видалити файл зі сховища.
            /*
            Д.З. Реалізувати сервіс видалення файлів (як частину StorageService)
            Додати перевірку на успішне додавання товару до БД, у разі помилки
            видаляти завантажений файл-картинку.
            Для випробування можна використати дублювання коду/slug
            * Після випробувань додати перевірку на унікальність коду товару.
            */
            
            restService.sendResponse( resp, restResponse
                    .setStatus( 500 )
                    .setData( "Internal Error. See logs " ) );
            return;
        }
        
        restService.sendResponse( resp, restResponse
                    .setStatus( 200 ).setData( product ) );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        if( "categories".equals(type) ) {    //   .../product?type=categories
            getCategories(req, resp);
        }
        else if( "category".equals(type) ) {    //   .../product?type=category&id=12312
            getCategory(req, resp);
        }
        else {
            getProducts(req, resp);
        }
    }
    
    private void getCategories(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String imgPath = getStoragePath( req );
        List<Category> categories = dataContext.getCategoryDao().getList();
        for( Category c : categories ) {
            c.setCategoryImageId( imgPath + c.getCategoryImageId() );
        }
        restService.sendResponse( resp, 
            new RestResponse()
                .setResourceUrl( "GET /product?type=categories" )
                .setMeta( Map.of(
                        "dataType", "array"
                ) )
                .setStatus( 200 )
                .setCacheTime( 86400 )
                .setData( categories ) ) ; 
    }
    
    private void getCategory(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String slug = req.getParameter( "slug" );
        RestResponse restResponse = new RestResponse()
                .setResourceUrl( "GET /product?type=category&slug=" + slug )
                .setMeta( Map.of(
                        "dataType", "object"
                ) )
                .setCacheTime( 86400 ) ;
        Category category;
        try { category = dataContext.getCategoryDao().getCategoryBySlug( slug ) ; }
        catch( RuntimeException ignore ) {
            restService.sendResponse( resp, restResponse
                .setStatus( 500 )
                .setData( "Take a look to the Logs" ) ) ;
            return;
        }
        if( category == null ) {
            restService.sendResponse( resp, restResponse
                .setStatus( 404 )
                .setData( "Category not found" ) ) ;
            return;
        }
        String imgPath = getStoragePath( req );
        category.setCategoryImageId( imgPath + category.getCategoryImageId() );
        for( Product p : category.getProducts() ) {
            p.setProductImageId( imgPath + p.getProductImageId() );
        }
        restService.sendResponse( resp, restResponse
                .setStatus( 200 )
                .setData( category ) ) ;
    }
    
    private void getProducts(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
    }
    
    private String getStoragePath( HttpServletRequest req ) {
        return String.format( Locale.ROOT, 
                "%s://%s:%d%s/storage/",
                req.getScheme(),
                req.getServerName(),
                req.getServerPort(),
                req.getContextPath()
        );
    }
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        restService.setCorsHeaders( resp );
    }
}
/*
Д.З. Оформити сторінку додавання нового продукту
* обмежити доступ до неї за токеном.
** обмежити доступ тільки з роллю "адміністратор"
*/