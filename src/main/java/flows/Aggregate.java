package flows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import blang.inits.experiments.tabwriters.factories.CSV;
import blang.inits.experiments.tabwriters.factories.Spark;
import blang.inits.parsing.CSVFile;
import blang.inits.parsing.QualifiedName;
import briefj.BriefIO;

public class Aggregate extends Experiment
{
  @Arg(description = "Command line arguments stored in tab separated values for each exec. "
      + "Syntax: comma separated list of (<key> [as <transformed-key>] )* from [optional] <tsv-file-in-each-exec>")
  public String keys;
  
  @Arg(description = "Comma separated value (cvs or csv.gz) file(s) in each exec containing the data to be aggregated.")
  public List<String> dataPathInEachExecFolder;
  
  @Arg(description = "Prefix of the directories, each containing stored command line arguments stored in tab separated values "
      + "as well data in csv format.") @DefaultValue("exec_")
  public String execFoldersPrefix                  = "exec_";
  
  public static String EXEC_FOLDER = "execFolder";
  
  @Override
  public void run()
  {
    for (String path : dataPathInEachExecFolder) 
      run(path);
  }
  
  public void run(String dataPathInEachExecFolder)
  {
    String outputFolderName = new File(dataPathInEachExecFolder).getName().replaceAll("[.]csv([.]gz)?", "");
    Path root = results.getFileInResultFolder(outputFolderName).toPath();
    List<ArgumentsFile> argFiles = parseArgumentsFiles(keys);
    
    BufferedWriter writer = null;
    
    loop : for (File execFolder : getExecFolders())
    {
      Map<String,String> currentArguments = new HashMap<>();
      for (ArgumentsFile argFile : argFiles)
      {
        Path currentConfigFile = execFolder.toPath().resolve(argFile.name);
        try 
        { 
          Map<String,String> parsedArgs = parseArguments(currentConfigFile, argFile.originalKeys);
          for (int i = 0; i < argFile.originalKeys.size(); i++)
          {
            String value = parsedArgs.get(argFile.originalKeys.get(i));
            String transfKey = argFile.transformedKeys.get(i);
            if (currentArguments.containsKey(transfKey) && 
                currentArguments.get(transfKey) != null && 
                !currentArguments.get(transfKey).equals(value))
              throw new RuntimeException("Key " + transfKey + " with conflicting values.");
            currentArguments.put(transfKey, value);
          }
        }
        catch (Exception e)  { 
          if (argFile.optional)
            ;
          else
          {
            System.out.println(ExceptionUtils.getStackTrace(e));
            warnMissing(currentConfigFile); 
            continue loop;
          }
        }
      }
      currentArguments.put(EXEC_FOLDER, execFolder.getName());
      File dataInEachExecFile = execFolder.toPath().resolve(dataPathInEachExecFolder).toFile();
      Collection<String> allTransformedKeys = allTransformedKeys(argFiles);
      
      if (!dataInEachExecFile.exists()) 
        System.err.println("File " + dataInEachExecFile + " does not exists, skipping this exec.");
      else if (experimentConfigs.tabularWriter instanceof CSV) {
        LinkedHashMap<String, String> prefix = argumentsToPrefix(currentArguments, allTransformedKeys);
        if (writer == null) {
          CSV settings = (CSV) experimentConfigs.tabularWriter;
          writer = results.getAutoClosedBufferedWriter(outputFolderName + ".csv" + (settings.compressed ? ".gz" : ""));
          String header = BriefIO.readLines(dataInEachExecFile).first().get();
          try {
            writer.append(briefj.CSV.toCSV(prefix.keySet()) + "," + header + "\n");
          } catch (IOException ioe) { throw new RuntimeException(ioe); }
        }
        String prefixStr = briefj.CSV.toCSV(prefix.values()) + ",";
        for (String line : BriefIO.readLines(dataInEachExecFile).skip(1))
          try {
            writer.append(prefixStr + line + "\n");
          } catch (IOException ioe) { throw new RuntimeException(ioe); }
      } else if (experimentConfigs.tabularWriter instanceof Spark) {
        Path leafDirectory = argumentsToDirectory(root, currentArguments, allTransformedKeys);
        // create symlink
        try 
        { 
          
          if (!dataInEachExecFile.exists())
          {
            warnMissing(dataInEachExecFile.toPath()); 
            continue loop;
          }
          if (dataInEachExecFile.isDirectory())
          {
            for (File subDir : dataInEachExecFile.listFiles())
              if (subDir.isDirectory())
                Files.createSymbolicLink(leafDirectory.resolve(subDir.getName()), subDir.toPath());
          }
          else 
          {
            Files.createSymbolicLink(leafDirectory.resolve("data.csv"), execFolder.toPath().resolve(dataPathInEachExecFolder)); 
          }
        } 
        catch (IOException e) { throw new RuntimeException(e); }
      }
    }
  }

  private Collection<String> allTransformedKeys(List<ArgumentsFile> argFiles)
  {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (ArgumentsFile argFile : argFiles)
      result.addAll(argFile.transformedKeys);
    result.add(EXEC_FOLDER);
    return result;
  }

  private void warnMissing(Path currentFile)
  {
    System.out.println("Skipping one exec folder (often because of missing file): " + currentFile.toString());
  }

  private List<File> getExecFolders()
  {
    File f = new File(execFoldersPrefix).getAbsoluteFile();
    String simplePrefix = f.getName();
    File parent = f.getParentFile();
    return Arrays.asList(parent.listFiles((__, name) -> name.startsWith(simplePrefix)));
  }

  private static Path argumentsToDirectory(Path root, Map<String, String> currentArguments, Collection<String> argumentsKeys)
  {
    Path current = root;
    for (String key : argumentsKeys)
      current = current.resolve(key + "=" + removeSciNotation(currentArguments.get(key)));
    if (current.toFile().exists())
      throw new RuntimeException("Duplicate argument combinations not allowed: " + currentArguments);
    current.toFile().mkdirs();
    return current;
  }
  
  private static LinkedHashMap<String,String> argumentsToPrefix(Map<String, String> currentArguments, Collection<String> argumentsKeys)
  {
    LinkedHashMap<String,String> result = new LinkedHashMap<>();
    for (String key : argumentsKeys)
      result.put(key, currentArguments.get(key));
    return result;
  }
  
  private Map<String, String> parseArguments(Path configFile, List<String> keys)
  {
    Map<QualifiedName,List<String>> allArguments = CSVFile.parseTSV(configFile.toFile()).asMap();
    Map<String, String> result = new HashMap<>();
    for (Entry<QualifiedName,List<String>> entry : allArguments.entrySet()) 
      result.put(entry.getKey().toString(), Joiner.on(" ").join(entry.getValue()));
    result.keySet().retainAll(keys);
    return result;
  }
  
  private static List<ArgumentsFile> parseArgumentsFiles(String line) 
  {
    List<String> argumentsFileSpecs = Splitter.onPattern("\\s*,\\s*").trimResults().omitEmptyStrings().splitToList(line);
    List<ArgumentsFile> result = new ArrayList<>();
    for (String argumentsFileSpec : argumentsFileSpecs)
      result.add(new ArgumentsFile(argumentsFileSpec));
    return result;
  }
  
  private static class ArgumentsFile
  {
    final String name;
    final boolean optional;
    final List<String> originalKeys    = new ArrayList<>();
    final List<String> transformedKeys = new ArrayList<>();
    
    ArgumentsFile(String spec)
    {
      List<String> keyFile = Splitter.on("from").trimResults().omitEmptyStrings().splitToList(spec);
      if (keyFile.size() != 2)
        throw new RuntimeException("Expected exactly one keyword 'from' in:" + spec);
      String keySpec  = keyFile.get(0);
      String fileSpec = keyFile.get(1);
      List<String> splitKeys = Splitter.onPattern("\\s+").trimResults().omitEmptyStrings().splitToList(keySpec);
      int index = 0;
      boolean asStmt = false;
      for (String splitKey : splitKeys)
      {
        if (splitKey.equals("as"))
        {
          if (asStmt)
            throw new RuntimeException("Error, encountered invalid string: 'as as'");
          asStmt = true;
        } else if (asStmt) {
          if (index == 0)
            throw new RuntimeException("Error, statement cannot starts with 'as'");
          transformedKeys.set(index - 1, splitKey);
          asStmt = false;
        } else {
          originalKeys.add(splitKey);
          transformedKeys.add(splitKey);
          index++;
        }
      }
      List<String> splitFileSpec = Splitter.onPattern("\\s+").trimResults().omitEmptyStrings().splitToList(fileSpec);
      if (splitFileSpec.isEmpty() || splitFileSpec.size() > 2 || 
          (splitFileSpec.size() == 2 && !splitFileSpec.get(0).equals("optional")))
        throw new RuntimeException("Invalid file spec: " + splitFileSpec);
      optional = splitFileSpec.size() == 2;
      name = splitFileSpec.get(splitFileSpec.size() - 1);
    }
  }
  
  /**
   * Used to fix bug/limitation in Spark
   */
  public static String removeSciNotation(String s)
  {
    if (s == null)
      return s;
    if (!NumberUtils.isCreatable(s))
      return s;
    if (s.contains("E") || s.contains("e"))
      return new BigDecimal(s).toPlainString();
    else
      return s;
  }
  
  public static void main(String [] args) 
  {
    Experiment.startAutoExit(args);
  }
}
