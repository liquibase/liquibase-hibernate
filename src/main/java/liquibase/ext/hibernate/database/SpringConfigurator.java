package liquibase.ext.hibernate.database;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.util.ReflectHelper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class SpringConfigurator {
	private static final Class<LocalSessionFactoryBean> FACTORY_BEAN_CLASS = LocalSessionFactoryBean.class;
	public static final String SPRING_BEAN = "bean";
	public static final String SPRING_BEAN_CLASS = "beanClass";
	private final ConfigLocator locator;

	public SpringConfigurator(ConfigLocator locator) {
		this.locator = locator;
	}

	public Configuration createSpringConfiguration() {
		Configuration config = new Configuration();
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.loadBeanDefinitions(new FileSystemResource(locator.getPath()));

		Properties props = locator.getProperties();
		Class<? extends LocalSessionFactoryBean> beanClass = FACTORY_BEAN_CLASS;

		String beanName = props.getProperty(SPRING_BEAN, null);
		String beanClassName = props.getProperty(SPRING_BEAN_CLASS, null);

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
		PropertyValue annotatedClassesProp = properties
				.getPropertyValue("annotatedClasses");
		List<TypedStringValue> annotatedClasses = (List<TypedStringValue>) annotatedClassesProp
				.getValue();
		if (annotatedClasses != null) {
			for (TypedStringValue classname : annotatedClasses) {
				config.addAnnotatedClass(findClass(classname.getValue()));
			}
		}

		// Add properties
		ManagedProperties hibernateProps = (ManagedProperties) properties
				.getPropertyValue("hibernateProperties").getValue();

		if (hibernateProps != null) {
			Properties configProps = new Properties();
			for (Map.Entry<?, ?> entry : hibernateProps.entrySet()) {
				TypedStringValue key = (TypedStringValue) entry.getKey();
				TypedStringValue value = (TypedStringValue) entry.getValue();

				configProps.setProperty(key.getValue(), value.getValue());
			}

			config.setProperties(configProps);
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
