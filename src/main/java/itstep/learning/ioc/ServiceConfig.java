package itstep.learning.ioc;

import com.google.inject.AbstractModule;
import itstep.learning.services.config.ConfigService;
import itstep.learning.services.config.JsonConfigService;
import itstep.learning.services.db.DbService;
import itstep.learning.services.db.MySqlDbService;
import itstep.learning.services.form_parse.FormParseService;
import itstep.learning.services.form_parse.MixedFormParseService;
import itstep.learning.services.hash.HashService;
import itstep.learning.services.hash.Md5HashService;
import itstep.learning.services.kdf.KdfService;
import itstep.learning.services.kdf.PbKdf1Service;
import itstep.learning.services.random.RandomService;
import itstep.learning.services.random.UtilRandomService;


public class ServiceConfig extends AbstractModule {

    @Override
    protected void configure() {
        bind( HashService.class   ).to( Md5HashService.class    );
        bind( KdfService.class    ).to( PbKdf1Service.class     );
        bind( DbService.class     ).to( MySqlDbService.class    );
        bind( RandomService.class ).to( UtilRandomService.class );
        bind( ConfigService.class ).to( JsonConfigService.class );
        bind( FormParseService.class ).to( MixedFormParseService.class );
    }
    
}
