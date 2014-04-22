importPackage(java.io);
importPackage(Packages.org.apache.sling.webresource.util);
importPackage(Packages.javax.jcr);
importPackage(Packages.org.apache.commons.io);
/**
 * Overrides to enable less to use sling functionality for: 
 * File Reads
 * File Writes
 * 
 * @author bpaulin
 * 
 */

/**
 * Overriding default Rhino readFile.
 * Reads a JCR File
 * 
 * @param filename
 */
function readFile(filename, characterCoding)
{
	if(!characterCoding)
	{
		characterCoding = null;
	}
	var inputStream = JCRUtils.getFileNodeAsStream(currentNode.getNode(filename));
	
	return String(IOUtils.toString(inputStream, characterCoding));
}


/**
 * Output method to compile less to CSS.
 * @param css The CSS String
 * @param compileOptions LESS compile options
 * 
 */
var lessSling = function(css, compileOptions) {
    var result;
    var parser = new less.Parser();

    parser.parse(css, function (e, root) {
    	if (e) {
    		java.lang.System.out.println(JSON.stringify(e)); 
       		throw e;    		
    	}
   		result = root.toCSS(compileOptions);
    });
    return result;
};