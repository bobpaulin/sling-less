importPackage(java.io);
importPackage(Packages.org.apache.sling.webresource.util);
importPackage(Packages.javax.jcr);
importPackage(Packages.org.apache.commons.io);



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
	
	var re = new RegExp('/', 'g');
	filename = filename.replace(re, File.separator);
	var testFilePath = new java.lang.StringBuffer();
	testFilePath.append("src");
	testFilePath.append(File.separator);
	testFilePath.append("test");
	testFilePath.append(File.separator);
	testFilePath.append("resources");
	testFilePath.append(File.separator);
	testFilePath.append("less");
	testFilePath.append(File.separator);
	testFilePath.append(filename);
	var testFile = new File(testFilePath.toString());
	var inputStream = new FileInputStream(testFile);
	
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

less.tree.functions.add = function (a, b) {
    return new(less.tree.Dimension)(a.value + b.value);
};
less.tree.functions.increment = function (a) {
    return new(less.tree.Dimension)(a.value + 1);
};
less.tree.functions._color = function (str) {
    if (str.value === "evil red") { return new(less.tree.Color)("600") }
};
//Fake out process which is typically provided by NodeJS.
var process = {title:'test'};
