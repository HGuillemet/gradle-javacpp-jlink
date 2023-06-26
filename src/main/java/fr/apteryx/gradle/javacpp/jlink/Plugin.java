package fr.apteryx.gradle.javacpp.jlink;

import fr.apteryx.gradle.javacpp.libextract.ExtractLibraries;
import org.beryx.jlink.JPackageImageTask;
import org.beryx.jlink.JlinkPlugin;
import org.beryx.jlink.JlinkTask;
import org.beryx.jlink.data.JPackageData;
import org.beryx.jlink.data.LauncherData;
import org.beryx.jlink.data.SecondaryLauncherData;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.beryx.jlink.data.JlinkPluginExtension;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class Plugin implements org.gradle.api.Plugin<Project> {

    @Override
    public void apply(Project project) {

        // Apply both plugin. Registers jlink and jpackageImage tasks
        project.getPlugins().apply(fr.apteryx.gradle.javacpp.libextract.Plugin.class);
        project.getPlugins().apply(JlinkPlugin.class);

        // Register new task to extract libraries after the jlink task
        TaskProvider<ExtractLibraries> libExtract =
            project.getTasks().register("libExtractIntoImage",
                ExtractLibraries.class, extractLibrariesTask -> {
                    JlinkTask jlinkTask = (JlinkTask) project.getTasks().getByName("jlink");
                    extractLibrariesTask.getTargetDirectory().set(project.provider(() -> {
                        Directory imageDir = jlinkTask.getImageDir();
                        String osName = System.getProperty("os.name").toLowerCase();
                        if (osName.contains("win"))
                            return imageDir.dir("bin");
                        else if (osName.contains("linux"))
                            return imageDir.dir("lib");
                        else if (osName.contains("mac os x"))
                            return imageDir.dir("lib");
                        else throw new GradleException("Unsupported OS " + osName);
                    }));
                    extractLibrariesTask.getClearTargetDirectory().set(false);
                    extractLibrariesTask.getSourceSet().set("main");
                });
        project.getTasks().named("jlink", task -> task.finalizedBy(libExtract));

        // Add new action in jpackageImage task to restore symlinks
        project.getTasks().named("jpackageImage", JPackageImageTask.class, jPackageImageTask -> {
            jPackageImageTask.doLast(t -> {
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) return;
                Directory inputDir = jPackageImageTask.getImageDir();
                JPackageData jpd = jPackageImageTask.getJpackageData();
                File outputDir = jpd.getImageOutputDir();
                project.getLogger().info("Restoring symlinks");
                final Path libInputPath = Path.of(inputDir.toString(), "lib");
                final Path libOutputPath;
                if (osName.contains("linux")) {
                    libOutputPath = Path.of(outputDir.toString(), jpd.getImageName(),
                        "lib/runtime/lib");
                } else if (osName.contains("mac os x")) {
                    libOutputPath = Path.of(outputDir.toString(), jpd.getImageName() + ".app",
                        "Contents/runtime/Contents/Home/lib");
                } else throw new GradleException("Unsupported OS " + osName);
                try {
                    Files.walkFileTree(libInputPath, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                            if (attrs.isSymbolicLink()) {
                                try {
                                    Path l = Files.readSymbolicLink(p);
                                    if (!l.isAbsolute()) {
                                        Path relPath = libInputPath.relativize(p);
                                        Path target = libOutputPath.resolve(relPath);
                                        if (!Files.exists(target)) {
                                            project.getLogger().warn(target + " does not exist");
                                        } else if (!Files.isSymbolicLink(target)) {
                                            Files.delete(target);
                                            Files.createSymbolicLink(target, l);
                                            project.getLogger().info("Restored symlink " + relPath);
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new GradleException(e.getMessage());
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new GradleException(e.getMessage());
                }
            });
        });

        // Alter extension just before jlink task to add command line parameter
        project.getTasks().named("jlink", JlinkTask.class, jlinkTask -> {
            jlinkTask.doFirst(t -> {
                JlinkPluginExtension ext = (JlinkPluginExtension) project.getExtensions().getByName("jlink");

                if (ext.getTargetPlatforms().isPresent() && ext.getTargetPlatforms().get().size() != 0)
                    throw new GradleException("JavaCPP jlink plugin cannot target a specific platform, only the platform it runs on");

                LauncherData ld = ext.getLauncherData().getOrNull();
                if (ld == null) ld = new LauncherData(project.getName());
                addCommandLineParameters(project, ld);
                ext.getLauncherData().set(ld);

                if (ext.getSecondaryLaunchers().isPresent()) {
                    List<SecondaryLauncherData> secondaryLauncherDataList = ext.getSecondaryLaunchers().get();
                    for (SecondaryLauncherData sld : secondaryLauncherDataList) {
                        addCommandLineParameters(project, sld);
                    }
                }
            });
        });
    }

    private static void addCommandLineParameters(Project project, LauncherData ld) {
        List<String> args = ld.getEffectiveJvmArgs(project);
        List<String> args2 = new ArrayList<>(args.size() + 1);
        args2.addAll(args);
        args2.add("-Dorg.bytedeco.javacpp.findlibraries=false");
        ld.setJvmArgs(args2);
    }
}

