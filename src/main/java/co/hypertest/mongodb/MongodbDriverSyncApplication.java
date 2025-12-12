package co.hypertest.mongodb;

import hypertest.javaagent.HypertestAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import static hypertest.javaagent.bootstrap.util.StringConstantsUtils.APP_STATUS;
import static hypertest.javaagent.bootstrap.util.StringConstantsUtils.UP_STATUS;

@SpringBootApplication
@ComponentScan(basePackages = {"co.hypertest.commonutils", "co.hypertest.mongodb"})
public class MongodbDriverSyncApplication {
    public static void main(String[] args) {
        String serviceIdentifier = "1417d1de-25e9-4888-b0f5-4673cc7c0657";
        String exporterUrl = "http://v2-beta-external.hypertest.co:4319";
        HypertestAgent.start(serviceIdentifier, "ServiceName", "API-KEY", exporterUrl, MongodbDriverSyncApplication.class);

        SpringApplication.run(MongodbDriverSyncApplication.class, args);
    }
}
