package com.litochina.cloud.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;

/**
 * @author chenxx
 * @date 2020/8/3/003
 */
@Configuration
@MapperScan(basePackages = "com.litochina.base.persistence.timMapper", sqlSessionTemplateRef = "sqlSessionTemplateTim")
public class TimMapperConfig {

    @Bean(name = "timDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.tim")
    public DataSource TimDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        return dataSource;
    }

    @Bean
    public ServletRegistrationBean druidStatViewServletTim() {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(new StatViewServlet(), "/druid/*");
        registrationBean.addInitParameter("allow", "10.10.9.24"); // IP白名单 (没有配置或者为空，则允许所有访问)
        registrationBean.addInitParameter("deny", ""); // IP黑名单 (存在共同时，deny优先于allow)
        registrationBean.addInitParameter("loginUsername", "root");
        registrationBean.addInitParameter("loginPassword", "cYy3000@");
        registrationBean.addInitParameter("resetEnable", "false");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean druidWebStatViewFilterTim() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(new WebStatFilter());
        registrationBean.addInitParameter("urlPatterns", "/*");
        registrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico,/druid/*");
        return registrationBean;
    }


    @Bean(name = "sqlSessionFactoryTim")
    public SqlSessionFactory sqlSessionFactoryTim(@Qualifier("timDataSource") DataSource dataSource)throws Exception{
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return bean.getObject();
    }

    @Bean(name = "dataSourceTransactionManagerTim")
    public DataSourceTransactionManager dataSourceTransactionManagerTim(@Qualifier("timDataSource") DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "sqlSessionTemplateTim")
    public SqlSessionTemplate sqlSessionTemplateTim(@Qualifier("sqlSessionFactoryTim") SqlSessionFactory sqlSessionFactory)
            throws Exception{
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
