package itstep.learning.ioc;

import com.google.inject.servlet.ServletModule;
import itstep.learning.filters.*;
import itstep.learning.servlets.*;


public class ServletConfig extends ServletModule {

    @Override
    protected void configureServlets() {
        // реєстрація фільтра - всі запити (/*) будуть проходити
        // через цей фільтр (перед переходом до сервлетів)
        filter( "/*" ).through( CharsetFilter.class );
        // filter( "/*" ).through( AuthFilter.class    );
        filter( "/*" ).through( AuthJwtFilter.class );
        
        // !! Для усіх сервлетів у проєкті
        // - прибираємо анотацію @WebServlet
        // - додаємо анотацію @Singleton
        serve( "/home" ).with( HomeServlet.class );
        serve( "/user" ).with( UserServlet.class );
        serve( "/product" ).with( ProductServlet.class );
    }
    
}
