package com.microsoft.vso.idea.test;

import com.microsoft.teamfoundation.build.webapi.BuildHttpClientBase;
import com.microsoft.teamfoundation.core.webapi.CoreHttpClientBase;
import com.microsoft.teamfoundation.distributedtask.webapi.TaskAgentHttpClientBase;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClientBase;
import com.microsoft.teamfoundation.workitemtracking.webapi.WorkItemTrackingHttpClientBase;
import com.microsoft.vss.client.core.VssHttpClientBase;

/**
 * Created by jasholl on 7/16/2015.
 */
public class DoesRestAPICompile {

    //com.microsoft.teamfoundation.client.build2
    BuildHttpClientBase buildHttpClientBase;

    //com.microsoft.teamfoundation.client.core
    CoreHttpClientBase coreHttpClientBase;

    //com.microsoft.teamfoundation.client.distributedtask
    TaskAgentHttpClientBase taskAgentHttpClientBase;

    //com.microsoft.teamfoundation.client.sourcecontrol
    GitHttpClientBase gitHttpClientBase;

    //com.microsoft.teamfoundation.client.workitemtracking
    WorkItemTrackingHttpClientBase workItemTrackingHttpClientBase;

    //com.microsoft.vss.client.core
    VssHttpClientBase vssHttpClientBase;
}
