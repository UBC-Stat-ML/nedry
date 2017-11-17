package mcli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import bayonet.math.EffectiveSampleSize;
import blang.inits.Arg;
import blang.inits.DefaultValue;
import blang.inits.experiments.Experiment;
import briefj.BriefIO;

public class ComputeESS extends Experiment 
{
  @Arg 
  File csvFile;
  
  @Arg 
  Optional<String> field = Optional.empty();
  
  @Arg @DefaultValue("1")
  int moment        = 1;

  @Override
  public void run() 
  {
    List<Double> samples = new ArrayList<>();
    if (field.isPresent())
      for (Map<String,String> line : BriefIO.readLines(csvFile).indexCSV())
        samples.add(Double.parseDouble(line.get(field.get()).trim()));
    else  
      for (String line : BriefIO.readLines(csvFile).filter(line -> !line.trim().isEmpty()))
        samples.add(Double.parseDouble(line.trim()));
    System.out.println(moment == 1 ?
      EffectiveSampleSize.ess(samples) :
      EffectiveSampleSize.ess(samples, x -> Math.pow(x, moment)));
  }

  public static void main(String [] args) 
  {
    Experiment.startAutoExit(args);
  }
}
