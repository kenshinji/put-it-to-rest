package org.zalando.putittorest;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.getDefaultHostnameVerifier;

@Component
class HttpClientFactoryBean implements FactoryBean<HttpClient> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactoryBean.class);

    private final HttpClientBuilder builder = HttpClientBuilder.create();
    private final RequestConfig.Builder config = RequestConfig.custom();
    private HttpClientCustomizer customizer = $ -> {
    };

    public void setFirstRequestInterceptors(final List<HttpRequestInterceptor> interceptors) {
        interceptors.forEach(builder::addInterceptorFirst);
    }

    public void setLastRequestInterceptors(final List<HttpRequestInterceptor> interceptors) {
        interceptors.forEach(builder::addInterceptorLast);
    }

    public void setLastResponseInterceptors(final List<HttpResponseInterceptor> interceptors) {
        interceptors.forEach(builder::addInterceptorLast);
    }

    public void setConnectionTimeout(final TimeSpan connectTimeout) {
        config.setConnectTimeout((int) connectTimeout.to(TimeUnit.MILLISECONDS));
    }

    public void setSocketTimeout(final TimeSpan socketTimeout) {
        config.setSocketTimeout((int) socketTimeout.to(TimeUnit.MILLISECONDS));
    }

    public void setConnectionTimeToLive(final TimeSpan timeToLive) {
        builder.setConnectionTimeToLive(timeToLive.getAmount(), timeToLive.getUnit());
    }

    public void setMaxConnectionsPerRoute(final int maxConnectionsPerRoute) {
        builder.setMaxConnPerRoute(maxConnectionsPerRoute);
    }

    public void setMaxConnectionsTotal(final int maxConnectionsTotal) {
        builder.setMaxConnTotal(maxConnectionsTotal);
    }

    public void setTrustedKeystore(final RestSettings.Keystore keystore) throws Exception {
        final SSLContextBuilder ssl = SSLContexts.custom();

        final String path = keystore.getPath();
        final String password = keystore.getPassword();

        final URL resource = HttpClientFactoryBean.class.getClassLoader().getResource(path);

        if (resource == null) {
            throw new FileNotFoundException(format("Keystore [%s] not found.", path));
        }

        try {
            ssl.loadTrustMaterial(resource, password == null ? null : password.toCharArray());
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(ssl.build(), getDefaultHostnameVerifier()));
        } catch (final Exception e) {
            LOG.error("Error loading keystore [{}]:", path, e); // log full exception, bean initialization code swallows it
            throw e;
        }
    }

    public void setCustomizer(final HttpClientCustomizer customizer) {
        this.customizer = customizer;
    }

    @Override
    public CloseableHttpClient getObject() {
        builder.setDefaultRequestConfig(config.build());
        customizer.customize(builder);
        return builder.build();
    }

    @Override
    public Class<?> getObjectType() {
        return CloseableHttpClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
