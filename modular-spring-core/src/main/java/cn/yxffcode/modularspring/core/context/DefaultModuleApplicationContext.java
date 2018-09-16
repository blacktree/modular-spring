package cn.yxffcode.modularspring.core.context;

import cn.yxffcode.modularspring.core.ext.DefaultExtensionHandlerInjector;
import cn.yxffcode.modularspring.core.ext.ExtensionHandlerBean;
import cn.yxffcode.modularspring.core.ext.ExtensionHandlerInjector;
import cn.yxffcode.modularspring.core.io.JarEntryResource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author gaohang on 7/9/17.
 */
public class DefaultModuleApplicationContext extends AbstractXmlApplicationContext implements ModuleApplicationContext {
  protected final String moduleName;

  private final ExtensionHandlerInjector extensionHandlerInjector;

  public DefaultModuleApplicationContext(String[] configLocations, boolean refresh,
                                         ApplicationContext parent, String moduleName) {
    super(parent);
    this.moduleName = moduleName;
    setConfigLocations(configLocations);
    extensionHandlerInjector = new DefaultExtensionHandlerInjector(this);
    if (refresh) {
      refresh();
    }
  }

  @Override
  protected final void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
    // Create a new XmlBeanDefinitionReader for the given BeanFactory.
    final XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(new ModularBeanDefinitionRegistry(beanFactory));

    beanDefinitionReader.setEnvironment(this.getEnvironment());
    beanDefinitionReader.setResourceLoader(this);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
    beanDefinitionReader.setValidating(false);
    initBeanDefinitionReader(beanDefinitionReader);
    loadBeanDefinitions(beanDefinitionReader);
  }

  protected void preProcessBeforeRefresh() {
    //add bean post processors
  }

  @Override
  protected final Resource getResourceByPath(String path) {
    if (path != null && path.startsWith("/")) {
      path = path.substring(1);
    }
    if (path.startsWith("jar:")) {
      return new JarEntryResource(path);
    }
    return new FileSystemResource(path);
  }

  @Override
  public String getModuleName() {
    return moduleName;
  }

  @Override
  public void prepareRun() {
    Map<String, ExtensionHandlerBean> beans = getExtensionHandlerBeans();
    if (beans == null || beans.isEmpty()) {
      return;
    }
    for (ExtensionHandlerBean extensionHandlerBean : beans.values()) {
      //执行扩展点的注入
      extensionHandlerInjector.inject(extensionHandlerBean);
    }
  }

  private Map<String, ExtensionHandlerBean> getExtensionHandlerBeans() {
    try {
      return getBeansOfType(ExtensionHandlerBean.class);
    } catch (BeansException e) {
      return Collections.emptyMap();
    }
  }
}
