package fr.apteryx.gradle.javacpp.jlink;

import fr.apteryx.gradle.javacpp.libextract.ExtractLibraries;
import org.beryx.jlink.JlinkPlugin;
import org.beryx.jlink.JlinkTask;
import org.beryx.jlink.data.LauncherData;
import org.beryx.jlink.data.SecondaryLauncherData;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.beryx.jlink.data.JlinkPluginExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Plugin implements org.gradle.api.Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getPlugins().apply(fr.apteryx.gradle.javacpp.libextract.Plugin.class);
        project.getPlugins().apply(JlinkPlugin.class);

        ExtractLibraries extractTask = project.getTasks().create("libExtractIntoImage",
            fr.apteryx.gradle.javacpp.libextract.ExtractLibraries.class, task -> {
                JlinkTask jlinkTask = (JlinkTask) project.getTasks().getByPath("jlink");
                File imageDir = jlinkTask.getImageDirAsFile();
                final File targetDirectory;
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win"))
                    targetDirectory = new File(imageDir, "bin");
                else if (osName.contains("linux"))
                    targetDirectory = new File(imageDir, "lib");
                else if (osName.contains("mac os x"))
                    targetDirectory = new File(imageDir, "Contents/Home/lib");
                else throw new GradleException("Unsupported OS " + osName);
                task.getTargetDirectory().set(targetDirectory);
                task.getClearTargetDirectory().set(false);
                task.getSourceSet().set("main");
            });

        project.getExtensions().configure(JlinkPluginExtension.class, ext -> {
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

        project.getTasks().getByName("jlink").finalizedBy(extractTask);

    }

    private static void addCommandLineParameters(Project project, LauncherData ld) {
        List<String> args = ld.getEffectiveJvmArgs(project);
        List<String> args2 = new ArrayList<>(args.size() + 1);
        args2.addAll(args);
        args2.add("-Dorg.bytedeco.javacpp.findlibraries=false");
        ld.setJvmArgs(args2);
    }
}

