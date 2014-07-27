package org.sergilos.servicemanager;

import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public class SecuredThreadPoolWrapperTest extends AbstractServiceManagerTest {
    private static final String TRUST_STORE = "/secured/truststore.jks";
    private static final String TRUST_STORE_PASS = "passCert";
    private static final String KEYSTORE_STORE = "/secured/keystore.jks";

    @Override
    protected TTransport getTransport(int port) throws TTransportException {
        TSSLTransportFactory.TSSLTransportParameters paramsClient = new TSSLTransportFactory.TSSLTransportParameters();
        paramsClient.setTrustStore(getClass().getResource(TRUST_STORE).getPath(), TRUST_STORE_PASS);

        return TSSLTransportFactory.getClientSocket("localhost", port, 1000, paramsClient);
    }

    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getWrapperFactory() {
        return new SecuredThreadPoolWrapper.SecuredThreadPoolWrapperFactory(getClass().getResource(KEYSTORE_STORE).getPath(), TRUST_STORE_PASS);
    }
}