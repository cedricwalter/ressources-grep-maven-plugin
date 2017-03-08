package com.cedricwalter.maven.grep;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * https://opensource.org/licenses/GPL-3.0
 */
@Mojo(name = "grep", threadSafe = true)
public class GrepMojo extends AbstractMojo {

    public static final String ALL_TEXT_EXTENSIONS = "([^\\s]+(\\.(?i)(txt|xml|xsl|properties|csv|conf|properties|jsp|css|scss|svg|js|html|episode|xsd|dtd))$)";

    @Parameter(required = true)
    private List<Grep> greps;

    @Parameter(defaultValue = "${project.build.directory}")
    private String folder;

    @Parameter(defaultValue = "found in file ${fileName} at line ${lineNumber} : ${line}")
    private String outputPattern;

    private Log log = getLog();

    public List<Grep> getGreps() {
        return greps;
    }

    public void setGreps(List<Grep> greps) {
        this.greps = greps;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getOutputPattern() {
        return outputPattern;
    }

    public void setOutputPattern(String outputPattern) {
        this.outputPattern = outputPattern;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            for (Grep grep : greps) {

                Pattern lookingFor = Pattern.compile(grep.getGrepPattern());

                String filePattern = grep.getFilePattern();
                if ("".equals(filePattern) || filePattern == null) {
                    filePattern = ALL_TEXT_EXTENSIONS;
                }

                Collection<File> files = FileUtils.listFiles(
                        new File(getFolder()),
                        new RegexFileFilter(filePattern),
                        DirectoryFileFilter.DIRECTORY
                );
                log.info("found : " + files.size() + " to grep for " + grep.getGrepPattern());

                for (File file : files) {
                    grepInFile(file, lookingFor, grep);
                }
            }
        } catch (MojoFailureException e) {
            //rethrow. this is a deliberate failure
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException("error grepping", e);
        }
    }

    private void grepInFile(File theFile, Pattern lookingFor, Grep grep) throws Exception {
        if (!theFile.exists()) {
            log.warn("Specified file does not exist: " + theFile.getCanonicalPath());
            return;
        }
        if (!theFile.canRead()) {
            log.warn("Cannot read from file " + theFile.getCanonicalPath());
            return;
        }
        log.debug("Grepping for " + lookingFor + " in " + theFile.getCanonicalPath());
        Matcher m;
        try (FileReader reader = new FileReader(theFile)){
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            int lineNumber = 0;
            boolean found = false;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                m = lookingFor.matcher(line);
                if (!(m.find())) {
                    continue;
                }
                processLine(theFile, line, grep, lineNumber);
                found = true;
            }
            if (!found) {
                failIfNotFound(theFile, grep);
            }
        }
    }

    private void processLine(File theFile, String theLine, Grep grep, int lineNumber) throws Exception {
        printMatch(theFile, theLine, grep, lineNumber);
        failIfFound(theFile, theLine, grep, lineNumber);
    }

    private void printMatch(File theFile, String theLine, Grep grep, int lineNumber) throws IOException, TemplateException {
        String templateToUse = grep.getOutputPattern();
        if (templateToUse == null) {
            templateToUse = outputPattern;
        }
        if (templateToUse == null) {
            log.info(theLine);
        } else {
            Template template = new Template("templateName", new StringReader(templateToUse), new Configuration());
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("line", theLine);
            parameters.put("fileName", theFile.getName());
            parameters.put("lineNumber", lineNumber + "");
            StringWriter output = new StringWriter();
            template.process(parameters, output);
            log.info(output.toString());
        }
    }

    private void failIfFound(File theFile, String theLine, Grep grep, int lineNumber) throws IOException, MojoFailureException {
        if (grep.isFailIfFound()) {
            String msg = grep.getGrepPattern() + " found in  " + theFile.getCanonicalPath() + ":" + lineNumber + " (" + theLine + ")";
            log.error(msg);
            throw new MojoFailureException(msg);
        }
    }

    private void failIfNotFound(File theFile, Grep grep) throws IOException, MojoFailureException {
        if (grep.isFailIfNotFound()) {
            String msg = grep.getGrepPattern() + " not found in  " + theFile.getCanonicalPath();
            log.error(msg);
            throw new MojoFailureException(msg);
        }
    }
}
