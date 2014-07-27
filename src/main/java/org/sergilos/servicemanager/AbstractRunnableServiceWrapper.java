package org.sergilos.servicemanager;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractRunnableServiceWrapper implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRunnableServiceWrapper.class);

    protected TServer server;
    protected String serviceName;
    private ApplicationContext applicationContext;
    private TMultiplexedProcessor multiplexedProcessor;

    public AbstractRunnableServiceWrapper(ApplicationContext applicationContext, String serviceName) {
        LOGGER.debug("Service setup: {}", serviceName);
        this.multiplexedProcessor = new TMultiplexedProcessor();
        this.applicationContext = applicationContext;
        this.serviceName = serviceName;
    }

    public void addProcessor(String serviceHandlerClass, String processorNameClass) {
        multiplexedProcessor.registerProcessor(processorNameClass, getProcess(serviceHandlerClass, processorNameClass));
    }

    public void initializeServer() throws TTransportException {
        server = getServer(multiplexedProcessor);
    }

    @SuppressWarnings("unchecked")
    private <T extends TProcessor> T getProcess(String serviceHandlerClass, String processorNameClass) {
        T processor = null;
        try {
            Class<?> handlerClass = Class.forName(serviceHandlerClass);
            Class<T> processorClass = (Class<T>) getProcessorClass(Class.forName(processorNameClass));
            Constructor<T> processorConstructor = (Constructor<T>) processorClass.getConstructors()[0];
            processor = processorConstructor.newInstance(getHandlerInstance(handlerClass));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.error("Error instantiating processor", e);
        }

        return processor;
    }

    private <T> T getHandlerInstance(Class<T> handler) throws InstantiationException, IllegalAccessException {
        T handlerInstance = handler.newInstance();
        if (this.applicationContext != null) {
            LOGGER.debug("Autowiring {}", handlerInstance.getClass().getSimpleName());
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(handlerInstance);
        }
        return handlerInstance;
    }

    private Class<?> getProcessorClass(Class<?> processorOuterClass) {
        for (Class<?> referencedClass : processorOuterClass.getClasses()) {
            if (referencedClass.getSimpleName().equals("Processor")) {
                return referencedClass;
            }
        }

        return null;
    }

    @Override
    public void run() {
        LOGGER.info("Start Service '{}'", serviceName);
        try {
            initializeServer();
            this.startService();
        } catch (TTransportException e) {
            LOGGER.error("Error initializing service " + serviceName, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception on Thirft Service", e);
        } finally {
            LOGGER.debug("Service thread closing {}", serviceName);
        }
    }

    public void stopService() {
        if (server != null && server.isServing()) {
            LOGGER.debug("Stop Service thread {}", serviceName);
            server.stop();
        }
    }

    public void startService() {
        if (!server.isServing()) {
            server.serve();
        }
    }

    protected abstract TServer getServer(TProcessor processor) throws TTransportException;

    public static abstract class ServiceWrapperFactory {
        public abstract AbstractRunnableServiceWrapper getServiceWrapper(ApplicationContext applicationContext,
                                                                         String serviceName, Integer port);
    }
}
