package com.litochina.cloud;


import com.litochina.base.common.common.BaseConstant;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.litochina")
//@MapperScan("com.qxaiot.**.mapper")
@EnableScheduling
public class CloudServerApplication {

    public static void main(String[] args) {
        System.setProperty("jasypt.encryptor.password", BaseConstant.JASYPT_PASSWORD);
        new SpringApplicationBuilder(CloudServerApplication.class).beanNameGenerator(new CustomBeanNameGenerator()).run(args);
    }

    public static class CustomBeanNameGenerator implements BeanNameGenerator {

        @Override
        public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry beanDefinitionRegistry) {
            return beanDefinition.getBeanClassName();
        }
    }
}
