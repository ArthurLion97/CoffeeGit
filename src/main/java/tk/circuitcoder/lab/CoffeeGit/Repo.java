package tk.circuitcoder.lab.CoffeeGit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.Transport;

public class Repo {
	public static enum RepoStatus {
		FINE(false,true,"Repository good to go"),
		UNINITED(false,true,"Repository not initialized"),
		EMPTY(false,true,"Empty repository"),
		
		FETCHING(false,false,"Fetching from remote..."),
		PUSHING(false,false,"Pulling from remote..."),
		PULLING(false,false,"Pushing to remote..."),
		
		FETCHED(false,true,"Fetch completed"),
		PUSHED(false,true,"Push completed"),
		MERGED(false,true,"Merge completed"),
		PULLED(false,true,"Pull completed"),
		
		CONFLICT(true,true,"An merge/pull conflict has occured. Click for more detail."),
		UNKNOW(true,true,"An unknow error has occured. Click for more detail.");
		
		private boolean e;
		private boolean i;
		private String dt;
		private RepoStatus(boolean error,boolean idle,String defaultText) {
			e=error;
			i=idle;
			dt=defaultText;
		}
		
		public boolean isError() {
			return e;
		}
		
		public boolean isIdle() {
			return i;
		}
		
		public String getDefaultString() {
			return dt;
		}
	}
	
	private Repository repo;
	private Git git;
	private RepoStatus status;
	private String statusLine;
	private String errno;
	//TODO: error message dequeue
	
	public Repo(String dir) throws IOException {
		RepositoryBuilder rb=CoffeeGit.gerRepositoryBuilder();
		File fileDir=new File(dir);
		repo=rb.setGitDir(fileDir)
				.readEnvironment()
				.findGitDir()
				.build();
		
		String[] gitDirList=fileDir.list();
		boolean initFlag=false;
		for(int i=0;(!initFlag)&&i<gitDirList.length;i++)  if(gitDirList[i].equals(".git")) initFlag=true;
		
		if(!initFlag) status=RepoStatus.UNINITED;
		if(repo.getRef("refs/HEAD")==null) status=RepoStatus.EMPTY;
		else status=RepoStatus.FINE;
	}
	
	public RepoStatus getStatus() {
		return status;
	}
	
	public String getStatusLine() {
		return statusLine;
	}
	
	public String getErrno() {
		return errno;
	}
	
	public Map<String,Ref> getRefMap() {
		return repo.getAllRefs();
	}
	
	public Map<String,Ref> getTagMap() {
		return repo.getTags();
	}
	
	public Map<String,Ref> getRemoteRefMap(String remote) throws NotSupportedException, TransportException, URISyntaxException {
		Transport t=Transport.open(repo,remote);
		FetchConnection fc=t.openFetch();
		return fc.getRefsMap();
	}
	
	public Map<String,Ref> getRemoteBranches(String remote) throws NotSupportedException, TransportException, URISyntaxException {
		Transport t=Transport.open(repo,remote);
		FetchConnection fc=t.openFetch();
		return fc.getRefsMap();
	}
	
	public DirCache doAdd(Collection<String> patterns) throws NoFilepatternException, GitAPIException {
		AddCommand ac=git.add();
		for(String p:patterns) {
			ac.addFilepattern(p);
		}
		return ac.call();
	}
	
	public RevCommit doCommit(PersonIdent author,PersonIdent committer,String message) throws NoHeadException, NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException, WrongRepositoryStateException, GitAPIException {
		return git.commit().setAuthor(author).setCommitter(committer).setMessage(message).call();
	}
	
	public MergeResult doMerge(String ref) throws GitAPIException, IOException {
		return git.merge().include(repo.getRef(ref)).call();
	}
	
	public RepoStatus merge(String ref) {
		MergeResult mr;
		try {
			mr = doMerge(ref);
		} catch (GitAPIException | IOException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		return processMergeResult(mr);
	}
	
	public FetchResult doFetch(String remote,List<RefSpec> specs,ProgressMonitor pm) throws URISyntaxException, IOException, InvalidRemoteException, GitAPIException {
		if(pm==null) return git.fetch().setRemote(remote).setRefSpecs(specs).call();
		else return git.fetch().setRemote(remote).setRefSpecs(specs).setProgressMonitor(pm).call();
	}
	
	public RepoStatus fetch(String remote,List<RefSpec> specs,ProgressMonitor pm) {
		status=RepoStatus.FETCHING;
		FetchResult fr;
		try {
			fr = doFetch(remote, specs, pm);
		} catch (URISyntaxException | IOException | GitAPIException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		status=RepoStatus.FETCHED;
		errno=fr.getMessages();
		return status;
	}
	
	public PullResult doPull(String remote,String branch,ProgressMonitor pm,boolean useRebase) throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException, org.eclipse.jgit.api.errors.TransportException, GitAPIException {
		PullCommand pc=git.pull().setRemote(remote).setRemoteBranchName(branch);
		if(pm!=null) pc.setProgressMonitor(pm);
		if(useRebase) pc.setRebase(true);
		return pc.call();
	}
	
	public RepoStatus pull(String remote,String branch,ProgressMonitor pm,boolean useRebase) {
		status=RepoStatus.PULLING;
		PullResult pr;
		try {
			pr=doPull(remote, branch, pm,useRebase);
		} catch (GitAPIException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		if(useRebase) return processRebaseResult(pr.getRebaseResult());
		else return processMergeResult(pr.getMergeResult());
	}
	
	public Iterable<PushResult> doPush(String remote,List<RefSpec> specs,ProgressMonitor pm) throws InvalidRemoteException, org.eclipse.jgit.api.errors.TransportException, GitAPIException {
		if(pm==null) return git.push().setRemote(remote).setRefSpecs(specs).call();
		else return git.push().setRemote(remote).setRefSpecs(specs).setProgressMonitor(pm).call();
	}
	
	public void push(String remote,List<RefSpec> specs,ProgressMonitor pm) {
		
	}
	
	private RepoStatus processMergeResult(MergeResult mr) {
		if(mr.getMergeStatus().isSuccessful()) {
			status=RepoStatus.MERGED;
			try {
				RevWalk rw=new RevWalk(repo);
				RevCommit rc=rw.parseCommit(mr.getNewHead());
				if(CoffeeGit.onDebug()) System.out.println("Merge Completed.\nNew Head: "+rc.getId().getName()+"    "+rc.getShortMessage());
				statusLine=rc.getId().getName()+"    "+rc.getShortMessage();
				errno="Merge Completed.\nNew Head: "+rc.getId().getName()+"    "+rc.getShortMessage();
			} catch(Exception e) {
				status=RepoStatus.UNKNOW;
			}
			return status;
		}
		else if(mr.getMergeStatus()==MergeStatus.CONFLICTING||
				mr.getMergeStatus()==MergeStatus.CHECKOUT_CONFLICT) {
			Map<String,int[][]> c=mr.getConflicts();
			//From javadoc
			StringBuilder confMsg=new StringBuilder("###Conflicts Detail###\n");
			for (String path : c.keySet()) {
				int[][] curr = c.get(path);
				System.out.println("Conflicts in file " + path);
				for (int i = 0; i < curr.length; ++i) {
					confMsg.append("  Conflict #").append(i).append('\n');
					for (int j = 0; j < (curr[i].length) - 1; ++j) {
						if (curr[i][j] >= 0)
							confMsg.append("    Chunk for ")
									.append(mr.getMergedCommits()[j])
									.append(" starts on line #")
			 						.append(curr[i][j])
			 						.append('\n');
					}
				}
			}
			if(CoffeeGit.onDebug()) System.out.print(confMsg);
			status=RepoStatus.CONFLICT;
			statusLine="Total: "+c.size()+" Files";
			errno=confMsg.toString();
			return status;
		} else {
			status=RepoStatus.UNKNOW;
			statusLine=mr.getMergeStatus().toString();
			errno="An Unexcepted Merge Error Has Occured";
			return RepoStatus.UNKNOW;
		}
	}
	
	@Deprecated
	private RepoStatus processRebaseResult(RebaseResult rr) {
		//NOT YET SUPPORTED
		return status;
	}
}
