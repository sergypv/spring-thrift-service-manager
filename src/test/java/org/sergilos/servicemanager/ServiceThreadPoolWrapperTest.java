package org.sergilos.servicemanager;

import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;


/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public class ServiceThreadPoolWrapperTest extends AbstractServiceManagerTest {
    @Override
    protected TTransport getTransport(int port) throws TTransportException {
        TSocket localhostSocket = new TSocket("localhost", port);
        localhostSocket.open();

        return new TFramedTransport(localhostSocket);
    }

    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getWrapperFactory() {
        return new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory(1, 1);
    }
}
