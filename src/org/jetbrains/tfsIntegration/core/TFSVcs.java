package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.tfsIntegration.stubs.RepositorySoap12Stub;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.WorkingFolder;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Workspace;
import org.jetbrains.tfsIntegration.core.tfs.TFSServerInfo;

import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * Date: 27.01.2008
 * Time: 18:24:26
 */
public class TFSVcs extends AbstractVcs {

  @NonNls public static final String TFS_NAME = "TFS";
  private static final Logger LOG = Logger.getInstance("org.jetbrains.tfsIntegration.core.TFSVcs");
  private TFSConfiguration myConfiguration;
  private TFSEntriesFileListener myEntriesFileListener;
  private VcsShowConfirmationOption myAddConfirmation;
  private VcsShowConfirmationOption myDeleteConfirmation;
  private VcsShowSettingOption myCheckoutOptions;

  public TFSVcs(Project project, TFSConfiguration configuration) {
    super(project);
    myConfiguration = configuration;
    myEntriesFileListener = new TFSEntriesFileListener(project);

    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    myAddConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, this);
    myDeleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, this);
    myCheckoutOptions = vcsManager.getStandardOption(VcsConfiguration.StandardOption.CHECKOUT, this);
  }

  public static void main(String []args) {
    //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
    TFSServerInfo.checkServer("http://st-tfs01:8080", "tfsuser", "SWIFTTEAMS", "Parol");
    //testTFSConnect();
  }

  private static void testTFSConnect() {

    //System.setProperty("http.proxyHost", "127.0.0.1");
    //System.setProperty("http.proxyPort", "8888");

    try {
      final int attempts[] = new int[]{3};
      HttpParams httpParams = HttpClientParams.getDefaultParams();
      httpParams.setParameter(CredentialsProvider.PROVIDER, new CredentialsProvider() {

        public Credentials getCredentials(AuthScheme authScheme, String s, int i, boolean b) throws CredentialsNotAvailableException {
          if (attempts[0]-- > 0) {
            return new NTCredentials("tfsuser", "Parol", "SENIN_NB_XP", "SWIFTTEAMS");
          }
          else {
            return null;
          }
        }
      });
      String targetEndPoint = "http://st-tfs01:8080/VersionControl/v1.0/repository.asmx";
      Workspace[] workspaces = new RepositorySoap12Stub(targetEndPoint).QueryWorkspaces("tfsuser", "SENIN_NB_XP").getWorkspace();
      for (Workspace w : workspaces) {
        System.out.println(w.getComputer() + "\t" + w.getOwner());
        WorkingFolder[] workingFolders = w.getFolders().getWorkingFolder();
        for (WorkingFolder wf : workingFolders) {
          System.out.println("\t" + wf.getItem() + " --> " + wf.getLocal());
        }
      }
    }
    catch (RemoteException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public static TFSVcs getInstance(Project project) {
    return (TFSVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(TFS_NAME);
  }

  @NonNls
  public String getName() {
    LOG.debug("getName");
    return TFS_NAME;
  }

  @NonNls
  public String getDisplayName() {
    LOG.debug("getDisplayName");
    return "TFS";
  }

  public Configurable getConfigurable() {
    LOG.debug("createConfigurable");
    return new TFSConfigurable(myProject);
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  public void activate() {
    super.activate();
    TFSApplicationSettings.getInstance().tfsActivated();
    VirtualFileManager.getInstance().addVirtualFileListener(myEntriesFileListener);
  }

  @Override
  public void deactivate() {
    VirtualFileManager.getInstance().removeVirtualFileListener(myEntriesFileListener);
    TFSApplicationSettings.getInstance().tfsDeactivated();
    super.deactivate();
  }

  public VcsShowConfirmationOption getAddConfirmation() {
    return myAddConfirmation;
  }

  public VcsShowConfirmationOption getDeleteConfirmation() {
    return myDeleteConfirmation;
  }

  public VcsShowSettingOption getCheckoutOptions() {
    return myCheckoutOptions;
  }

  public TFSConfiguration getSvnConfiguration() {
    return myConfiguration;
  }

}
