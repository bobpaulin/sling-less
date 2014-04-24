package org.apache.sling.less;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.WebResourceScriptRunnerFactory;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.model.GlobalCompileOptions;
import org.apache.sling.webresource.util.JCRUtils;
import org.apache.sling.webresource.util.ScriptUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * LESS Compiler Service for compiling LESS files into CSS on the fly
 * in Sling.
 * 
 * @author bpaulin
 *
 */

@Component(label = "LESS Compiler Service", immediate = true, metatype = true)
@Service
public class LessCompilerImpl implements WebResourceScriptCompiler {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private WebResourceScriptRunnerFactory webResourceScriptRunnerFactory;

    @org.apache.felix.scr.annotations.Property(label = "LESS Compiler Script Path", value = "/system/less/less-rhino-1.7.0.js")
    private final static String LESS_COMPILER_PATH = "less.compiler.path";

    @org.apache.felix.scr.annotations.Property(label = "LESSC Compiler Script Path", value = "/system/less/lessc-rhino-1.7.0.js")
    private final static String LESSC_COMPILER_PATH = "lessc.compiler.path";
    
    @org.apache.felix.scr.annotations.Property(label = "LESS Cache Path", value = "/var/less")
    private final static String LESS_CACHE_PATH = "less.cache.path";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String lessCompilerPath;
    
    private String lesscCompilerPath;

    private String lessCachePath;
    
    private WebResourceScriptRunner scriptRunner;

    public void activate(final ComponentContext context) throws Exception {
        Dictionary config = context.getProperties();
        lessCompilerPath = PropertiesUtil.toString(
                config.get(LESS_COMPILER_PATH),
                "/system/less/less-rhino-1.7.0.js");
        lesscCompilerPath = PropertiesUtil.toString(
                config.get(LESSC_COMPILER_PATH),
                "/system/less/lessc-rhino-1.7.0.js");
        lessCachePath = PropertiesUtil.toString(config.get(LESS_CACHE_PATH),
                "/var/less");
        loadLessRunner();
	    
    }

	private void loadLessRunner() throws LoginException, RepositoryException {
		ResourceResolver resolver = null;
        try {
            resolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);
        
	        InputStream lessCompilerStream = JCRUtils.getFileResourceAsStream(
	                resolver, lessCompilerPath);
	        
	        InputStream lesscCompilerStream = JCRUtils.getFileResourceAsStream(
	        		resolver, lesscCompilerPath);
	        
	        InputStream lessCombineStream = new SequenceInputStream(lessCompilerStream, lesscCompilerStream);
	        
	        InputStream lessOverridesStream = JCRUtils.getFileResourceAsStream(
                    resolver, "/system/less/sling-less-overrides.js");
	        
	        InputStream globalScriptStream = new SequenceInputStream(lessCombineStream, lessOverridesStream);
	        
	        this.scriptRunner = this.webResourceScriptRunnerFactory.createRunner("less.js", globalScriptStream);
        }finally{
        	if(resolver != null)
        	{
        		resolver.close();
        	}
        }
	}

    public InputStream compile(InputStream lessStream)
            throws WebResourceCompileException {
        return compile(lessStream, null);
    }

    public InputStream compile(InputStream lessStream,
            Map<String, Object> compileOptions)
            throws WebResourceCompileException {

        Map<String, Object> lessCompileOptions = new HashMap<String, Object>();

        String sourcePath = null;

        GlobalCompileOptions globalCompileOptions = ScriptUtils.getGlobalCompileOptions(compileOptions);
        
        if (globalCompileOptions != null) {
            sourcePath = globalCompileOptions.getSourcePath();
        }
        ResourceResolver resolver = null;
        try {
            String coffeeScript = IOUtils.toString(lessStream);
            StringBuffer scriptBuffer = new StringBuffer();
            scriptBuffer.append("lessSling(");
            scriptBuffer.append(ScriptUtils.toJSMultiLineString(coffeeScript));
            scriptBuffer.append(", ");
            if (lessCompileOptions.isEmpty()) {
                scriptBuffer.append("{strictMath:true}");
            } else {
                scriptBuffer.append(ScriptUtils
                        .generateCompileOptionsString(lessCompileOptions));
            }
            scriptBuffer.append(");");
            
            InputStream lessCompileScript = new ByteArrayInputStream(scriptBuffer.toString().getBytes());
            
            Map<String, Object> scriptVariables = new HashMap<String, Object>();
            
            if(sourcePath != null)
            {
                
                resolver = resourceResolverFactory
                        .getAdministrativeResourceResolver(null);
                Resource sourceResource = resolver.getResource(sourcePath);
                Resource sourceFolder = sourceResource.getParent();
                
                scriptVariables.put("currentNode", sourceFolder.adaptTo(Node.class));
                scriptVariables.put("name", sourceFolder.getPath());
                  
            }
                    
            String compiledScript = scriptRunner.evaluateScript(lessCompileScript, scriptVariables);
            //String compiledScript = (String) rhinoContext.evaluateReader(
            //        rootScope, lessReader, "LESS", 1, null);
            return new ByteArrayInputStream(compiledScript.getBytes());
        } catch (Exception e) {
            throw new WebResourceCompileException(e);
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }

    public String getCacheRoot() {
        return this.lessCachePath;
    }

    public boolean canCompileNode(Node sourceNode) {
        String extension = null;
        String mimeType = null;
        try {

            if (sourceNode.hasNode(Property.JCR_CONTENT)) {
                Node sourceContent = sourceNode.getNode(Property.JCR_CONTENT);
                if (sourceContent.hasProperty(Property.JCR_MIMETYPE)) {
                    mimeType = sourceContent.getProperty(Property.JCR_MIMETYPE)
                            .getString();
                }
            }
            extension = JCRUtils.getNodeExtension(sourceNode);

        } catch (RepositoryException e) {
            // Log Exception
            log.info("Node Name can not be read.  Skipping node.");
        }

        return "less".equals(extension) || "text/less".equals(mimeType);
    }

    public String compiledScriptExtension() {
        return "css";
    }

    public void setResourceResolverFactory(
            ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }
    
    public void setWebResourceScriptRunnerFactory(
			WebResourceScriptRunnerFactory webResourceScriptRunnerFactory) {
		this.webResourceScriptRunnerFactory = webResourceScriptRunnerFactory;
	}
}
