package cn.org.shelly.edu.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 启动信息打印
 * @author shelly
 */
@Component
@Slf4j
public class ApiDocStartupPrinter implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port}")
    private String port;

    @Value("${spring.application.name}")
    private String appName;


    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        try {
            System.out.println("\n----------------------------------------------------------");
            System.out.println("应用 '" + appName + "' 启动成功!");
            System.out.println("Swagger/Knife4j文档: \thttp://localhost:" + port + "/doc.html");
            System.out.println("----------------------------------------------------------\n");
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
