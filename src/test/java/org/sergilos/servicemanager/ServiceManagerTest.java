package org.sergilos.servicemanager;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
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
public class ServiceManagerTest {
    private static final String SERVICE_PACKAGE = "org.sergilos.servicemanager.remote.test";
    private ServiceManager serviceManager;

    @Before
    public void setup() throws TTransportException {
        String serviceNames = "MathService,MathService,ThreadService";
        String serviceInterfaces = SERVICE_PACKAGE + ".MathTestServiceAddition," + SERVICE_PACKAGE + ".MathTestServiceSubtraction," + SERVICE_PACKAGE + ".ThreadTestService";
        String serviceImplementations = SERVICE_PACKAGE + ".MathServiceAdditionImpl," + SERVICE_PACKAGE + ".MathServiceSubtractionImpl," + SERVICE_PACKAGE + ".ThreadServiceImpl";
        String servicePorts = "10900,10900,10901";

        serviceManager = new ServiceManager(serviceNames, serviceInterfaces, serviceImplementations, servicePorts, false, new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory(1, 1));
        serviceManager.setApplicationContext(new GenericApplicationContext());
        serviceManager.startupServer();
    }

    private void setupLiveServer() throws TTransportException {
        serviceManager.setApplicationContext(new GenericApplicationContext());
        serviceManager.startupServer();
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

    private void setupLiveServiceByCfg() throws TTransportException {
        String serviceNames = "MathService,MathService,ThreadService";
        String serviceInterfaces = SERVICE_PACKAGE + ".MathTestServiceAddition," + SERVICE_PACKAGE + ".MathTestServiceSubtraction," + SERVICE_PACKAGE + ".ThreadTestService";
        String serviceImplementations = SERVICE_PACKAGE + ".MathServiceAdditionImpl," + SERVICE_PACKAGE + ".MathServiceSubtractionImpl," + SERVICE_PACKAGE + ".ThreadServiceImpl";
        String servicePorts = "10902,10902,10903";

        serviceManager = new ServiceManager(serviceNames, serviceInterfaces, serviceImplementations, servicePorts, true, new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory(1, 1));
        setupLiveServer();
    }

    private void setupLiveServiceByXml() throws TTransportException, FileNotFoundException, XPathExpressionException, ParserConfigurationException,
            SAXException, IOException {
        serviceManager = new ServiceManager(getClass().getResource("/testServiceConfiguration.xml").getPath(), new ServiceThreadPoolWrapper.ServiceThreadPoolWrapperFactory(1, 1));
        setupLiveServer();
    }

    @Test
    public void testInitializedServices() throws TException {
        setupLiveServiceByCfg();
        testServices(10902);
        tearDown();
    }

    private void testServices(int port) throws TException {
        TTransport transport = new TFramedTransport(new TSocket("localhost", port));
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        TMultiplexedProtocol multiplexProtocolAddition = new TMultiplexedProtocol(protocol, SERVICE_PACKAGE + ".MathTestServiceAddition");
        MathTestServiceAddition.Client mathAdditionClient = new MathTestServiceAddition.Client(multiplexProtocolAddition);
        TMultiplexedProtocol multiplexProtocolSubtraction = new TMultiplexedProtocol(protocol, SERVICE_PACKAGE + ".MathTestServiceSubtraction");
        MathTestServiceSubtraction.Client mathSubtractionClient = new MathTestServiceSubtraction.Client(multiplexProtocolSubtraction);

        Assert.assertEquals(300, mathAdditionClient.testingSum(100, 200));
        Assert.assertEquals(-100, mathSubtractionClient.testingSubtract(100, 200));
    }

    @Test
    public void getSerivesList() {
        Collection<AbstractRunnableServiceWrapper> serverList = serviceManager.getSerivesList();
        Assert.assertEquals(2, serverList.size());
    }

    @Test
    public void getService() {
        Assert.assertNotNull(serviceManager.getService("MathService"));
        Assert.assertNull(serviceManager.getService("AnotherService"));
        Assert.assertNull(serviceManager.getService(null));
    }

    public void tearDown() {
        serviceManager.stopServices();
    }

    @Test
    public void testInitializedServicesWithXML() throws TException, FileNotFoundException, XPathExpressionException, ParserConfigurationException,
            SAXException, IOException {
        setupLiveServiceByXml();
        testServices(10904);
        tearDown();
    }
}
