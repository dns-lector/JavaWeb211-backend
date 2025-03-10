package itstep.learning.servlets;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.dal.dto.User;
import itstep.learning.models.UserSignupFormModel;
import itstep.learning.rest.RestResponse;
import itstep.learning.rest.RestService;
import itstep.learning.services.config.ConfigService;
import itstep.learning.services.db.DbService;
import itstep.learning.services.kdf.KdfService;
import itstep.learning.services.random.RandomService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// import oracle.jdbc.pool.OracleDataSource;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

// @WebServlet("/home")
@Singleton
public class HomeServlet extends HttpServlet {
    private final RandomService randomService;
    private final KdfService kdfService;
    private final DbService dbService;
    private final DataContext dataContext;
    private final RestService restService;
    private final ConfigService configService;
    
    @Inject
    public HomeServlet( RandomService randomService, KdfService kdfService, DbService dbService, itstep.learning.dal.dao.DataContext dataContext, itstep.learning.rest.RestService restService, itstep.learning.services.config.ConfigService configService) {
        this.randomService = randomService;
        this.kdfService = kdfService;
        this.dbService = dbService;
        this.dataContext = dataContext;
        this.restService = restService;
        this.configService = configService;
    }    

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String message;
        try {
            String sql = "SELECT CURRENT_TIMESTAMP";
            Statement statement = dbService.getConnection().createStatement();
            ResultSet resultSet = statement.executeQuery( sql );
            resultSet.next();
            message = resultSet.getString( 1 );   // !!! JDBC відлік з 1
            // resultSet.close();

            resultSet = statement.executeQuery( "SHOW DATABASES" );
            StringBuilder sb = new StringBuilder();
            while ( resultSet.next() ) {
                sb.append( ", " );
                sb.append( resultSet.getString( 1 ) );
            }
            resultSet.close();
            statement.close();
            message += sb.toString();
        }
        catch( SQLException ex ) {
            message = ex.getMessage();
        }
        String msg = 
                dataContext.getUserDao().installTables()
                && dataContext.getAccessTokenDao().installTables()
                && dataContext.getProductDao().installTables()
                // && dataContext.getCategoryDao().seedData()
                    ? "Install OK"
                    : "Install Fail";
        
        restService.sendResponse( resp, 
                new RestResponse()
                .setStatus( 200 )
                .setMessage( message + " " + 
                        randomService.randomInt() + " " + 
                        configService.getValue("db.MySql.port").getAsInt() + " " + 
                        msg )
        ); // 20 2c b9 62 ac 59 07 5b 96 4b 07 15 2d 23 4b 70
    }      // 202cb962ac59075b964b07152d234b70

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RestResponse restResponse =  
                new RestResponse()
                .setResourceUrl( "POST /home" )
                .setCacheTime( 0 )
                .setMeta( Map.of(
                        "dataType", "object",
                        "read", "GET /home",
                        "update", "PUT /home",
                        "delete", "DELETE /home"
                ) );
        
        UserSignupFormModel model;
        try {
            model = restService.fromBody( req, UserSignupFormModel.class ) ;
        }
        catch( IOException ex ) {
            restService.sendResponse( resp, restResponse
                    .setStatus( 422 )
                    .setMessage( ex.getMessage() ) 
            );
            return ;
        }
        User user = dataContext.getUserDao().addUser( model ) ;
        if( user == null ) {
            restResponse
                .setStatus( 507 )
                .setMessage( "DB error" )                
                .setData( model ) ;
        }
        else {
            restResponse
                .setStatus( 201 )
                .setMessage( "Created" )                
                .setData( user ) ;
        }
        restService.sendResponse( resp, restResponse ) ;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        restService.setCorsHeaders( resp );
    }
}
/*
DAL                 Data Access Layer
 MySqlRepo
   DTO (Entity)     Data Transfer Object
     User
     Token
   DAO              Data Access Object (CRUD)
     UserDao
     TokenDao
 OracleRepo
   DTO (Entity)
     Product
     Action
   DAO
     ProductDao
     ActionDao
  
Д.З. Модифікувати сервіс випадкових чисел:
підключити до нього серві ДатаЧасу, за допомогою якого ініціалізувати (сідувати)
генератор.
Зареєструвати в інжекторі, підключити у сервлеті, включити
результат роботи до відповіді сервера.
*/
/*
IoC Inversion of Control - Інверсія управління - Архітектурний патерн,
згідно з яким управління (життєвим циклом об'єктів) перекладається на
спеціалізований модуль (контейнер служб, інжектор, Resolver).

1) Реєстрація - додавання до контейнера інформації, зазвичай у формі
    [тип - час життя (scope)]
        | 
      |тип1|
      |тип2|  Services - загальнодоступні об'єкти
      |тип3|
     Container

2) Resolve:  class -> || -> object (у т.ч. не новий)
                      |Connection|
                      |Logger    |
                            \ injection
class(Connection c, Logger logger)
{ _conn = c, _logger = logger) }

DI - Dependency Injection - спосіб (патерн) передачі посилань на об'єкти-служби
до інших об'єктів.

DIP - Dependency Inversion Principle (SOLID) - загально-архітектурний принцип,
який радить вживати максимально можливу абстракцію для залежностей


Java, web

  Deploy
App -> Tomcat -> Run 
           {event->listener}  -- повідомлення про подію запуску / деплою
                |   |
    Request1 -> |   | -> Response1
    Request2 -> |   | -> Response2
                 / \ 
     ->[Filters]->[Servlet]->[JSP]->
      Middleware  Controller  Razor


Guice - Бібліотека для IoC від Google
Spring
*/

/*
Асинхронне виконання коду
Синхронне - послідовне (у часі)
   ++++++++----------*********
Асинхронність - будь-яке відхилення від синхронності
   ++++++++
   ---------     Паралельне виконання
   *********

   +++++++---------    Ниткового типу
   *********

   +  +
    -  -         Паралельне з перемиканням
     *  *

Способи реалізації асинхронності:
 - Багатозадачність - засоби мови програмування (Task, Future, Promise)
 - Багатопоточність - засоби операційної системи (за наявності)
 - Багатопроцесність - засоби операційної системи
 - Мережні (grid, network) технології

async fun(){...}
res = await fun(); X

task = fun();
task2 = fun2();
other work
res = await task;  !
res2 = await task2;  !


res = await fun().then(...).then(...); ?
fun().then(...).then(...).then( res => ... ); !   Нитка (Continuations)
fun2().then(.2.).then(.2.).then( res2 => ... )
*/