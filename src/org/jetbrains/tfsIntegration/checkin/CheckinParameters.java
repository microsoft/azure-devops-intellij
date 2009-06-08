/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.checkin;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.MultiMap;
import com.intellij.vcsUtil.VcsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinNoteFieldDefinition;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.CheckinWorkItemAction;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Annotation;
import org.jdom.Element;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;

public class CheckinParameters {

  public static class CheckinNote {
    public @NotNull final String name;
    public final boolean required;
    public @Nullable String value;

    private CheckinNote(String name, boolean required) {
      this.name = name;
      this.required = required;
    }
  }

  private static class ServerData {
    public final List<CheckinNote> myCheckinNotes;
    public final WorkItemsCheckinParameters myWorkItems;
    public final List<PolicyDescriptor> myPolicies;
    public List<PolicyFailure> myPolicyFailures;
    public List<String> myEmptyNotes;
    public final Collection<FilePath> myFiles;

    private ServerData(List<CheckinNote> checkinNotes,
                       WorkItemsCheckinParameters workItems,
                       List<PolicyDescriptor> policies,
                       Collection<FilePath> files) {
      myCheckinNotes = checkinNotes;
      myWorkItems = workItems;
      myPolicies = policies;
      myFiles = files;
    }
  }

  private final CheckinProjectPanel myPanel;
  private Map<ServerInfo, ServerData> myData;
  private boolean myPoliciesEvaluated;
  private String myOverrideReason;
  private String myPoliciesLoadError;

  private CheckinParameters(final CheckinProjectPanel panel,
                            @NotNull Map<ServerInfo, ServerData> data,
                            boolean policiesEvaluated,
                            String policiesLoadError) {
    myPanel = panel;
    myData = data;
    myPoliciesEvaluated = policiesEvaluated;
    myPoliciesLoadError = policiesLoadError;
  }


  public CheckinParameters(final CheckinProjectPanel panel, final boolean evaluatePolicies) throws OperationFailedException {
    myPanel = panel;
    final Collection<FilePath> filePaths = new ArrayList<FilePath>(panel.getFiles().size());
    for (File file : panel.getFiles()) {
      filePaths.add(VcsUtil.getFilePath(file));
    }

    final TfsExecutionUtil.ResultWithError<Void> result =
      TfsExecutionUtil.executeInBackground("Validating Check In", panel.getProject(), new TfsExecutionUtil.VoidProcess() {
        public void run() throws TfsException, VcsException {
          ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
          pi.setText("Loading check in notes and policies definitons");
          final MultiMap<ServerInfo, String> serverToProjects = new MultiMap<ServerInfo, String>() {
            @Override
            protected Collection<String> createCollection() {
              return new THashSet<String>();
            }
          };
          final Map<ServerInfo, Collection<FilePath>> serverToFiles = new HashMap<ServerInfo, Collection<FilePath>>();
          WorkstationHelper.processByWorkspaces(filePaths, false, new WorkstationHelper.VoidProcessDelegate() {
            public void executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
              Collection<FilePath> files = serverToFiles.get(workspace.getServer());
              if (files == null) {
                files = new ArrayList<FilePath>();
                serverToFiles.put(workspace.getServer(), files);
              }
              for (ItemPath path : paths) {
                serverToProjects.putValue(workspace.getServer(), VersionControlPath.getPathToProject(path.getServerPath()));
                files.add(path.getLocalPath());
              }
            }
          });

          pi.checkCanceled();

          List<ServerInfo> sortedServers = new ArrayList<ServerInfo>(serverToProjects.keySet());
          Collections.sort(sortedServers, new Comparator<ServerInfo>() {
            public int compare(ServerInfo o1, ServerInfo o2) {
              return o1.getUri().compareTo(o2.getUri());
            }
          });

          Map<ServerInfo, ServerData> data = new LinkedHashMap<ServerInfo, ServerData>();
          StringBuilder policiesLoadError = new StringBuilder();
          for (ServerInfo server : sortedServers) {
            final Collection<String> teamProjects = serverToProjects.get(server);
            final List<CheckinNoteFieldDefinition> checkinNoteDefinitions = server.getVCS().queryCheckinNoteDefinition(teamProjects);
            pi.checkCanceled();
            Map<String, CheckinNoteFieldDefinition> nameToDefinition = new HashMap<String, CheckinNoteFieldDefinition>();
            // factorize different team projects definitions by name and sort them by display order field
            for (CheckinNoteFieldDefinition definition : checkinNoteDefinitions) {
              if (!nameToDefinition.containsKey(definition.getName()) || definition.getReq()) {
                nameToDefinition.put(definition.getName(), definition);
              }
            }
            List<CheckinNoteFieldDefinition> sortedDefinitions = new ArrayList<CheckinNoteFieldDefinition>(nameToDefinition.values());
            Collections.sort(sortedDefinitions, new Comparator<CheckinNoteFieldDefinition>() {
              public int compare(final CheckinNoteFieldDefinition o1, final CheckinNoteFieldDefinition o2) {
                return o1.get_do() - o2.get_do();
              }
            });

            List<CheckinNote> checkinNotes = new ArrayList<CheckinNote>(sortedDefinitions.size());
            for (CheckinNoteFieldDefinition checkinNote : sortedDefinitions) {
              checkinNotes.add(new CheckinNote(checkinNote.getName(), checkinNote.getReq()));
            }

            List<PolicyDescriptor> descriptors = new ArrayList<PolicyDescriptor>();
            try {
              if (TFSConfigurationManager.getInstance().supportTfsCheckinPolicies()) {
                Collection<Annotation> annotations =
                  server.getVCS().queryAnnotations(TFSConstants.TFS_CHECKIN_POLICIES_ANNOTATION, teamProjects);
                for (Annotation annotation : annotations) {
                  if (annotation.getValue() != null) {
                    for (PolicyDescriptor descriptor : StatelessPolicyParser.parseDescriptors(annotation.getValue())) {
                      if (descriptor.isEnabled()) {
                        descriptors.add(descriptor);
                      }
                    }
                  }
                }
              }

              if (TFSConfigurationManager.getInstance().supportStatefulCheckinPolicies()) {
                Collection<Annotation> annotations =
                  server.getVCS().queryAnnotations(TFSConstants.STATEFUL_CHECKIN_POLICIES_ANNOTATION, teamProjects);
                for (Annotation annotation : annotations) {
                  if (annotation.getValue() != null) {
                    for (PolicyDescriptor descriptor : StatefulPolicyParser.parseDescriptors(annotation.getValue())) {
                      if (descriptor.isEnabled()) {
                        descriptors.add(descriptor);
                      }
                    }
                  }
                }
              }
            }
            catch (PolicyParseException e) {
              policiesLoadError.append(e.getMessage());
            }
            pi.checkCanceled();

            data.put(server, new ServerData(checkinNotes, new WorkItemsCheckinParameters(), descriptors, serverToFiles.get(server)));
          }

          myPoliciesLoadError = policiesLoadError.length() > 0 ? policiesLoadError.toString() : null;
          myData = data;

          if (evaluatePolicies) {
            pi.setText("Evaluating check in policies");
            evaluatePolicies(pi);
            myPoliciesEvaluated = true;
          }
        }
      });

    if (myData == null) {
      throw new OperationFailedException(result.cancelled ? "Validation cancelled by user" : result.error.getMessage());
    }

    validateNotes();
  }

  public boolean policiesEvaluated() {
    return myPoliciesEvaluated;
  }

  public String getPoliciesLoadError() {
    return myPoliciesLoadError;
  }

  public void evaluatePolicies(ProgressIndicator pi) {
    //noinspection ConstantConditions
    for (final Map.Entry<ServerInfo, ServerData> entry : myData.entrySet()) {
      PolicyContext context = createPolicyContext(entry.getKey());

      List<PolicyFailure> allFailures = new ArrayList<PolicyFailure>();
      for (PolicyDescriptor descriptor : entry.getValue().myPolicies) {
        PolicyBase policy = null;
        try {
          policy = CheckinPoliciesManager.find(descriptor.getType());
        }
        catch (DuplicatePolicyIdException e) {
          final String tooltip = MessageFormat
            .format("Several check in policies with the same id found: ''{0}''.\nPlease review your extensions.", e.getDuplicateId());
          allFailures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, "Duplicate check in policy id", tooltip));
          break;
        }

        if (policy == null) {
          if (TFSConfigurationManager.getInstance().reportNotInstalledCheckinPolicies()) {
            allFailures.add(new NotInstalledPolicyFailure(descriptor.getType(), !(descriptor instanceof StatefulPolicyDescriptor)));
          }
          continue;
        }

        pi.setText(MessageFormat.format("Evaluating check in policy: {0}", policy.getPolicyType().getName()));
        pi.setText2("");
        if (descriptor instanceof StatefulPolicyDescriptor) {
          try {
            policy.loadState((Element)((StatefulPolicyDescriptor)descriptor).getConfiguration().clone());
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (RuntimeException e) {
            TFSVcs.LOG.warn(e);
            String message = MessageFormat.format("Check in policy ''{0}'' failed to load configuration", policy.getPolicyType().getName());
            String tooltip = MessageFormat.format("The following error occured while loading: {0}", e.getMessage());
            allFailures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, message, tooltip));
            continue;
          }
        }

        try {
          final PolicyFailure[] failures = policy.evaluate(context, pi);
          allFailures.addAll(Arrays.asList(failures));
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (RuntimeException e) {
          TFSVcs.LOG.warn(e);
          String message = MessageFormat.format("Check in policy ''{0}'' failed to evaluate", policy.getPolicyType().getName());
          String tooltip = MessageFormat.format("The following error occured while evaluating: {0}", e.getMessage());
          allFailures.add(new PolicyFailure(CheckinPoliciesManager.DUMMY_POLICY, message, tooltip));
        }
        pi.checkCanceled();
      }
      entry.getValue().myPolicyFailures = allFailures;
    }
    myPoliciesEvaluated = true;
  }

  public PolicyContext createPolicyContext(final ServerInfo server) {
    final ServerData serverData = myData.get(server);
    return new PolicyContext() {
      public Collection<FilePath> getFiles() {
        return Collections.unmodifiableCollection(serverData.myFiles);
      }

      public Project getProject() {
        return myPanel.getProject();
      }

      public String getCommitMessage() {
        return myPanel.getCommitMessage();
      }

      public Map<WorkItem, WorkItemAction> getWorkItems() {
        Map<WorkItem, WorkItemAction> result = new HashMap<WorkItem, WorkItemAction>(serverData.myWorkItems.getWorkItemsActions().size());
        for (Map.Entry<WorkItem, CheckinWorkItemAction> entry : serverData.myWorkItems.getWorkItemsActions().entrySet()) {
          result
            .put(entry.getKey(), entry.getValue() == CheckinWorkItemAction.Associate ? WorkItemAction.Associate : WorkItemAction.Resolve);
        }
        return result;
      }
    };
  }

  public enum Severity {
    ERROR, WARNING, BOTH
  }

  @Nullable
  public Pair<String/*message*/, Severity> getValidationMessage(final Severity severity) {
    StringBuilder result = new StringBuilder();
    Severity resultingSeverity = Severity.WARNING;

    final boolean checkError = severity == Severity.ERROR || severity == Severity.BOTH;
    boolean checkWarning = severity == Severity.WARNING || severity == Severity.BOTH;

    if (!myPoliciesEvaluated && checkWarning) {
      if (TFSConfigurationManager.getInstance().supportStatefulCheckinPolicies() ||
          TFSConfigurationManager.getInstance().supportTfsCheckinPolicies()) {
        result.append("Check in policies have not been evaluated");
      }
      checkWarning = false;
    }

    //noinspection ConstantConditions
    for (Map.Entry<ServerInfo, ServerData> entry : myData.entrySet()) {
      final ServerData data = entry.getValue();
      if ((checkError && !data.myEmptyNotes.isEmpty()) || (checkWarning && !data.myPolicyFailures.isEmpty())) {
        if (result.length() > 0) {
          result.append("\n");
        }
        //noinspection ConstantConditions
        if (myData.size() > 1) {
          result.append(entry.getKey().getUri()).append("\n");
        }
        if (checkError && !data.myEmptyNotes.isEmpty()) {
          resultingSeverity = Severity.ERROR;
          final String message;
          if (data.myEmptyNotes.size() > 1) {
            message = MessageFormat.format("Check in notes ''{0}'' are required to commit",
                                           StringUtil.join(data.myEmptyNotes.toArray(new String[data.myEmptyNotes.size()]), "', '"));

          }
          else {
            message = MessageFormat.format("Check in note ''{0}'' is required to commit", data.myEmptyNotes.iterator().next());
          }
          result.append(message);

        }
        if (checkWarning && !data.myPolicyFailures.isEmpty()) {
          if (checkError && !data.myEmptyNotes.isEmpty()) {
            result.append("\n");
          }
          result.append("Check in policy warnings found");
        }
      }
    }

    return result.length() > 0 ? Pair.create(result.toString(), resultingSeverity) : null;
  }

  public void validateNotes() {
    //noinspection ConstantConditions
    for (ServerData serverData : myData.values()) {
      List<String> emptyNotes = new ArrayList<String>();
      for (CheckinNote checkinNote : serverData.myCheckinNotes) {
        if (checkinNote.required && StringUtil.isEmptyOrSpaces(checkinNote.value)) {
          emptyNotes.add(checkinNote.name);
        }
      }
      serverData.myEmptyNotes = emptyNotes;
    }
  }

  //@Nullable
  //public CheckinValidationMessage validate(PolicyContext context,
  //                                         @Nullable CheckinValidationMessage.Severity certainSeverity,
  //                                         Condition<ServerInfo> acceptServer) {
  //  CheckinValidationMessage.Severity severity = certainSeverity;
  //  StringBuilder message = null;
  //  for (Map.Entry<ServerInfo, CheckinParameters> entry : params.entrySet()) {
  //    if (!acceptServer.value(entry.getKey())) {
  //      continue;
  //    }
  //
  //    final Collection<CheckinValidationMessage> msgs = new ArrayList<CheckinValidationMessage>(entry.getValue().validate(context));
  //    if (certainSeverity != null) {
  //      for (Iterator<CheckinValidationMessage> i = msgs.iterator(); i.hasNext();) {
  //        if (i.next().getSeverity() != certainSeverity) {
  //          i.remove();
  //        }
  //      }
  //    }
  //
  //    if (!msgs.isEmpty()) {
  //      if (message == null) {
  //        message = new StringBuilder();
  //      }
  //      else if (message.length() > 0) {
  //        message.append("\n");
  //      }
  //
  //      if (params.size() > 1) { // show server address if original file set affected several servers
  //        message.append(entry.getKey().getUri()).append(":");
  //      }
  //      for (CheckinValidationMessage msg : msgs) {
  //        if (message.length() > 0) {
  //          message.append("\n");
  //        }
  //        message.append(msg.getMessage());
  //        if (severity == null || msg.getSeverity().compareTo(severity) < 0) {
  //          severity = msg.getSeverity();
  //        }
  //      }
  //    }
  //  }
  //  //noinspection ConstantConditions
  //  return message != null ? new CheckinValidationMessage(severity, message.toString()) : null;
  //}

  public List<ServerInfo> getServers() {
    //noinspection ConstantConditions
    return new ArrayList<ServerInfo>(myData.keySet());
  }

  public List<CheckinNote> getCheckinNotes(ServerInfo server) {
    //noinspection ConstantConditions
    return Collections.unmodifiableList(myData.get(server).myCheckinNotes);
  }

  public boolean hasEmptyNotes(ServerInfo server) {
    //noinspection ConstantConditions
    return !myData.get(server).myEmptyNotes.isEmpty();
  }

  public boolean hasPolicyFailures(ServerInfo server) {
    //noinspection ConstantConditions
    return (TFSConfigurationManager.getInstance().supportStatefulCheckinPolicies() ||
            TFSConfigurationManager.getInstance().supportTfsCheckinPolicies()) &&
           (!myPoliciesEvaluated || !myData.get(server).myPolicyFailures.isEmpty());
  }

  public List<PolicyFailure> getFailures(ServerInfo server) {
    if (!myPoliciesEvaluated) {
      return Collections.emptyList();
    }
    else {
      return Collections.unmodifiableList(myData.get(server).myPolicyFailures);
    }
  }

  public List<PolicyFailure> getAllFailures() {
    if (!myPoliciesEvaluated) {
      return Collections.emptyList();
    }
    else {
      List<PolicyFailure> result = new ArrayList<PolicyFailure>();
      for (ServerData data : myData.values()) {
        result.addAll(data.myPolicyFailures);
      }
      return result;
    }
  }


  public WorkItemsCheckinParameters getWorkItems(ServerInfo server) {
    //noinspection ConstantConditions
    return myData.get(server).myWorkItems;
  }

  //public Collection<? extends CheckinValidationMessage> validateCheckinPolicies(final PolicyContext context) {
  //  final Collection<CheckinValidationMessage> result = new ArrayList<CheckinValidationMessage>();
  //
  //  Runnable runnable = new Runnable() {
  //    public void run() {
  //      final ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
  //      pi.setIndeterminate(false);
  //      int i = 1;
  //      for (PolicyBase policy : myPolicies) {
  //        pi.checkCanceled();
  //        pi.setText2(policy.getPolicyType().getName());
  //        try {
  //          final PolicyFailure[] failures = policy.evaluate(context, pi);
  //          for (PolicyFailure failure : failures) {
  //            result.add(new CheckinValidationMessage(CheckinValidationMessage.Severity.PolicyWarning, failure.getMessage()));
  //          }
  //        }
  //        catch (ProcessCanceledException e) {
  //          throw e;
  //        }
  //        catch (RuntimeException e) {
  //          TFSVcs.LOG.warn(e);
  //          String message =
  //            MessageFormat.format("Check In policy ''{0}'' failed to evaluate: {1}", policy.getPolicyType().getName(), e.getMessage());
  //          result.add(new CheckinValidationMessage(CheckinValidationMessage.Severity.PolicyWarning, message));
  //        }
  //        pi.setFraction(i / myPolicies.size());
  //      }
  //    }
  //  };
  //  final boolean completed = ProgressManager.getInstance()
  //    .runProcessWithProgressSynchronously(runnable, "Evaluating Check In Policies", true, context.getProject());
  //
  //  if (completed) {
  //    return result;
  //  }
  //  else {
  //    return Collections.singletonList(
  //      new CheckinValidationMessage(CheckinValidationMessage.Severity.Error, "Check In policies evaluation cancelled by user"));
  //  }
  //}

  public CheckinParameters createCopy() {
    @SuppressWarnings({"ConstantConditions"}) Map<ServerInfo, ServerData> result = new LinkedHashMap<ServerInfo, ServerData>(myData.size());
    //noinspection ConstantConditions
    for (Map.Entry<ServerInfo, ServerData> entry : myData.entrySet()) {
      final ServerData serverData = entry.getValue();
      List<CheckinNote> checkinNotesCopy = new ArrayList<CheckinNote>(serverData.myCheckinNotes.size());
      for (CheckinNote original : serverData.myCheckinNotes) {
        CheckinNote copy = new CheckinNote(original.name, original.required);
        copy.value = original.value;
        checkinNotesCopy.add(copy);
      }
      ServerData serverDataCopy =
        new ServerData(checkinNotesCopy, serverData.myWorkItems.createCopy(), serverData.myPolicies, serverData.myFiles);
      serverDataCopy.myEmptyNotes = new ArrayList<String>(serverData.myEmptyNotes);
      serverDataCopy.myPolicyFailures = serverData.myPolicyFailures;
      result.put(entry.getKey(), serverDataCopy);
    }
    return new CheckinParameters(myPanel, result, myPoliciesEvaluated, myPoliciesLoadError);
  }

  public void setOverrideReason(String value) {
    myOverrideReason = value;
  }

  public String getOverrideReason() {
    return myOverrideReason;
  }


  @Nullable
  public Pair<String, Map<String, String>> getPolicyOverride(ServerInfo server) {
    if (myOverrideReason == null) {
      return null;
    }

    Map<String, String> failures = new LinkedHashMap<String, String>();
    for (PolicyFailure policyFailure : myData.get(server).myPolicyFailures) {
      failures.put(policyFailure.getPolicyName(), policyFailure.getMessage());
    }

    return Pair.create(myOverrideReason, failures);
  }

}
