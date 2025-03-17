package itstep.learning.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.dal.dto.AccessToken;
import itstep.learning.dal.dto.User;
import itstep.learning.dal.dto.UserAccess;
import itstep.learning.models.UserAuthJwtModel;
import itstep.learning.models.UserAuthViewModel;
import itstep.learning.rest.RestResponse;
import itstep.learning.rest.RestService;
import itstep.learning.services.hash.HashService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class UserServlet extends HttpServlet {
    private final DataContext dataContext;
    private final RestService restService;
    private final HashService hashService;
    private final Logger logger;

    @Inject
    public UserServlet( RestService restService, DataContext dataContext, Logger logger, itstep.learning.services.hash.HashService hashService) {
        this.restService = restService;
        this.dataContext = dataContext;
        this.logger = logger;
        this.hashService = hashService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RestResponse restResponse =  
                new RestResponse()
                .setResourceUrl( "GET /user" )
                .setMeta( Map.of(
                        "dataType", "object",
                        "read", "GET /user",
                        "update", "PUT /user",
                        "delete", "DELETE /user"
                ) );
        String authHeader = req.getHeader( "Authorization" );
        if( authHeader == null ) {
            restService.sendResponse( resp, 
                    restResponse.setStatus( 401 )
                            .setData( "Authorization header required" ) );
            return;
        }
        String authScheme = "Basic ";
        if( ! authHeader.startsWith( authScheme ) ) {
            restService.sendResponse( resp, 
                    restResponse.setStatus( 401 )
                            .setData( "Authorization scheme error" ) );
            return;
        }        
        String credentials = authHeader.substring( authScheme.length() ) ;
        try {
            credentials = new String( 
                    Base64.getDecoder().decode( 
                            credentials.getBytes() ) ) ;
        }
        catch( Exception ex ) {
            restService.sendResponse( resp, 
                    restResponse.setStatus( 422 )
                            .setData( "Decode error: " + ex.getMessage() ) );
            return;
        }
        String[] parts = credentials.split( ":", 2 ) ;
        if( parts.length != 2 ) {
            restService.sendResponse( resp, 
                    restResponse.setStatus( 422 )
                            .setData( "Format error splitting by ':' " ) );
            return;
        }
        UserAccess userAccess = dataContext.getUserDao().authorize( parts[0], parts[1] ) ;
        if( userAccess == null ) {
            restService.sendResponse( resp, 
                    restResponse.setStatus( 401 )
                            .setData( "Credentials rejected" ) );
            return;
        }
        // Створюємо токен для користувача
        AccessToken token = dataContext.getAccessTokenDao().create( userAccess ) ;
        User user =  dataContext.getUserDao().getUserById( userAccess.getUserId() );
        
        String jwtHeader = new String( Base64.getUrlEncoder().encode(
                "{\"alg\": \"HS256\", \"typ\": \"JWT\" }".getBytes() ) ) ;
        String jwtPayload = new String( Base64.getUrlEncoder().encode(
                restService.gson.toJson( userAccess ).getBytes() ) ) ;
        String jwtSignature = new String( Base64.getUrlEncoder().encode(
                hashService.digest( "the secret" + jwtHeader + "." + jwtPayload ).getBytes() ) ) ;
        String jwtToken = jwtHeader + "." + jwtPayload + "." + jwtSignature;
        
        restResponse
                .setStatus( 200 )
                .setData( 
                        // new UserAuthViewModel( user, userAccess, token )
                        new UserAuthJwtModel( user, jwtToken, 
                                dataContext
                                .getCartDao()
                                .getUserCart( userAccess.getUserAccessId(), false ) )
                )
                .setCacheTime( 600 );
        restService.sendResponse( resp, restResponse );
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RestResponse restResponse = new RestResponse()
                .setResourceUrl( "DELETE /user" )
                .setMeta( Map.of(
                        "dataType", "object",
                        "read", "GET /user",
                        "update", "PUT /user",
                        "delete", "DELETE /user"
                ) );
        String userId = req.getParameter( "id" ) ;   //  /user?id=...
        if( userId == null ) {
            restService.sendResponse( resp, restResponse
                .setStatus( 400 )
                .setData( "Missing required ID" )
            );
            return ;
        }
        UUID userUuid;
        try { userUuid = UUID.fromString( userId ) ; }
        catch( Exception ignore ) { 
            restService.sendResponse( resp, restResponse
                .setStatus( 400 )
                .setData( "Invalid ID format" )
            );
            return ;
        }
        User user = dataContext.getUserDao().getUserById( userUuid );
        if( user == null ) {
            restService.sendResponse( resp, restResponse
                .setStatus( 401 )
                .setData( "Unauthorized" )
            );
            return ;
        }
        try { dataContext.getUserDao().deleteAsync(user).get(); }
        catch( InterruptedException | ExecutionException ex ) {
            logger.log( Level.SEVERE, "deleteAsync fail: {0}", ex.getMessage() );
            restService.sendResponse( resp, restResponse
                .setStatus( 500 )
                .setData( "Server error. See server's logs" )
            );
            return ;
        }
        restResponse
                .setStatus( 202 )
                .setData( "Deleted" )
                .setCacheTime( 0 );
        restService.sendResponse( resp, restResponse );
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RestResponse restResponse =  
                new RestResponse()
                .setResourceUrl( "PUT /user" )
                .setMeta( Map.of(
                        "dataType", "object",
                        "read", "GET /user",
                        "update", "PUT /user",
                        "delete", "DELETE /user"
                ) );
        // Перевіряємо авторизацію за токеном
        UserAccess userAccess = (UserAccess) req.getAttribute( "authUserAccess" );
        if( userAccess == null ) {
            restService.sendResponse( resp, 
                    restResponse
                            .setStatus( 401 )
                            .setData( req.getAttribute( "authStatus" ) ) );
            return;
        }
        
        User userUpdates;
        try {
            userUpdates = restService.fromBody( req, User.class ) ;
        }
        catch( IOException ex ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 422 )
                    .setMessage( ex.getMessage() ) 
            );
            return;
        }
        if( userUpdates == null || userUpdates.getUserId() == null ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 422 )
                    .setMessage( "Unparseable data or identity undefined" )
            );
            return;
        }
        User user = dataContext
                .getUserDao()
                .getUserById( userUpdates.getUserId() );
        if( user == null ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 404 )
                    .setMessage( "User not found" )
            );
            return;
        }
        
        if( ! dataContext.getUserDao().update( userUpdates ) ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 500 )
                    .setMessage( "Server error. See logs" )
            );
            return;
        }
        
        restResponse
                .setStatus( 202 )
                .setData( userUpdates )
                .setCacheTime( 0 );
        restService.sendResponse( resp, restResponse );
    }
    
    
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        restService.setCorsHeaders( resp );
    }
}
