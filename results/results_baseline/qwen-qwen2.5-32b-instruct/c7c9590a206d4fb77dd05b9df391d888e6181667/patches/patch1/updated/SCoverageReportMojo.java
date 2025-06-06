package org.scoverage.plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.render.RenderingContext;
import org.apache.maven.doxia.sink.SiteRendererSink;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.StringUtils;

import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import scoverage.domain.Constants;
import scoverage.domain.Coverage;
import scoverage.domain.Statement;
import scoverage.reporter.IOUtils;
import scoverage.reporter.Serializer;
import scoverage.reporter.CoberturaXmlWriter;
import scoverage.reporter.CoverageAggregator;
import scoverage.reporter.ScoverageHtmlWriter;
import scoverage.reporter.ScoverageXmlWriter;

/**
 * Generates SCoverage report.
 */
@Mojo( name = "report", threadSafe = false )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageReportMojo
    extends AbstractMojo
    implements MavenReport
{
    // ... (rest of the class remains unchanged)
}