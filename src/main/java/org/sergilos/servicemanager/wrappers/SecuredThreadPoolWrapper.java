package org.sergilos.servicemanager.wrappers;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.sergilos.servicemanager.AbstractRunnableServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public class SecuredThreadPoolWrapper extends AbstractRunnableServiceWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredThreadPoolWrapper.class);

    private int remotePort;
    private String keystoreFile;
    private String keystorePass;

    private SecuredThreadPoolWrapper(ApplicationContext applicationContext, String serviceName, int remotePort,
                                    String keystoreFile, String keystorePass) {
        super(applicationContext, serviceName);
        LOGGER.debug("Service setup: {}", serviceName);
        this.remotePort = remotePort;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
    }

    @Override
    protected TServer getServer(TProcessor processor) throws TTransportException {
        LOGGER.debug("Setting Secured Server on port {} and keystore", remotePort, keystoreFile);

        TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
        params.setKeyStore(keystoreFile, keystorePass);

        TServerSocket serverTransport;
        try {
            serverTransport = TSSLTransportFactory.getServerSocket(remotePort, 1000, InetAddress.getByName("localhost"), params);
        } catch (UnknownHostException e) {
            throw new TTransportException(e);
        }

        return new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
    }

    public static class SecuredThreadPoolWrapperFactory extends ServiceWrapperFactory {
        private String keystoreFile;
        private String keystorePass;
        private String truststoreFile;
        private String truststorePass;

        public SecuredThreadPoolWrapperFactory(String keystoreFile, String keystorePass, String truststoreFile, String truststorePass) {
            this.keystoreFile = keystoreFile;
            this.keystorePass = keystorePass;
            this.truststoreFile = truststoreFile;
            this.truststorePass = truststorePass;
        }

        public static SecuredThreadPoolWrapperFactory getServerInstance(String keystoreFile, String keystorePass) {
            return new SecuredThreadPoolWrapperFactory(keystoreFile, keystorePass, null, null);
        }

        public static SecuredThreadPoolWrapperFactory getClientInstance(String truststoreFile, String truststorePass) {
            return new SecuredThreadPoolWrapperFactory(null, null, truststoreFile, truststorePass);
        }

        @Override
        public AbstractRunnableServiceWrapper getServiceServerWrapper(ApplicationContext applicationContext,
                                                                      String serviceName, Integer port) {
            return new SecuredThreadPoolWrapper(applicationContext, serviceName, port, keystoreFile, keystorePass);
        }

        @Override
        public TProtocol getClientProtocol(String serviceInterfaceClassName, String host, Integer port) throws TTransportException {
            if(truststoreFile == null || truststorePass == null){
                throw new IllegalStateException("truststore not defined. Initialize the Factory properly");
            }

            TSSLTransportFactory.TSSLTransportParameters paramsClient = new TSSLTransportFactory.TSSLTransportParameters();
            paramsClient.setTrustStore(truststoreFile, truststorePass);
            TTransport transport = TSSLTransportFactory.getClientSocket(host, port, 1000, paramsClient);
            TProtocol protocol = new TBinaryProtocol(transport);

            return new TMultiplexedProtocol(protocol, serviceInterfaceClassName);
        }
    }
}
