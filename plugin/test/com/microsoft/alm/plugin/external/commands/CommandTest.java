// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.tools.TfTool;
import com.microsoft.alm.plugin.external.utils.ProcessHelper;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProcessHelper.class, TfTool.class})
public class CommandTest extends AbstractCommandTest {

    /**
     * This test makes sure that output from a command is flushed before the completion event is fired.
     *
     * @throws Exception
     */
    @Test
    public void testRaceCondition() throws Exception {
        // Fake the tool location so this works on any machine
        PowerMockito.mockStatic(TfTool.class);
        when(TfTool.getValidLocation()).thenReturn("/path/tf_home");

        Process proc = Mockito.mock(Process.class);
        PowerMockito.mockStatic(ProcessHelper.class);
        when(ProcessHelper.startProcess(anyString(), anyList())).thenReturn(proc);
        when(proc.getErrorStream()).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        });
        when(proc.getInputStream()).thenReturn(new InputStream() {
            private String result = "12345";
            private int index = 0;

            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (index < result.length()) {
                    return result.charAt(index++);
                } else {
                    return -1;
                }
            }
        });
        when(proc.waitFor()).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return 0;
            }
        });
        when(proc.exitValue()).thenReturn(0);

        final MyCommand cmd = new MyCommand(null);
        final String output = cmd.runSynchronously();
        Assert.assertEquals("12345", StringUtils.strip(output));
    }


    private class MyCommand extends Command<String> {

        public MyCommand(ServerContext context) {
            super("mycommand", context);
        }

        @Override
        public String parseOutput(String stdout, String stderr) {
            return stdout;
        }
    }
}
