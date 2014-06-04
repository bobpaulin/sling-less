package org.apache.sling.less;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.WebResourceScriptCache;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.WebResourceScriptRunnerFactory;
import org.apache.sling.webresource.impl.WebResourceInventoryManagerImpl;
import org.apache.sling.webresource.impl.WebResourceScriptCacheImpl;
import org.apache.sling.webresource.impl.WebResourceScriptRunnerFactoryImpl;
import org.apache.sling.webresource.model.GlobalCompileOptions;
import org.osgi.service.component.ComponentContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import static org.easymock.EasyMock.*;
import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LessCompilerImplTest extends TestCase {
    
    private LessCompilerImpl lessCompiler;
    
    private WebResourceScriptRunnerFactoryImpl webResourceScriptRunnerFactory;
    
    private ResourceResolverFactory mockResourceResolverFactory;
    
    private ComponentContext mockComponentContext;
    
    private ResourceResolver mockResourceResolver;
    
    private Resource mockResource;
    
    private Node mockNode;
    
    private Property mockProperty;
    
    private Binary mockBinary;
    
    @BeforeClass
    public void setUp() throws Exception
    {
        super.setUp();
        
        lessCompiler = new LessCompilerImpl();
        
        webResourceScriptRunnerFactory = new WebResourceScriptRunnerFactoryImpl();
        
        WebResourceInventoryManager inventoryManagerMock = new WebResourceInventoryManagerImpl();
        
        WebResourceScriptCache scriptCacheMock = new WebResourceScriptCacheImpl();
        
        webResourceScriptRunnerFactory.setWebResourceInventoryManager(inventoryManagerMock);
        
        webResourceScriptRunnerFactory.setWebResourceScriptCache(scriptCacheMock);
        
        mockResourceResolverFactory = createMock(ResourceResolverFactory.class);
        
        lessCompiler.setResourceResolverFactory(mockResourceResolverFactory);
        
        lessCompiler.setWebResourceScriptRunnerFactory(webResourceScriptRunnerFactory);
        
        mockComponentContext = createMock(ComponentContext.class);
        mockResourceResolver = createMock(ResourceResolver.class);
        mockResource = createMock(Resource.class);
        mockNode = createMock(Node.class);
        mockProperty = createMock(Property.class);
        mockBinary = createMock(Binary.class);
        
        InputStream lessInputStream = getClass().getResourceAsStream("/SLING-INF/system/less/less-rhino-1.7.0.js");
        
        InputStream lesscInputStream = getClass().getResourceAsStream("/SLING-INF/system/less/lessc-rhino-1.7.0.js");
        
        InputStream slingLessOverridesInputStream = getClass().getResourceAsStream("/sling-less-overrides.js");
        
        Hashtable bundleProperties = new Hashtable();
        bundleProperties.put("less.compiler.path", "/test/path");
        bundleProperties.put("less.cache.path", "/test/path2");
        bundleProperties.put("lessc.compiler.path", "/test/path3");
        expect(mockComponentContext.getProperties()).andReturn(bundleProperties);
        expect(mockResourceResolverFactory.getAdministrativeResourceResolver(null)).andReturn(mockResourceResolver);        
        
        expect(mockResourceResolver.getResource("/test/path")).andReturn(mockResource);
        expect(mockResource.adaptTo(Node.class)).andReturn(mockNode);
        expect(mockNode.getNode(Property.JCR_CONTENT)).andReturn(mockNode);
        expect(mockNode.getProperty(Property.JCR_DATA)).andReturn(mockProperty);
        expect(mockProperty.getBinary()).andReturn(mockBinary);
        expect(mockBinary.getStream()).andReturn(lessInputStream);
        
        expect(mockResourceResolver.getResource("/test/path3")).andReturn(mockResource);
        expect(mockResource.adaptTo(Node.class)).andReturn(mockNode);
        expect(mockNode.getNode(Property.JCR_CONTENT)).andReturn(mockNode);
        expect(mockNode.getProperty(Property.JCR_DATA)).andReturn(mockProperty);
        expect(mockProperty.getBinary()).andReturn(mockBinary);
        expect(mockBinary.getStream()).andReturn(lesscInputStream);
        
        expect(mockResourceResolver.getResource("/system/less/sling-less-overrides.js")).andReturn(mockResource);
        expect(mockResource.adaptTo(Node.class)).andReturn(mockNode);
        expect(mockNode.getNode(Property.JCR_CONTENT)).andReturn(mockNode);
        expect(mockNode.getProperty(Property.JCR_DATA)).andReturn(mockProperty);
        expect(mockProperty.getBinary()).andReturn(mockBinary);
        expect(mockBinary.getStream()).andReturn(slingLessOverridesInputStream);
        
        mockResourceResolver.close();
        
        expect(mockResourceResolverFactory.getAdministrativeResourceResolver(null)).andReturn(mockResourceResolver);
        expect(mockResourceResolver.getResource(isA(String.class))).andReturn(mockResource);
        expect(mockResource.getParent()).andReturn(mockResource);
        expect(mockResource.adaptTo(Node.class)).andReturn(mockNode);
        expect(mockResource.getPath()).andReturn("less/file.less");
        mockResourceResolver.close();
        
        replay(mockBinary);
        replay(mockProperty);
        replay(mockComponentContext);
        replay(mockNode);
        replay(mockResource);
        replay(mockResourceResolver);
        replay(mockResourceResolverFactory);
        
        lessCompiler.activate(mockComponentContext);
    }
    
    @Test
    public void testCharset() throws Exception {
        testFileCompile("charsets.less");
    }
    
    @Test
    public void testColors() throws Exception {
        testFileCompile("colors.less");
    }
    
    @Test
    public void testComments() throws Exception {
        testFileCompile("comments.less");
    }
    
    @Test
    public void testCss3() throws Exception {
        testFileCompile("css-3.less");
    }

    @Test
    public void testCssEscape() throws Exception {
        testFileCompile("css-escapes.less");
    }
    
    @Test
    public void testCss() throws Exception {
        testFileCompile("css.less");
    }
    
    @Test
    public void testFunctions() throws Exception {
        testFileCompile("functions.less");
    }
    
    @Test
    public void testIEFilters() throws Exception {
        testFileCompile("ie-filters.less");
    }
    
    @Test
    public void testImportOnce() throws Exception {
        testFileCompile("import-once.less");
    }
    
    @Test
    public void testImport() throws Exception {
        testFileCompile("import.less");
    }
    
    @Test
    public void testJavaScript() throws Exception {
        testFileCompile("javascript.less");
    }
    
    @Test
    public void testLazyEval() throws Exception {
        testFileCompile("lazy-eval.less");
    }
    
    @Test
    public void testMedia() throws Exception {
        testFileCompile("media.less");
    }
    
    @Test
    public void testMixInsArgs() throws Exception {
        testFileCompile("mixins-args.less");
    }
    
    @Test
    public void testMixInsClosure() throws Exception {
        testFileCompile("mixins-closure.less");
    }
    
    @Test
    public void testMixInsGuards() throws Exception {
        testFileCompile("mixins-guards.less");
    }
    
    @Test
    public void testMixInsImportant() throws Exception {
        testFileCompile("mixins-important.less");
    }
    
    @Test
    public void testMixInsNamedArgs() throws Exception {
        testFileCompile("mixins-named-args.less");
    }
    
    @Test
    public void testMixInsNested() throws Exception {
        testFileCompile("mixins-nested.less");
    }
    
    @Test
    public void testMixInsPatterns() throws Exception {
        testFileCompile("mixins-pattern.less");
    }
    
    @Test
    public void testMixIns() throws Exception {
        testFileCompile("mixins.less");
    }
    
    @Test
    public void testOperations() throws Exception {
        testFileCompile("operations.less");
    }
    
    @Test
    public void testParens() throws Exception {
        testFileCompile("parens.less");
    }
    
    @Test
    public void testRulesets() throws Exception {
        testFileCompile("rulesets.less");
    }
    
    @Test
    public void testScope() throws Exception {
        testFileCompile("scope.less");
    }
    
    @Test
    public void testSelectors() throws Exception {
        testFileCompile("selectors.less");
    }
    
    @Test
    public void testStrings() throws Exception {
        testFileCompile("strings.less");
    }
    
    /*URLs not currently supported.
    @Test
    public void testUrls() throws Exception {
        testFileCompile("urls.less");
    }
    */
    
    @Test
    public void testVariables() throws Exception {
        testFileCompile("variables.less");
    }
    
    @Test
    public void testWhitespace() throws Exception {
        testFileCompile("whitespace.less");
    }
    
    private void testFileCompile(String fileName) throws Exception {
        InputStream lessStream = getTestLessFile(fileName);
        
        Map<String, Object> globalCompileOptions = new HashMap<String, Object>();
        GlobalCompileOptions compileOptions = new GlobalCompileOptions();
        compileOptions.setSourcePath(fileName);
        
        globalCompileOptions.put("global", compileOptions);
        
        InputStream result = lessCompiler.compile(lessStream, globalCompileOptions);
        
        assertEquals("File named: " + fileName + " should compile to proper CSS", IOUtils.toString(getResultCssFile(fileName), "UTF-8"), IOUtils.toString(result, "UTF-8"));
    }
    
    private InputStream convertFileToStream(String filePath) throws Exception
    {
        return getClass().getResourceAsStream(filePath);
    }
    
    private InputStream getTestLessFile(String fileName) throws Exception
    {
        return convertFileToStream("/less/"+ fileName);
    }
    
    private InputStream getResultCssFile(String fileName) throws Exception
    {
        fileName = fileName.replaceFirst(".less", "");
        return convertFileToStream("/css/"+ fileName + ".css");
    }
    
    @AfterClass
    public void after()
    {
        verify(mockBinary);
        verify(mockProperty);
        verify(mockComponentContext);
        verify(mockNode);
        verify(mockResource);
        verify(mockResourceResolver);
        verify(mockResourceResolverFactory);
    }

}
