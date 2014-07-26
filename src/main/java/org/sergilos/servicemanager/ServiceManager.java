package org.sergilos.servicemanager;

import com.google.common.collect.ImmutableSet;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

public class ServiceManager implements ApplicationContextAware {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);

	public final static String CONFIG_SEPARATOR = ",";

	private Map<String, AbstractRunnableServiceWrapper> serviceMap = new HashMap<String, AbstractRunnableServiceWrapper>();
	private Map<String, Thread> threadServiceMap = new HashMap<String, Thread>();
	private ApplicationContext applicationContext;
    private AbstractRunnableServiceWrapper.ServiceWrapperFactory serviceWrapperFactory;

	private List<String> serviceNamesList;
	private List<String> serviceInterfacesList;
	private List<String> serviceImplementationsList;
	private List<String> servicePortsList;
	private boolean startServices = true;

	public ServiceManager(String xmlConfigurationLocation, AbstractRunnableServiceWrapper.ServiceWrapperFactory serviceWrapperFactory) {
		this.serviceNamesList = new ArrayList<>();
		this.serviceInterfacesList = new ArrayList<>();
		this.serviceImplementationsList = new ArrayList<>();
		this.servicePortsList = new ArrayList<>();
		this.startServices = true;
        this.serviceWrapperFactory = serviceWrapperFactory;

		try {
			initializeByXml(xmlConfigurationLocation);
		} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalArgumentException("Error loading configuration from xml " + xmlConfigurationLocation, e);
		}
	}

	private void initializeByXml(String xmlConfigurationLocation) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		LOGGER.info("Initializing by xml: " + xmlConfigurationLocation);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		InputStream inputStream = new FileInputStream(xmlConfigurationLocation);
		Document document = builder.parse(inputStream);
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList serviceNodes = (NodeList) xPath.compile("/thirftServiceConfiguration/service").evaluate(document, XPathConstants.NODESET);
		for (int i = 0; serviceNodes != null && i < serviceNodes.getLength(); i++) {
			Node serviceNode = serviceNodes.item(i);
			if (serviceNode.getNodeType() == Node.ELEMENT_NODE) {
				NamedNodeMap serviceAttributes = serviceNode.getAttributes();
				NodeList serviceDefinitionNodes = serviceNode.getChildNodes();

				for (int x = 0; serviceDefinitionNodes != null && x < serviceDefinitionNodes.getLength(); x++) {
					Node serviceDefinitionNode = serviceDefinitionNodes.item(x);
					if (serviceDefinitionNode.getNodeType() == Node.ELEMENT_NODE) {
						NamedNodeMap serviceDefinitionAttributes = serviceDefinitionNode.getAttributes();
						String serviceName = serviceAttributes.getNamedItem("name").getNodeValue();
						String servicePort = serviceAttributes.getNamedItem("port").getNodeValue();
						String serviceInterface = serviceDefinitionAttributes.getNamedItem("interface").getNodeValue();
						String serviceImplementation = serviceDefinitionAttributes.getNamedItem("implementation").getNodeValue();
						this.serviceNamesList.add(serviceName);
						this.servicePortsList.add(servicePort);
						this.serviceInterfacesList.add(serviceInterface);
						this.serviceImplementationsList.add(serviceImplementation);
						LOGGER.debug("Service entry: {}:{} [ {}->{} ]", serviceName, servicePort, serviceInterface, serviceImplementation);
					}
				}
			}
		}
	}

	public ServiceManager(String serviceNamesString, String serviceInterfacesString, String serviceImplementationsString, String servicePortsString,
                          AbstractRunnableServiceWrapper.ServiceWrapperFactory serviceWrapperFactory) {
		if (serviceNamesString == null || serviceInterfacesString == null || serviceImplementationsString == null || servicePortsString == null) {
			throw new IllegalArgumentException("One or more parameters are null, but all the ServiceManager parameters are mandatory");
		}

		String[] serviceNames = serviceNamesString.split(CONFIG_SEPARATOR);
		String[] serviceInterfaces = serviceInterfacesString.split(CONFIG_SEPARATOR);
		String[] serviceImplementations = serviceImplementationsString.split(CONFIG_SEPARATOR);
		String[] servicePorts = servicePortsString.split(CONFIG_SEPARATOR);

		this.serviceNamesList = Arrays.asList(serviceNames);
		this.serviceInterfacesList = Arrays.asList(serviceInterfaces);
		this.serviceImplementationsList = Arrays.asList(serviceImplementations);
		this.servicePortsList = Arrays.asList(servicePorts);
        this.serviceWrapperFactory = serviceWrapperFactory;
    }

	public ServiceManager(String serviceNamesString, String serviceInterfacesString, String serviceImplementationsString, String servicePortsString,
                          boolean startServices, AbstractRunnableServiceWrapper.ServiceWrapperFactory serviceWrapperFactory) {
		this(serviceNamesString, serviceInterfacesString, serviceImplementationsString, servicePortsString, serviceWrapperFactory);
		this.startServices = startServices;
        this.serviceWrapperFactory = serviceWrapperFactory;
	}

	public ServiceManager(List<String> serviceNamesList, List<String> serviceInterfacesList, List<String> serviceImplementationsList,
                          List<String> servicePortsList, AbstractRunnableServiceWrapper.ServiceWrapperFactory serviceWrapperFactory) {
		if (serviceNamesList == null || serviceImplementationsList == null || serviceImplementationsList == null || servicePortsList == null) {
			throw new IllegalArgumentException("One or more parameters are null, but all the ServiceManager parameters are mandatory");
		}

		this.serviceNamesList = serviceNamesList;
		this.serviceInterfacesList = serviceInterfacesList;
		this.serviceImplementationsList = serviceImplementationsList;
		this.servicePortsList = servicePortsList;
        this.serviceWrapperFactory = serviceWrapperFactory;
	}

	public void startupServer() throws TTransportException {
		LOGGER.debug("Startup ServiceManager Server");
		initializeServiceMap(serviceNamesList, serviceInterfacesList, serviceImplementationsList, servicePortsList);

		threadServiceMap.clear();
		for (Entry<String, AbstractRunnableServiceWrapper> serviceEntry : serviceMap.entrySet()) {
			Thread serviceThread = new Thread(serviceEntry.getValue());
			threadServiceMap.put(serviceEntry.getKey(), serviceThread);
			if (this.startServices) {
				LOGGER.debug("Initializing created Thrift services");
				serviceThread.start();
			}
		}
	}

	public void stopServices() {
		LOGGER.debug("Stopping ServiceManager Server");
		for (Entry<String, AbstractRunnableServiceWrapper> serviceEntry : serviceMap.entrySet()) {
			serviceEntry.getValue().stopService();
		}
	}

	public Collection<AbstractRunnableServiceWrapper> getSerivesList() {

		return ImmutableSet.<AbstractRunnableServiceWrapper> builder().addAll(serviceMap.values()).build();
	}

	public AbstractRunnableServiceWrapper getService(String serviceName) {
		return serviceMap.get(serviceName);
	}

	private void initializeServiceMap(List<String> serviceNamesList, List<String> serviceInterfacesList, List<String> serviceImplementationsList,
			List<String> servicePortsList) throws TTransportException {

		if (serviceInterfacesList.isEmpty() || serviceInterfacesList.size() != serviceImplementationsList.size()
				|| serviceInterfacesList.size() != servicePortsList.size() || serviceInterfacesList.size() != serviceNamesList.size()) {
			throw new IllegalArgumentException(
					String.format(
                            "Invalid service definition. Expecting four not empty lists of the same size, but got: Names: %s \nInterfaces: %s \nImplementations: %s \nPorts: %s",
                            serviceNamesList, serviceInterfacesList, serviceImplementationsList, servicePortsList));
		}

		for (int i = 0; i < serviceInterfacesList.size(); i++) {
			String serviceName = serviceNamesList.get(i).trim();
			AbstractRunnableServiceWrapper serviceWrapper = serviceMap.get(serviceName);
			if (serviceWrapper == null) {
				serviceWrapper = serviceWrapperFactory.getServiceWrapper(applicationContext, serviceName, new Integer(servicePortsList.get(i).trim()));
				serviceMap.put(serviceName, serviceWrapper);
				LOGGER.debug("Initializing service: " + serviceName);
			}

			serviceWrapper.addProcessor(serviceImplementationsList.get(i).trim(), serviceInterfacesList.get(i).trim());
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
