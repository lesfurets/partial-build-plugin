package com.vackosar.gitflowincrementalbuild.boundary;

import com.vackosar.gitflowincrementalbuild.mocks.RepoTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IT extends RepoTest {

    @Test
    public void buildWithAlsoMake() throws Exception {
        final String output = executeBuild(true);
        System.out.println(output);

        Assert.assertFalse(output.contains(" child1"));
        Assert.assertFalse(output.contains(" child2"));
        Assert.assertFalse(output.contains(" subchild1"));
        Assert.assertFalse(output.contains(" subchild42"));

        Assert.assertTrue(output.contains(" subchild2"));
        Assert.assertTrue(output.contains(" child3"));
        Assert.assertTrue(output.contains(" child4"));
        Assert.assertTrue(output.contains(" subchild41"));
        Assert.assertTrue(output.contains(" child6"));
    }

    @Test
    public void buildWithoutAlsoMake() throws Exception {
        final String output = executeBuild(false);
        System.out.println(output);

        Assert.assertFalse(output.contains(" child1"));
        Assert.assertFalse(output.contains(" child2"));
        Assert.assertFalse(output.contains(" subchild1"));
        Assert.assertFalse(output.contains(" subchild42"));
        Assert.assertFalse(output.contains(" child6"));
        Assert.assertTrue(output.contains("child6 ---\n[INFO] Tests are skipped."));

        Assert.assertTrue(output.contains(" subchild2"));
        Assert.assertTrue(output.contains(" child3"));
        Assert.assertTrue(output.contains(" child4"));
        Assert.assertTrue(output.contains(" subchild41"));
    }

    private String executeBuild(boolean alsoMake) throws IOException, InterruptedException {
        String version = Files.readAllLines(Paths.get("pom.xml")).stream().filter(s -> s.contains("<version>")).findFirst().get().replaceAll("</*version>", "").replaceAll("^[ \t]*", "");
        final Process process =
                new ProcessBuilder("cmd", "/c", "mvn", "install", alsoMake?"-am":"", "--file", "parent\\pom.xml", "-DgibVersion=" + version, "-Dgib.skipDependenciesTest=true")
                        .directory(new File("tmp/repo"))
                        .start();
        String output = convertStreamToString(process.getInputStream());
        System.out.println(convertStreamToString(process.getErrorStream()));
        process.waitFor();
        return output;
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
