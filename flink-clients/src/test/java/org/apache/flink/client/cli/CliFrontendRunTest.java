/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.client.cli;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.client.deployment.ClusterClientServiceLoader;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.runtime.jobgraph.RestoreMode;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import org.apache.commons.cli.CommandLine;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.apache.flink.client.cli.CliFrontendTestUtils.getTestJarPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests for the RUN command. */
public class CliFrontendRunTest extends CliFrontendTestBase {

    @BeforeClass
    public static void init() {
        CliFrontendTestUtils.pipeSystemOutToNull();
    }

    @AfterClass
    public static void shutdown() {
        CliFrontendTestUtils.restoreSystemOut();
    }

    @Test
    public void testRun() throws Exception {
        final Configuration configuration = getConfiguration();

        // test without parallelism, should use parallelism default
        {
            String[] parameters = {"-v", getTestJarPath()};
            verifyCliFrontend(configuration, getCli(), parameters, 4, false);
        }
        //  test parallelism in detached mode, should use parallelism default
        {
            String[] parameters = {"-v", "-d", getTestJarPath()};
            verifyCliFrontend(configuration, getCli(), parameters, 4, true);
        }

        // test configure parallelism
        {
            String[] parameters = {"-v", "-p", "42", getTestJarPath()};
            verifyCliFrontend(configuration, getCli(), parameters, 42, false);
        }

        // test detached mode
        {
            String[] parameters = {"-p", "2", "-d", getTestJarPath()};
            verifyCliFrontend(configuration, getCli(), parameters, 2, true);
        }

        // test configure savepoint path (no ignore flag)
        {
            String[] parameters = {"-s", "expectedSavepointPath", getTestJarPath()};

            CommandLine commandLine =
                    CliFrontendParser.parse(CliFrontendParser.RUN_OPTIONS, parameters, true);
            ProgramOptions programOptions = ProgramOptions.create(commandLine);
            ExecutionConfigAccessor executionOptions =
                    ExecutionConfigAccessor.fromProgramOptions(
                            programOptions, Collections.emptyList());

            SavepointRestoreSettings savepointSettings =
                    executionOptions.getSavepointRestoreSettings();
            assertTrue(savepointSettings.restoreSavepoint());
            assertEquals("expectedSavepointPath", savepointSettings.getRestorePath());
            assertFalse(savepointSettings.allowNonRestoredState());
        }

        // test configure savepoint path (with ignore flag)
        {
            String[] parameters = {"-s", "expectedSavepointPath", "-n", getTestJarPath()};

            CommandLine commandLine =
                    CliFrontendParser.parse(CliFrontendParser.RUN_OPTIONS, parameters, true);
            ProgramOptions programOptions = ProgramOptions.create(commandLine);
            ExecutionConfigAccessor executionOptions =
                    ExecutionConfigAccessor.fromProgramOptions(
                            programOptions, Collections.emptyList());

            SavepointRestoreSettings savepointSettings =
                    executionOptions.getSavepointRestoreSettings();
            assertTrue(savepointSettings.restoreSavepoint());
            assertEquals("expectedSavepointPath", savepointSettings.getRestorePath());
            assertTrue(savepointSettings.allowNonRestoredState());
        }

        // test jar arguments
        {
            String[] parameters = {
                getTestJarPath(), "-arg1", "value1", "justavalue", "--arg2", "value2"
            };

            CommandLine commandLine =
                    CliFrontendParser.parse(CliFrontendParser.RUN_OPTIONS, parameters, true);
            ProgramOptions programOptions = ProgramOptions.create(commandLine);

            assertEquals("-arg1", programOptions.getProgramArgs()[0]);
            assertEquals("value1", programOptions.getProgramArgs()[1]);
            assertEquals("justavalue", programOptions.getProgramArgs()[2]);
            assertEquals("--arg2", programOptions.getProgramArgs()[3]);
            assertEquals("value2", programOptions.getProgramArgs()[4]);
        }
    }

    @Test
    public void testClaimRestoreModeParsing() throws Exception {
        testRestoreMode("-rm", "claim", RestoreMode.CLAIM);
    }

    @Test
    public void testLegacyRestoreModeParsing() throws Exception {
        testRestoreMode("-rm", "legacy", RestoreMode.LEGACY);
    }

    @Test
    public void testNoClaimRestoreModeParsing() throws Exception {
        testRestoreMode("-rm", "no_claim", RestoreMode.NO_CLAIM);
    }

    @Test
    public void testClaimRestoreModeParsingLongOption() throws Exception {
        testRestoreMode("--restoreMode", "claim", RestoreMode.CLAIM);
    }

    @Test
    public void testLegacyRestoreModeParsingLongOption() throws Exception {
        testRestoreMode("--restoreMode", "legacy", RestoreMode.LEGACY);
    }

    @Test
    public void testNoClaimRestoreModeParsingLongOption() throws Exception {
        testRestoreMode("--restoreMode", "no_claim", RestoreMode.NO_CLAIM);
    }

    private void testRestoreMode(String flag, String arg, RestoreMode expectedMode)
            throws Exception {
        String[] parameters = {"-s", "expectedSavepointPath", "-n", flag, arg, getTestJarPath()};

        CommandLine commandLine =
                CliFrontendParser.parse(CliFrontendParser.RUN_OPTIONS, parameters, true);
        ProgramOptions programOptions = ProgramOptions.create(commandLine);
        ExecutionConfigAccessor executionOptions =
                ExecutionConfigAccessor.fromProgramOptions(programOptions, Collections.emptyList());

        SavepointRestoreSettings savepointSettings = executionOptions.getSavepointRestoreSettings();
        assertTrue(savepointSettings.restoreSavepoint());
        assertEquals(expectedMode, savepointSettings.getRestoreMode());
        assertEquals("expectedSavepointPath", savepointSettings.getRestorePath());
        assertTrue(savepointSettings.allowNonRestoredState());
    }

    @Test(expected = CliArgsException.class)
    public void testUnrecognizedOption() throws Exception {
        // test unrecognized option
        String[] parameters = {"-v", "-l", "-a", "some", "program", "arguments"};
        Configuration configuration = getConfiguration();
        CliFrontend testFrontend =
                new CliFrontend(configuration, Collections.singletonList(getCli()));
        testFrontend.run(parameters);
    }

    @Test(expected = CliArgsException.class)
    public void testInvalidParallelismOption() throws Exception {
        // test configure parallelism with non integer value
        String[] parameters = {"-v", "-p", "text", getTestJarPath()};
        Configuration configuration = getConfiguration();
        CliFrontend testFrontend =
                new CliFrontend(configuration, Collections.singletonList(getCli()));
        testFrontend.run(parameters);
    }

    @Test(expected = CliArgsException.class)
    public void testParallelismWithOverflow() throws Exception {
        // test configure parallelism with overflow integer value
        String[] parameters = {"-v", "-p", "475871387138", getTestJarPath()};
        Configuration configuration = new Configuration();
        CliFrontend testFrontend =
                new CliFrontend(configuration, Collections.singletonList(getCli()));
        testFrontend.run(parameters);
    }

    @Test(expected = CliArgsException.class)
    public void testInvalidNegativeParallelismOption() throws Exception {
        String[] parameters = {"-p", "-2", getTestJarPath()};
        Configuration configuration = new Configuration();
        CliFrontend testFrontend =
                new CliFrontend(configuration, Collections.singletonList(getCli()));
        testFrontend.run(parameters);
    }

    @Test
    public void testDefaultParallelismOption() throws Exception {
        final Configuration configuration = getConfiguration();
        String[] parameters = {
            "-p", String.valueOf(ExecutionConfig.PARALLELISM_DEFAULT), getTestJarPath()
        };
        verifyCliFrontend(
                configuration, getCli(), parameters, ExecutionConfig.PARALLELISM_DEFAULT, false);
    }

    @Test
    public void testDefaultParallelismOptionOverridesConfiguration() throws Exception {
        // The parallelism should be the same with Cli param -1 instead of config 1.
        final Configuration configuration = getConfiguration();
        configuration.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        String[] parameters = {
            "-p", String.valueOf(ExecutionConfig.PARALLELISM_DEFAULT), getTestJarPath()
        };
        verifyCliFrontend(
                configuration, getCli(), parameters, ExecutionConfig.PARALLELISM_DEFAULT, false);
    }

    // --------------------------------------------------------------------------------------------

    public static void verifyCliFrontend(
            Configuration configuration,
            AbstractCustomCommandLine cli,
            String[] parameters,
            int expectedParallelism,
            boolean isDetached)
            throws Exception {
        RunTestingCliFrontend testFrontend =
                new RunTestingCliFrontend(
                        configuration,
                        new DefaultClusterClientServiceLoader(),
                        cli,
                        expectedParallelism,
                        isDetached);
        testFrontend.run(parameters); // verifies the expected values (see below)
    }

    public static void verifyCliFrontend(
            Configuration configuration,
            ClusterClientServiceLoader clusterClientServiceLoader,
            AbstractCustomCommandLine cli,
            String[] parameters,
            int expectedParallelism,
            boolean isDetached)
            throws Exception {
        RunTestingCliFrontend testFrontend =
                new RunTestingCliFrontend(
                        configuration,
                        clusterClientServiceLoader,
                        cli,
                        expectedParallelism,
                        isDetached);
        testFrontend.run(parameters); // verifies the expected values (see below)
    }

    private static final class RunTestingCliFrontend extends CliFrontend {

        private final int expectedParallelism;
        private final boolean isDetached;

        private RunTestingCliFrontend(
                Configuration configuration,
                ClusterClientServiceLoader clusterClientServiceLoader,
                AbstractCustomCommandLine cli,
                int expectedParallelism,
                boolean isDetached) {
            super(configuration, clusterClientServiceLoader, Collections.singletonList(cli));
            this.expectedParallelism = expectedParallelism;
            this.isDetached = isDetached;
        }

        @Override
        protected void executeProgram(Configuration configuration, PackagedProgram program) {
            final ExecutionConfigAccessor executionConfigAccessor =
                    ExecutionConfigAccessor.fromConfiguration(configuration);
            assertEquals(isDetached, executionConfigAccessor.getDetachedMode());
            assertEquals(expectedParallelism, executionConfigAccessor.getParallelism());
        }
    }
}
