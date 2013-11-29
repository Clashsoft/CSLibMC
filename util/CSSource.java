package clashsoft.cslib.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CSSource extends CSString
{
	public static boolean isQuoted(String string, int pos)
	{
		return isQuoted(analyseSource(string, pos));
	}
	
	public static boolean isQuoted(int[] data)
	{
		return (data[4] & 1) != 0;
	}
	
	public static boolean isCharQuoted(String string, int pos)
	{
		return isCharQuoted(analyseSource(string, pos));
	}
	
	public static boolean isCharQuoted(int[] data)
	{
		return (data[4] & 2) != 0;
	}
	
	public static boolean isLiteral(String string, int pos)
	{
		return isLiteral(analyseSource(string, pos));
	}
	
	public static boolean isLiteral(int[] data)
	{
		return (data[4] & 4) != 0;
	}
	
	public static int getParenthesisDepth(String string, int pos)
	{
		return getParenthesisDepth(analyseSource(string, pos));
	}
	
	public static int getParenthesisDepth(int[] data)
	{
		return data[0];
	}
	
	public static int getSquareBracketDepth(String string, int pos)
	{
		return getSquareBracketDepth(analyseSource(string, pos));
	}
	
	public static int getSquareBracketDepth(int[] data)
	{
		return data[1];
	}
	
	public static int getCurlyBracketDepth(String string, int pos)
	{
		return getCurlyBracketDepth(analyseSource(string, pos));
	}
	
	public static int getCurlyBracketDepth(int[] data)
	{
		return data[2];
	}
	
	public static int getAngleBracketDepth(String string, int pos)
	{
		return getAngleBracketDepth(analyseSource(string, pos));
	}
	
	public static int getAngleBracketDepth(int[] data)
	{
		return data[3];
	}
	
	public static int[] analyseSource(String string, int pos)
	{
		int depth1 = 0;
		int depth2 = 0;
		int depth3 = 0;
		int depth4 = 0;
		int other = 0;
		boolean quote = false;
		boolean charQuote = false;
		boolean literal = false;
		
		for (int i = 0; (i < pos) && (i < string.length()); i++)
		{
			char c = string.charAt(i);
			
			if (!literal)
			{
				if (c == '"')
					quote = !quote;
				else if (c == '\'')
					charQuote = !charQuote;
				else if (c == '\\')
					literal = true;
				else if (!quote && !charQuote)
				{
					switch (c)
					{
					case '(':
						depth1++;
					case ')':
						depth1--;
					case '[':
						depth2++;
					case ']':
						depth2--;
					case '{':
						depth3++;
					case '}':
						depth3--;
					case '<':
						depth4++;
					case '>':
						depth4--;
					default:
						continue;
					}
				}
			}
			else
				literal = false;
		}
		
		other = (literal ? 4 : 0) | (charQuote ? 2 : 0) | (quote ? 1 : 0);
		
		return new int[] { depth1, depth2, depth3, depth4, other };
	}
	
	public static String stripComments(String string)
	{
		StringBuilder result = new StringBuilder(string.length());
		
		boolean quote = false;
		boolean charQuote = false;
		boolean literal = false;
		
		for (int i = 0; i < string.length(); i++)
		{
			char c = string.charAt(i);
			
			if (!literal) // Do not check if the current char is a literal
			{
				if (c == '"') // String quote switch
					quote = !quote;
				else if (c == '\'') // Char quote switch
					charQuote = !charQuote;
				else if (c == '\\') // Literals
					literal = true;
				else if (!quote && !charQuote) // Do not check if the current char is quoted
				{
					if (c == '/' && i + 1 < string.length()) // Comment indicators always start with a '/'
					{
						char c1 = string.charAt(i + 1);
						if (c1 == '/') // Line comment
						{
							i = string.indexOf("\n", i) - 1;
							continue;
						}
						else if (c1 == '*') // Multi-line comment
						{
							i = string.indexOf("*/", i + 1) + 1;
							continue;
						}
					}
				}
			}
			else
				literal = false;
			
			// Append the string
			int i1 = string.indexOf("/", i);
			if (i1 == -1)
				i1 = string.length();
			result.append(string.substring(i, i1));
			i = i1 - 1;
		}
		
		return result.toString();
	}
	
	public static List<Class<?>> getClassesForPackage(Package pack)
	{
		String packageName = pack.getName();
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		
		File directory = null;
		String fullPath;
		String relPath = packageName.replace('.', '/');
		URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
		
		if (resource == null)
		{
			throw new RuntimeException("No resource for " + relPath);
		}
		fullPath = resource.getFile();
		
		try
		{
			directory = new File(resource.toURI());
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(packageName + " (" + resource + ") does not appear to be a valid URL / URI.  Strange, since we got it from the system...", e);
		}
		catch (IllegalArgumentException e)
		{
			directory = null;
		}
		
		if (directory != null && directory.exists())
		{
			// Get the list of the files contained in the package
			String[] files = directory.list();
			for (int i = 0; i < files.length; i++)
			{
				// we are only interested in .class files
				if (files[i].endsWith(".class"))
				{
					// removes the .class extension
					String className = packageName + '.' + files[i].substring(0, files[i].length() - 6);
					
					try
					{
						classes.add(Class.forName(className));
					}
					catch (ClassNotFoundException e)
					{
						throw new RuntimeException("ClassNotFoundException loading " + className);
					}
				}
			}
		}
		else
		{
			try
			{
				String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
				JarFile jarFile = new JarFile(jarPath);
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements())
				{
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					if (entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length()))
					{
						String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
						
						try
						{
							classes.add(Class.forName(className));
						}
						catch (ClassNotFoundException e)
						{
							jarFile.close();
							throw new RuntimeException("ClassNotFoundException loading " + className);
						}
					}
				}
				jarFile.close();
			}
			catch (IOException e)
			{
				throw new RuntimeException(packageName + " (" + directory + ") does not appear to be a valid package", e);
			}
		}
		return classes;
	}
}
