package org.sergilos.servicemanager;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ServiceThreadPoolWrapper extends AbstractRunnableServiceWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceThreadPoolWrapper.class);

    private int remotePort;
    private int numSelectorThreads;
    private int numWorkerThreads;

    private ServiceThreadPoolWrapper(ApplicationContext applicationContext, String serviceName, int remotePort,
                                    int numSelectorThreads, int numWorkerThreads) {
        super(applicationContext, serviceName);
        LOGGER.debug("Service setup: {}", serviceName);

        this.remotePort = remotePort;
        this.numSelectorThreads = numSelectorThreads;
        this.numWorkerThreads = numWorkerThreads;
    }

    @Override
    protected TServer getServer(TProcessor processor) throws TTransportException {
        LOGGER.debug("Setting Server with {} selector threads and {} worker threads", numSelectorThreads, numWorkerThreads);

        TNonblockingServerSocket transport = new TNonblockingServerSocket(remotePort);

        TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(transport);
        args.transportFactory(new TFramedTransport.Factory());
        args.protocolFactory(new TBinaryProtocol.Factory());
        args.processor(processor);
        args.selectorThreads(numSelectorThreads);
        args.workerThreads(numWorkerThreads);

        return new TThreadedSelectorServer(args);
    }

    public static class ServiceThreadPoolWrapperFactory extends ServiceWrapperFactory {
        private int numSelectorThreads;
        private int numWorkerThreads;

        public ServiceThreadPoolWrapperFactory(int numSelectorThreads, int numWorkerThreads) {
            this.numSelectorThreads = numSelectorThreads;
            this.numWorkerThreads = numWorkerThreads;
        }

        @Override
        public AbstractRunnableServiceWrapper getServiceWrapper(ApplicationContext applicationContext, String serviceName, Integer port) {
            return new ServiceThreadPoolWrapper(applicationContext, serviceName, port, numSelectorThreads, numWorkerThreads);
        }
    }
}
