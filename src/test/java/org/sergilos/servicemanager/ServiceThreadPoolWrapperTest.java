package org.sergilos.servicemanager;

/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public class ServiceThreadPoolWrapperTest extends AbstractServiceManagerTest {
    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getServerWrapperFactory() {
        return new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory(1, 1);
    }

    @Override
    protected AbstractRunnableServiceWrapper.ServiceWrapperFactory getClientWrapperFactory() {
        return new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory();
    }
}
