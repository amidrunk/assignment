package encube.assignment;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class DatabaseReset implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var ctx = SpringExtension.getApplicationContext(context);
        var db = ctx.getBean(DatabaseClient.class);

        // Truncate all user-defined tables in the public schema, taking into account that there can be foreign key constraints
        db.sql("""
                DO $$ DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
                        EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE';
                    END LOOP;
                END $$;
                """)
          .then()
          .block();
    }
}
