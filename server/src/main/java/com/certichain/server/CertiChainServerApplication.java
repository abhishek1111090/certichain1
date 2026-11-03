package com.certichain.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. Runs on embedded Tomcat ({@code java -jar certichain-server.war}) or can be
 * dropped into an external Tomcat 10.1 thanks to {@link SpringBootServletInitializer}.
 *
 * <ul>
 *   <li>{@code @ServletComponentScan} picks up the plain {@code @WebServlet}, {@code @WebFilter}
 *       and {@code @WebListener} classes in the {@code web} package.</li>
 *   <li>{@code @ImportResource} loads the classic XML bean definitions in {@code beans.xml}.</li>
 * </ul>
 */
@SpringBootApplication
@ServletComponentScan
@ImportResource("classpath:beans.xml")
@EnableScheduling
@EnableAsync
public class CertiChainServerApplication extends SpringBootServletInitializer {

    static {
        // H2 AUTO_SERVER advertises this address to the desktop client. The default is the
        // Wi-Fi address, which goes stale when the network changes and exposes the database
        // to the LAN; loopback is stable and reachable only from this PC.
        System.setProperty("h2.bindAddress", "127.0.0.1");
    }

    public static void main(String[] args) {
        SpringApplication.run(CertiChainServerApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CertiChainServerApplication.class);
    }
}
