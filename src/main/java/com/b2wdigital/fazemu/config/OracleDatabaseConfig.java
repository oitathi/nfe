package com.b2wdigital.fazemu.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

/**
 * Oracle Database Config.
 *
 * @author Thiago Di Santi {thiago.santi@b2wdigital.com}
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "nfe.database", ignoreUnknownFields = false)
public class OracleDatabaseConfig {

    private String user;
    private String password;
    private String url;
    private String className;
    private Integer initialPoolSize;
    private Integer minPoolSize;
    private Integer maxPoolSize;
    private Integer abandonedConnectionTimeout;
    private Integer inactiveConnectionTimeout;
    private Integer timeoutCheckInterval;
    private Integer maxConnectionReuseTime;
    private Boolean validateConnectionOnBorrow;
    private Integer connectionWaitTimeout;
    private Integer time2LiveConnectionTimeout;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Integer queryTimeout;

    @Bean(name = "dataSource")
    @Primary
    protected DataSource dataSource() throws SQLException {

        PoolDataSource dataSource = PoolDataSourceFactory.getPoolDataSource();
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setURL(url);
        dataSource.setFastConnectionFailoverEnabled(true);

        dataSource.setConnectionFactoryClassName(className);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setAbandonedConnectionTimeout(abandonedConnectionTimeout);
        dataSource.setInactiveConnectionTimeout(inactiveConnectionTimeout);
        dataSource.setTimeoutCheckInterval(timeoutCheckInterval);
        dataSource.setMaxConnectionReuseTime(maxConnectionReuseTime);
        dataSource.setValidateConnectionOnBorrow(validateConnectionOnBorrow);
        dataSource.setConnectionWaitTimeout(connectionWaitTimeout);
        dataSource.setTimeToLiveConnectionTimeout(time2LiveConnectionTimeout);
        dataSource.setConnectionProperty("oracle.net.CONNECT_TIMEOUT", connectTimeout.toString());
        dataSource.setConnectionProperty("oracle.jdbc.ReadTimeout", readTimeout.toString());
        dataSource.setQueryTimeout(queryTimeout);

        return dataSource;
    }

    @Bean(name = "jdbcOperations")
    @Primary
    protected JdbcTemplate jdbcOperations() throws SQLException {
        return new JdbcTemplate(dataSource());
    }

    @Bean(name = "namedParameterJdbcOperations")
    @Primary
    protected NamedParameterJdbcTemplate namedParameterJdbcOperations() throws SQLException {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean(name = "transactionManager")
    protected DataSourceTransactionManager dataSourceTransactionManager() throws SQLException {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean(name = "lobHandler")
    protected LobHandler oracleLobHandler() {
        return new DefaultLobHandler();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(Integer initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getAbandonedConnectionTimeout() {
        return abandonedConnectionTimeout;
    }

    public void setAbandonedConnectionTimeout(Integer abandonedConnectionTimeout) {
        this.abandonedConnectionTimeout = abandonedConnectionTimeout;
    }

    public Integer getInactiveConnectionTimeout() {
        return inactiveConnectionTimeout;
    }

    public void setInactiveConnectionTimeout(Integer inactiveConnectionTimeout) {
        this.inactiveConnectionTimeout = inactiveConnectionTimeout;
    }

    public Integer getTimeoutCheckInterval() {
        return timeoutCheckInterval;
    }

    public void setTimeoutCheckInterval(Integer timeoutCheckInterval) {
        this.timeoutCheckInterval = timeoutCheckInterval;
    }

    public Integer getMaxConnectionReuseTime() {
        return maxConnectionReuseTime;
    }

    public void setMaxConnectionReuseTime(Integer maxConnectionReuseTime) {
        this.maxConnectionReuseTime = maxConnectionReuseTime;
    }

    public Boolean getValidateConnectionOnBorrow() {
        return validateConnectionOnBorrow;
    }

    public void setValidateConnectionOnBorrow(Boolean validateConnectionOnBorrow) {
        this.validateConnectionOnBorrow = validateConnectionOnBorrow;
    }

    public Integer getConnectionWaitTimeout() {
        return connectionWaitTimeout;
    }

    public void setConnectionWaitTimeout(Integer connectionWaitTimeout) {
        this.connectionWaitTimeout = connectionWaitTimeout;
    }

    public Integer getTime2LiveConnectionTimeout() {
        return time2LiveConnectionTimeout;
    }

    public void setTime2LiveConnectionTimeout(Integer time2LiveConnectionTimeout) {
        this.time2LiveConnectionTimeout = time2LiveConnectionTimeout;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(Integer queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

}
