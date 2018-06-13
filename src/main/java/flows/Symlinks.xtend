package flows

import java.io.InputStreamReader
import java.io.BufferedReader
import java.util.regex.Pattern
import briefj.BriefStrings
import java.io.File
import briefj.run.Results
import java.nio.file.Paths
import java.nio.file.Files
import blang.inits.Arg
import blang.inits.DefaultValue
import blang.inits.Creators
import blang.inits.parsing.Posix
import binc.Command
import static binc.Command.call

class Symlinks {
  @Arg @DefaultValue("false") 
  boolean open = false
  
  public static def void main(String [] args) {
    val creator = Creators::conventional
    val app = 
      try { creator.init(Symlinks, Posix.parse(args)) }
      catch (Exception e) { 
        System.err.println(creator.fullReport)
        System.exit(1)
        null
      }
    app.run
  }
  static val pattern = Pattern.compile("\\[([A-Za-z0-9]+)[/]([A-Za-z0-9]+)\\] .* [>] ([A-Za-z0-9_-]+)( [(][0-9]+[)])?.*")
  def run() {
    val br = new BufferedReader(new InputStreamReader(System.in))
    val allLinks = new File("links") => [mkdir]
    val currentExec = new File(allLinks, Results::nextRandomResultFolderName) => [mkdir]
    if (open) 
      call(Command::byName("open").withArg(currentExec.absolutePath))
    while (true) {
      val input = br.readLine();
      if (input !== null) {
        println(input)
        val matches = BriefStrings::allGroupsFromFirstMatch(pattern, input)
        if (!matches.empty) {
          val prefix =  matches.get(0)
          val suffix =  matches.get(1)
          val processName = matches.get(2)
          val _index = matches.get(3)
          val parsedIndex = if (_index === null) 0 else Integer.parseInt(_index.replaceAll(".*\\(", "").replaceAll("\\).*", ""))
          val _subdir = Paths::get("work", prefix).toFile
          val workdir = complete(_subdir, suffix)
          val linkParentFolder = new File(currentExec, processName) => [mkdirs]
          val link = new File(linkParentFolder, "" + parsedIndex)
          val relative =  linkParentFolder.toPath.relativize(workdir.toPath)
          Files::createSymbolicLink(link.toPath, relative) 
        }
      } else return
    }
  }
  static def File complete(File directory, String prefix) {
    var File found = null
    for (child : directory.list) {
      if (child.startsWith(prefix)) {
        if (found !== null) 
          throw new RuntimeException
        found = new File(directory, child)
      }
    }
    return found
  }
}
