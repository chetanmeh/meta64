package com.meta64.mobile.webconsole;

import javax.annotation.PostConstruct;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import com.meta64.mobile.repo.OakRepository;
import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.felix.http.proxy.ProxyServlet;
import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.jackrabbit.oak.run.osgi.ServiceRegistryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Integrates the Felix WebConsole support with servlet container. The console is
 * accessible at /osgi/system/console/
 */
@Configuration
@Component
public class WebConsoleSupport {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Adding a dependency to ensure that by the time servlet
     * is registered BundleContext is set in ServletContext
     */
    @Autowired
    private OakRepository repository;

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    private void postConstruct() throws Exception {
        PojoServiceRegistry reg = ((ServiceRegistryProvider)repository.getRepository()).getServiceRegistry();

        //Configure repository backed SecurityProvider
        reg.registerService(WebConsoleSecurityProvider.class.getName(), new RepositorySecurityProvider(), null);

        //Expose the Spring Application context to Script Console access
        reg.registerService(ApplicationContext.class.getName(), context, null);
    }

    @Bean
    public ServletRegistrationBean felixProxyServlet() {
        return new ServletRegistrationBean(new ProxyServlet(), "/osgi/*");
    }

    /**
     * A simple WebConsoleSecurityProvider implementation which only allows
     * repository admin user to perform login
     */
    private class RepositorySecurityProvider implements WebConsoleSecurityProvider {
        @Override
        public Object authenticate(String userName, String password) {
            final Credentials creds = new SimpleCredentials(userName,
                    (password == null) ? new char[0] : password.toCharArray());
            Session session = null;
            try {
                session = repository.getRepository().login(creds);

                if ("admin".equals(userName)){
                    return userName;
                }

            } catch (LoginException re) {
                log.info("authenticate: User {} failed to authenticate with the repository " +
                        "for Web Console access", userName, re);
            } catch (RepositoryException re) {
                log.info("authenticate: Generic problem trying grant User {} access to the Web Console", userName, re);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
            return null;
        }

        @Override
        public boolean authorize(Object user, String role) {
            //No fine grained access control for now
            return true;
        }
    }
}
