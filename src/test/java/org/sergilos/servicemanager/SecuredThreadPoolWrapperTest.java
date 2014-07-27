package org.sergilos.servicemanager;

/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public class SecuredThreadPoolWrapperTest extends AbstractServiceManagerTest {
    private static final String TRUST_STORE = "/secured/truststore.jks";
    private static final String TRUST_STORE_PASS = "passCert";
    private static final String KEYSTORE_STORE = "/secured/keystore.jks";

    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getServerWrapperFactory() {
        return SecuredThreadPoolWrapper.SecuredThreadPoolWrapperFactory.getServerInstance(getClass().getResource(KEYSTORE_STORE).getPath(), TRUST_STORE_PASS);
    }

    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getClientWrapperFactory() {
        return SecuredThreadPoolWrapper.SecuredThreadPoolWrapperFactory.getClientInstance(getClass().getResource(TRUST_STORE).getPath(), TRUST_STORE_PASS);
    }
}