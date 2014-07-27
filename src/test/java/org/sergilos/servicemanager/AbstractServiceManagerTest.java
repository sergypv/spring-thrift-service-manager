package org.sergilos.servicemanager;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransportException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sergilos.servicemanager.remote.test.MathTestServiceAddition;
import org.sergilos.servicemanager.remote.test.MathTestServiceSubtraction;
import org.springframework.context.support.GenericApplicationContext;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Sergio Pereira
 * @since Jul 2014
 */
public abstract class AbstractServiceManagerTest {
    protected static final String SERVICE_PACKAGE = "org.sergilos.servicemanager.remote.test";
    protected ServiceServerManager serviceServerManager;

    @Before
    public void setup() throws TTransportException {
        String serviceNames = "MathService,MathService,ThreadService";
        String serviceInterfaces = SERVICE_PACKAGE + ".MathTestServiceAddition," + SERVICE_PACKAGE + ".MathTestServiceSubtraction," + SERVICE_PACKAGE + ".ThreadTestService";
        String serviceImplementations = SERVICE_PACKAGE + ".MathServiceAdditionImpl," + SERVICE_PACKAGE + ".MathServiceSubtractionImpl," + SERVICE_PACKAGE + ".ThreadServiceImpl";
        String servicePorts = "10900,10900,10901";

        serviceServerManager = new ServiceServerManager(serviceNames, serviceInterfaces, serviceImplementations, servicePorts, false, getServerWrapperFactory());
        serviceServerManager.setApplicationContext(new GenericApplicationContext());
        serviceServerManager.startupServer();
    }

    @After
    public void tearDown() {
        serviceServerManager.stopServices();
    }

    private void setupLiveServiceByXml() throws TTransportException, FileNotFoundException, XPathExpressionException, ParserConfigurationException,
            SAXException, IOException {
        serviceServerManager = new ServiceServerManager(getClass().getResource("/testServiceConfiguration.xml").getPath(), getServerWrapperFactory());
        setupLiveServer();
    }

    @Test
    public void getServicesList() {
        Collection<AbstractRunnableServiceWrapper> serverList = serviceServerManager.getSerivesList();
        Assert.assertEquals(2, serverList.size());
    }

    @Test
    public void getService() {
        Assert.assertNotNull(serviceServerManager.getService("MathService"));
        Assert.assertNull(serviceServerManager.getService("AnotherService"));
        Assert.assertNull(serviceServerManager.getService(null));
    }

    @Test
    public void testInitializedServicesWithXML() throws TException, FileNotFoundException, XPathExpressionException, ParserConfigurationException,
            SAXException, IOException {
        setupLiveServiceByXml();
        testServices(10904);
    }

    @Test
    public void testInitializedServices() throws TException {
        setupLiveServiceByCfg();
        testServices(10902);
    }

    private void setupLiveServiceByCfg() throws TTransportException {
        String serviceNames = "MathService,MathService,ThreadService";
        String serviceInterfaces = SERVICE_PACKAGE + ".MathTestServiceAddition," + SERVICE_PACKAGE + ".MathTestServiceSubtraction," + SERVICE_PACKAGE + ".ThreadTestService";
        String serviceImplementations = SERVICE_PACKAGE + ".MathServiceAdditionImpl," + SERVICE_PACKAGE + ".MathServiceSubtractionImpl," + SERVICE_PACKAGE + ".ThreadServiceImpl";
        String servicePorts = "10902,10902,10903";

        serviceServerManager = new ServiceServerManager(serviceNames, serviceInterfaces, serviceImplementations, servicePorts, true, getServerWrapperFactory());
        setupLiveServer();
    }

    protected void testServices(int port) throws TException {
        AbstractRunnableServiceWrapper.ServiceWrapperFactory clientWrapperFactory = getClientWrapperFactory();
        TProtocol additionProtocol = clientWrapperFactory.getClientProtocol(SERVICE_PACKAGE + ".MathTestServiceAddition", "localhost", port);
        TProtocol subtractionProtocol = clientWrapperFactory.getClientProtocol(SERVICE_PACKAGE + ".MathTestServiceSubtraction", "localhost", port);

        MathTestServiceAddition.Client mathAdditionClient = new MathTestServiceAddition.Client(additionProtocol);
        MathTestServiceSubtraction.Client mathSubtractionClient = new MathTestServiceSubtraction.Client(subtractionProtocol);

        Assert.assertEquals(300, mathAdditionClient.testingSum(100, 200));
        Assert.assertEquals(-100, mathSubtractionClient.testingSubtract(100, 200));

        additionProtocol.getTransport().close();
        subtractionProtocol.getTransport().close();
    }

    protected void setupLiveServer() throws TTransportException {
        serviceServerManager.setApplicationContext(new GenericApplicationContext());
        serviceServerManager.startupServer();
        synchronized (this) {
            try {
                // Wait for servers to come-up
                this.wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
    }

    protected abstract AbstractRunnableServiceWrapper.ServiceWrapperFactory getServerWrapperFactory();

    protected abstract AbstractRunnableServiceWrapper.ServiceWrapperFactory getClientWrapperFactory();
}
