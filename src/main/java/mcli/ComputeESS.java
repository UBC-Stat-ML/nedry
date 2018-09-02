package mcli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

import bayonet.math.EffectiveSampleSize;
import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import briefj.BriefIO;
import briefj.BriefMaps;

public class ComputeESS extends Experiment 
{
  @Arg(description = "csv file containing samples")
  public File inputFile;
  
  @Arg(description = "Look for this column name for samples.") 
                  @DefaultValue("value")
  public String samplesColumn = "value";
  
  @Arg(description = "Look for this column name for iteration index (or empty if none).")
                         @DefaultValue("sample")
  public String iterationIndexColumn = "sample";
  
  @Arg        @DefaultValue("1")
  public int moment        = 1;
         
  @Arg            @DefaultValue("0.25")
  public double burnInFraction = 0.25;
  
  @Arg     @DefaultValue("ess.csv")
  public String output = "ess.csv";

  @Override
  public void run() { try { computeEss(); } catch (Exception e) { throw new RuntimeException(e); } }
    
  public void computeEss() throws IOException
  {
    if (burnInFraction < 0.0 || burnInFraction > 1.0)
      throw new RuntimeException();
    Map<Map<String,String>,List<Double>> samples = new LinkedHashMap<>();
    for (Map<String,String> line : BriefIO.readLines(inputFile).indexCSV()) {
      double value = Double.parseDouble(line.get(samplesColumn).trim());
      line.remove(samplesColumn);
      line.remove(iterationIndexColumn);
      BriefMaps.getOrPutList(samples, line).add(value);
    }
    BufferedWriter writer = results.getAutoClosedBufferedWriter(output);
    {
      List<String> header = new ArrayList<>(samples.keySet().iterator().next().keySet());
      header.add("ess");
      writer.append(Joiner.on(",").join(header) + "\n");
    }
    for (Map<String,String> key : samples.keySet()) {
      List<Double> curSamples = samples.get(key);
      double ess = EffectiveSampleSize.ess(curSamples.subList((int) (burnInFraction * curSamples.size()), curSamples.size()), x -> Math.pow(x, moment)); 
      List<String> entries = new ArrayList<>(key.values());
      entries.add("" +  ess);
      writer.append(Joiner.on(",").join(entries) + "\n");
    }
  }

  public static void main(String [] args) 
  {
    Experiment.startAutoExit(args);
  }
}
