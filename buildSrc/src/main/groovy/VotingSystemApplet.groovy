import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gradle.api.tasks.bundling.*
import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.UUID;

class VotingSystemApplet extends Jar {
	
	private static Logger logger = LoggerFactory.getLogger(VotingSystemApplet.class);
	
	File appletDependencies
	File outputFolder
	File buildDirectory
	File classesFolder
	/*def ignoredJars = ["groovy-all-1.8.6.jar", 
		"bcprov-jdk16-1.46.jar", "bcmail-jdk16-1.46.jar", "bctsp-jdk16-1.46.jar",
		"groovy-2.1.0.jar", "antlr-2.7.7.jar", "asm-4.0.jar", "asm-tree-4.0.jar", 
		"asm-commons-4.0.jar", "asm-util-4.0.jar", "asm-analysis-4.0.jar",
		"mail-1.4.1.jar", "activation-1.1.1.jar"]*/
	def ignoredJars = ["groovy-all-1.8.6.jar", 
		"groovy-2.1.0.jar", "antlr-2.7.7.jar", "asm-4.0.jar", "asm-tree-4.0.jar", 
		"bcprov-jdk16-1.46.jar",
		"asm-commons-4.0.jar", "asm-util-4.0.jar", "asm-analysis-4.0.jar"]

	VotingSystemApplet () {
		super()
		println "--- :${project.name}:VotingSystemApplet --- "
		buildDirectory = new File("$project.buildDir");
		if(!buildDirectory.exists()) buildDirectory.mkdir();
		outputFolder = new File("$project.buildDir/org.votingsystem.applet-dependencies");
		if(!outputFolder.exists()) outputFolder.mkdir();
		classesFolder = new File("$project.buildDir/classes");
		if(!classesFolder.exists()) classesFolder.mkdir();
		appletDependencies = new File("$project.buildDir/org.votingsystem.applet-dependencies.jar")
		appletDependencies.delete()
		from project.sourceSets.main.output
		from {
			project.configurations.compile.collect {
				if(it.isDirectory()) it
				else {
					if(!it.exists()) return;
					if(ignoredJars.contains(it.name)) {
						return
					}
					byte[] buf = new byte[2048];
					ZipInputStream zin = new ZipInputStream(new FileInputStream(it));
					ZipEntry entry = zin.getNextEntry();
					while (entry != null) {
						if(!(entry.name.toUpperCase().contains("META-INF"))
							|| entry.name.toUpperCase().contains("MAILCAP")) {
							File newEntryFile = new File(outputFolder.getPath()
								+ File.separator + entry.name);
							File destinationParent = newEntryFile.getParentFile();
							destinationParent.mkdirs();
							if (!entry.isDirectory()) {
								FileOutputStream fos = new FileOutputStream(newEntryFile);
								int len;
								while ((len = zin.read(buf)) > 0) {
									fos.write(buf, 0, len);
								}
								fos.close();
							}
						}
						entry = zin.getNextEntry();
					}
					zin.close();
				}
			}
			ant.zip(destfile: appletDependencies.path, basedir: outputFolder)
			project.zipTree(appletDependencies)
		}

	}
	
	
	@TaskAction def deleteDirs() {
		if(appletDependencies) appletDependencies.delete()
		if(outputFolder) {
			println("-------------- borrando: $outputFolder.path")
			outputFolder.deleteDir()
		}
	}
	

}
