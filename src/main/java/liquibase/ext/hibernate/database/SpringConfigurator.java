package liquibase.ext.hibernate.database;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import liquibase.exception.DatabaseException;

import org.hibernate.cfg.Configuration;
import org.hibernate.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SpringConfigurator {

	private static final Logger LOG = LoggerFactory
			.getLogger(SpringConfigurator.class);

	private final ConfigLocator locator;

	public SpringConfigurator(ConfigLocator locator) {
		this.locator = locator;
	}

	public Configuration createSpringConfiguration() throws IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, ParserConfigurationException,
			DatabaseException {
		Configuration config = new Configuration();

		// Disable xml validation
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setValidating(false);

		// Read configuration
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setNamespaceAware(true);
		reader.loadBeanDefinitions(new ClassPathResource(locator.getPath()));

		Properties props = locator.getProperties();
		Class<? extends LocalSessionFactoryBean> beanClass = LocalSessionFactoryBean.class;

		String beanName = props.getProperty("bean", null);
		String beanClassName = props.getProperty("beanClass", null);

		if (beanClassName != null)
			beanClass = findClass(beanClassName, beanClass);

		if (beanName == null) {
			throw new IllegalStateException(
					"A 'bean' name is required, matching a '" + beanClassName
							+ "' definition in '" + locator.getPath() + "'.");
		}

		BeanDefinition beanDef = registry.getBeanDefinition(beanName);
		if (beanDef == null)
			throw new IllegalStateException("A bean named '" + beanName
					+ "' could not be found in '" + locator.getPath() + "'.");

		MutablePropertyValues properties = beanDef.getPropertyValues();

		// Add annotated classes list.
		PropertyValue annotatedClassesProperty = properties
				.getPropertyValue("annotatedClasses");
		if (annotatedClassesProperty != null) {
			List<TypedStringValue> annotatedClasses = (List<TypedStringValue>) annotatedClassesProperty
					.getValue();
			if (annotatedClasses != null) {
				for (TypedStringValue className : annotatedClasses) {
					LOG.info("Found annotated class " + className.getValue());
					config.addAnnotatedClass(findClass(className.getValue()));
				}
			}
		}

		// Add mapping locations
		PropertyValue mappingLocationsProp = properties
				.getPropertyValue("mappingLocations");
		if (mappingLocationsProp != null) {
			List<TypedStringValue> mappingLocations = (List<TypedStringValue>) mappingLocationsProp
					.getValue();
			if (mappingLocations != null) {
				ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
				for (TypedStringValue mappingLocation : mappingLocations) {
					LOG.info("Found mappingLocation "
							+ mappingLocation.getValue());
					Resource[] resources = resourcePatternResolver
							.getResources(mappingLocation.getValue());
					for (int i = 0; i < resources.length; i++) {
						LOG.info("Adding resource  " + resources[i].getURL());
						try {
							DocumentBuilder documentBuilder = documentBuilderFactory
									.newDocumentBuilder();
							// Disable DTD resolution
							documentBuilder
									.setEntityResolver(new EntityResolver() {

										@Override
										public InputSource resolveEntity(
												String arg0, String arg1)
												throws SAXException,
												IOException {
											return new InputSource(
													new StringReader(""));
										}
									});
							Document document = documentBuilder
									.parse(resources[i].getInputStream());
							config.addDocument(document);
						} catch (SAXException e) {
							throw new DatabaseException(
									"Error reading document "
											+ resources[i].getURL(), e);
						}
					}
				}
			}
		}

		// Add properties
		ManagedProperties hibernateProperties = (ManagedProperties) properties
				.getPropertyValue("hibernateProperties").getValue();

		if (hibernateProperties != null) {
			Properties configurationProperties = new Properties();
			for (Map.Entry<?, ?> entry : hibernateProperties.entrySet()) {
				TypedStringValue key = (TypedStringValue) entry.getKey();
				TypedStringValue value = (TypedStringValue) entry.getValue();

				configurationProperties.setProperty(key.getValue(),
						value.getValue());
			}

			config.setProperties(configurationProperties);
		} else {
			throw new IllegalStateException(
					"Please provide a 'hibernateProperties' property set to define the hibernate connection settings.");
		}

		return config;
	}

	private Class<?> findClass(String className) {
		return findClass(className, Object.class);
	}

	private <T> Class<? extends T> findClass(String className,
			Class<T> superClass) {
		try {
			Class<?> newClass = ReflectHelper.classForName(className);
			if (superClass.isAssignableFrom(newClass)) {
				return newClass.asSubclass(superClass);
			} else {
				throw new IllegalStateException("The provided class '"
						+ className + "' is not assignable from the '"
						+ superClass.getName() + "' superclass.");
			}
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Unable to find required class: '"
					+ className + "'. Please check classpath and class name.");
		}
	}

}
