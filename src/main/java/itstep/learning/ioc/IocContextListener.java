package itstep.learning.ioc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class IocContextListener extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(   // ~builder.Services ASP
                new ServletConfig(),
                new ServiceConfig()
        );
    }
    
}
/*
ContextListener - "слухачі" подій створення контексту, тобто
запуску/деплою проєкту. Можуть вважатись точкою входу/запуску
*/